package opencvj.projector;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import opencvj.MatConvas;
import opencvj.Mats;
import opencvj.OpenCvJ;
import opencvj.OpenCvJException;
import opencvj.OpenCvJUtils;
import opencvj.OpenCvViewManager;
import opencvj.calib.CheckerBoard;
import opencvj.camera.FlipCode;
import opencvj.camera.OpenCvJCamera;
import opencvj.camera.OpenCvJCameraFactory;
import opencvj.misc.PerspectiveTransform;
import utils.Initializable;
import utils.UninitializedException;
import utils.UnitUtils;
import utils.config.ConfigNode;
import utils.config.json.JsonConfiguration;
import utils.io.IOUtils;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class CameraProjectorComposite implements Initializable {
    private static final Logger s_logger = Logger.getLogger("OPENCV.CPC");
    
	private static final int DEF_SAMPLE_COUNT = 10;
	private static final String DEF_TIMEOUT = "1s";
	private static final FlipCode DEF_FLIP_CODE = FlipCode.NONE; 
	private static final File DEF_PARAMETER_FILE = null;
	private static final String DEF_DEBUG_WINDOW = null;
	
	public static class Params {
		/** 캘리브레이션을 위한 카메라 이미지 샘플 갯수. */
		public int sampleCount;
		/** 캘리브레이션 시간제한 (milliseconds). */
		public long calibrationTimeout;
		public FlipCode flipCode;
		/** 캘리브레이션에 사용할 체커보드 정보. */
		public CheckerBoard checker;
		/** 캘리브레이션 결과를 저장할 파일. */
		public File parameterFile;
		public boolean initCalibration;
		/** 캘리브세이션 과정을 보여주기 위한 창 이름. */
		public String debugWindowName;
		
		public Params() {
			sampleCount = DEF_SAMPLE_COUNT;
			calibrationTimeout = UnitUtils.parseDuration(DEF_TIMEOUT);
			flipCode = DEF_FLIP_CODE;
			parameterFile = DEF_PARAMETER_FILE;
			initCalibration = true;
			debugWindowName = DEF_DEBUG_WINDOW;
		}
		
		public Params(ConfigNode config) {
			sampleCount = config.get("sample_count").asInt(DEF_SAMPLE_COUNT);
			calibrationTimeout = (int)config.get("calibration_timeout").asDuration(DEF_TIMEOUT);
			flipCode = OpenCvJUtils.asFlipCode(config.get("flip_code"), DEF_FLIP_CODE); 
			checker = CheckerBoard.create(config.get("checker_board"));
			parameterFile = config.get("parameter_filepath").asFile(DEF_PARAMETER_FILE);
			initCalibration = config.get("init_calibration").asBoolean(true);
			debugWindowName = config.get("debug_window_name").asString(DEF_DEBUG_WINDOW);
		}
	};
	
	// properties (BEGIN)
	private volatile OpenCvJCameraFactory m_cameraFact;
	private volatile OpenCvBeamProjector m_projector;
	private volatile ConfigNode m_config;
	// properties (END)
	
	private Params m_params;
	private CameraProjectorBinding m_cpParam;
	
	public static CameraProjectorComposite create(OpenCvJCameraFactory cameraFact, OpenCvBeamProjector projector,
													ConfigNode config) throws Exception {
		CameraProjectorComposite cpc = new CameraProjectorComposite();
		cpc.setCameraFactory(cameraFact);
		cpc.setProjector(projector);
		cpc.setConfig(config);
		cpc.initialize();
		
		return cpc;
	}
	
	public CameraProjectorComposite() { }
	
	public final void setCameraFactory(OpenCvJCameraFactory cameraFact) {
		m_cameraFact = cameraFact;
	}
	
	public final void setProjector(OpenCvBeamProjector projector) {
		m_projector = projector;
	}
	
	public final void setConfig(ConfigNode config) {
		m_config = config;
	}
	
//	public final void setConfig(String configStr) {
//		m_config = new Config(configStr);
//	}
	
	public final void setParams(Params params) {
		m_params = params;
	}

	@Override
	public void initialize() throws Exception {
		if ( m_cameraFact == null ) {
			throw new UninitializedException("Property 'camera' was not specified: class="
											+ getClass().getName());
		}
		if ( m_projector == null ) {
			throw new UninitializedException("Property 'projector' was not specified: class="
											+ getClass().getName());
		}
		if ( m_params == null ) {
			if ( m_config == null ) {
				throw new UninitializedException("Property 'config' was not specified: class="
												+ getClass().getName());
			}
			m_params = new Params(m_config);
		}

		boolean done = false;
		if ( m_params.initCalibration ) {
			try {
				calibrate();
				done = true;
			}
			catch ( Throwable e ) {
				s_logger.warn("fails to perform initial calibration: cause=" + e);
			}
		}
		
		if ( !done ) {
			// 초기 캘리브레이션을 수행하지 않거나, 캘리브레이션에 실패한 경우는 가장 마지막으로
			// 성공한 캘리브레이션 매핑 값을 사용한다.
			if ( m_params.parameterFile != null && m_params.parameterFile.canRead() ) {
				if ( s_logger.isInfoEnabled() ) {
					s_logger.info("loading former calibration parameter: file="
									+ m_params.parameterFile.getAbsolutePath());
				}
				
				ConfigNode parameterConfig = JsonConfiguration.load(m_params.parameterFile)
																.getRoot();
				m_cpParam = CameraProjectorBinding.create(parameterConfig);
			}
			else {
				throw new UninitializedException("camera-projector has not calibrated: "
											+ "cause=fails to access parameter file: path="
											+ m_params.parameterFile.getAbsolutePath());
			}
		}
	}

	@Override
	public void destroy() throws Exception { }
	
	public Params getParams() {
		return m_params;
	}
	
	public OpenCvJCameraFactory getCameraFactory() {
		return m_cameraFact;
	}
	
	public OpenCvBeamProjector getProjector() {
		return m_projector;
	}
	
	public ProjectionCamera getProjectionCamera() {
		return new ProjectionCamera(this);
	}
	
	public CameraProjectorBinding getCameraProjectorBinding() {
		return m_cpParam;
	}
	
	public Point[] getProjectionCorners() {
		assertValidMapping();
		
		return m_cpParam.m_projectionCorners.clone();
	}
	
	public void extractProjectionImage(Mat cameraImage, Mat extracted) {
		assertValidMapping();
		
		Mats.createIfNotValid(extracted, m_cpParam.m_projectorSize, cameraImage.type());
		m_cpParam.m_transCameraToScreen.perform(cameraImage, extracted, m_cpParam.m_projectorSize);
	}
	
	public PerspectiveTransform getProjectorToCameraTransform() {
		return m_cpParam.m_transScreenToCamera;
	}
	
	public PerspectiveTransform getCameraToProjectorTransform() {
		return m_cpParam.m_transCameraToScreen;
	}

	public Point[] toProjectorCoordinates(Point[] cameraCoords) {
		return m_cpParam.toProjectorCoordinates(cameraCoords);
	}

	public Point[] toCameraCoordinates(Point[] projectorCoords) {
		return m_cpParam.toCameraCoordinates(projectorCoords);
	}
	
	public void calibrate() {
		if ( s_logger.isDebugEnabled() ) {
			s_logger.debug("calibrating CameraProjectorComposite...");
		}

		Size screenSize = m_projector.getSize();

		// 인자로 주어진 체커보드 이미지를 이용하여 마커 코너점 좌표를 구한다.
		//
		Mat checkerImage = m_params.checker.readCheckerImage();
		try {
			if ( checkerImage.size() != screenSize ) {
				Imgproc.resize(checkerImage, checkerImage, screenSize);
			}
			
			Point[] checkerCorners = m_params.checker.findCorners(checkerImage);
			if ( checkerCorners == null ) {
				throw new OpenCvJException("fails to detect corners from the checker board image");
			}
			
			// 테이블에 체커보드를 투사하여 투사된 체커보드에서의 코너점을 인식한다.
			// 이때 오차 제거를 위해 미리 지정된 횟수만큼 코너점을 인식하여 이들의 평균 값을 구한다
			//
			if ( s_logger.isDebugEnabled() ) {
				s_logger.debug("collecting sample corner points from the projected checkerboard image...");
			}
			
			List<Point[]> samples = collectSamples(checkerImage);
			if ( samples == null ) {
				throw new OpenCvJException(String.format("fails to collect enough sample images, "
													+ "required=%d timeout=%dms",
													m_params.sampleCount, m_params.calibrationTimeout));
			}
			
			//
			// 수집된 n개의 이미지에서 검출된 checker의 교점 위치들의 평균 값을 구한다.
			//
			int nblocks = (int)m_params.checker.getPatternSize().area();
			Point[] projCorners = new Point[nblocks];
			for ( int i =0; i < nblocks; ++i ) {
				projCorners[i] = new Point(0,0);
			}
			for ( int i =0; i < m_params.sampleCount; ++i ) {
				for ( int j =0; j < nblocks; ++j ) {
					projCorners[j].x += samples.get(i)[j].x / m_params.sampleCount;
					projCorners[j].y += samples.get(i)[j].y / m_params.sampleCount;
				}
			}

			// 체커보드 이미지에서 검출한 코너점들과 체커보드 이미지를 화면에 투사하여 검출된 코너점들
			// 사이의 관계를 이용해서 perspective transform 객체를 생성함.
			switch ( m_params.flipCode ) {
				case BOTH:
					Point[] reordered = new Point[projCorners.length];
					for ( int i =0; i < projCorners.length; ++i ) {
						reordered[i] = projCorners[projCorners.length-i-1];
					}
					projCorners = reordered;
					break;
				default:
					throw new OpenCvJException("bad flipCode: " + m_params.flipCode);
			}

			m_cpParam = new CameraProjectorBinding();
			m_cpParam.m_cameraSize = m_cameraFact.getSize();
			m_cpParam.m_projectorSize = m_projector.getSize();
			m_cpParam.m_transScreenToCamera.setRansacHomography(checkerCorners, projCorners);
			m_cpParam.m_transCameraToScreen = m_cpParam.m_transScreenToCamera.inv();
			
			Point[] screenCorners = OpenCvJUtils.getCorners(m_cpParam.m_projectorSize);
			m_cpParam.m_projectionCorners = m_cpParam.m_transScreenToCamera.perform(screenCorners);
		}
		finally {
			Mats.releaseAll(checkerImage);
		}
		
		if ( s_logger.isInfoEnabled() ) {
			s_logger.info("calibration done: CameraProjectorComposite");
		}

		// calibration 성공 후 parameter를 지정된 파일에 저장한다.
		if ( m_params.parameterFile != null ) {
			try {
				Files.createDirectories(m_params.parameterFile.getParentFile().toPath());
				m_cpParam.write(m_params.parameterFile);
				if ( s_logger.isDebugEnabled() ) {
					s_logger.debug("Camera-Projector binding has written to the file '"
									+ m_params.parameterFile.getAbsolutePath() + "'");
				}
			}
			catch ( IOException e ) {
				s_logger.warn("fails to write Camera-Projector binding: cause=" + e);
			}
		}
	}
	
	private List<Point[]> collectSamples(Mat checkerImage) {
		// 프로젝터에 체커 이미지를 투사하고, 코너점 인식을 시작한다.
		m_projector.setVisible(true);
		m_projector.setPower(true);
		m_projector.show(checkerImage);

		OpenCvJCamera camera = m_cameraFact.createCamera();
		camera.open();
		
		try {
			// 프로젝터에 투사 직후 이미지를 캡쳐하면 투사 이전의 이미지가 캡쳐되는 경우가 있기 때문에
			// 일정기간 동안의 이미지를 버린다.
			OpenCvJUtils.eatFramesMillis(camera, 500);	// 500ms
	
			boolean done = false;
			long due = System.currentTimeMillis() + m_params.calibrationTimeout;
	
			Mat image = new Mat();
			List<Point[]> samples = new ArrayList<Point[]>();
			while ( !done && System.currentTimeMillis() <= due ) {
				camera.capture(image);
				
				Point[] corners = m_params.checker.findCorners(image);
				if ( corners != null ) {
					if ( m_params.debugWindowName != null ) {
						drawCorners(new MatConvas(image), corners); 
						OpenCvViewManager.show(m_params.debugWindowName, image);
					}
					
					samples.add(corners);
					if ( samples.size() >= m_params.sampleCount ) {
						done = true;
					}
				}
			}
			if ( m_params.debugWindowName != null ) {
				OpenCvViewManager.destroyView(m_params.debugWindowName);
			}
			
			return done ? samples : null;
		}
		finally {
			m_projector.setPower(false);
			m_projector.setVisible(false);
			IOUtils.closeQuietly(camera);
		}
	}
	
	private static void drawCorners(MatConvas convas, Point[] corners) {
		for ( int i =0; i < corners.length; ++i ) {
			convas.drawCircle(corners[i], 3, OpenCvJ.RED, Core.FILLED);
			if ( i < corners.length - 1 ) {
				convas.drawLine(corners[i], corners[i+1], OpenCvJ.GREEN, 1);
			}
		}
	}
	
//	private void notifyCpParamUpdated() {
//		if ( m_transToScreen != null ) {
//			m_transToScreen.release();
//		}
//		m_transToScreen = PerspectiveTransform.createPerspectiveTransform(
//													m_cpParam.projectionCorners,
//													OpenCvJUtils.getCorners(m_cpParam.screenSize));
//	}
	
	private void assertValidMapping() {
		if ( m_cpParam == null ) {
			throw new OpenCvJException("CameraProjectorComposite does have a valid mapping");
		}
	}
}
