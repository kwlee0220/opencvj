package opencvj.marker;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;

import opencvj.MatConvas;
import opencvj.Mats;
import opencvj.OpenCvJ;
import opencvj.OpenCvJException;
import opencvj.OpenCvJLoader;
import opencvj.OpenCvJUtils;
import opencvj.blob.AdaptiveImageThreshold;
import opencvj.blob.Blob;
import opencvj.blob.BlobForest;
import opencvj.blob.BlobForest.Node;
import opencvj.camera.FlipCode;
import utils.Initializable;
import utils.UninitializedException;
import utils.config.ConfigNode;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class MarkerDetector implements Initializable {
	private static final Size KERNEL_SIZE = new Size(3,3);
	
    public static class Params {
    	public FlipCode flipCode =FlipCode.NONE;
    	public boolean invert = false;
    	
    	public Params(ConfigNode config) {
    		flipCode = OpenCvJUtils.asFlipCode(config.get("flip_code"), FlipCode.NONE);
    		invert = config.get("invert").asBoolean(false);
    	}
    }
	
	public static class Info {
		/** 마커 ID */
		public int id = -1;
		/** 마커의 중심점 좌표 */
		public Point center;
		/** 마커의 네 코너 점 */
		public Point[] corners;
	}
	
	// properties (BEGIN)
	private volatile OpenCvJLoader m_loader;
	private volatile ConfigNode m_config;
	// properties (END)

	private Params m_params;
	private AdaptiveImageThreshold m_threshold;
	
	public static final MarkerDetector create(OpenCvJLoader loader, ConfigNode config) throws Exception {
		MarkerDetector marker = new MarkerDetector();
		marker.setOpenCvJLoader(loader);
		marker.setConfig(config);
		marker.initialize();
		
		return marker;
	}
	
	public MarkerDetector() { }

	public final void setOpenCvJLoader(OpenCvJLoader loader) {
		m_loader = loader;
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
		if ( m_loader == null ) {
			throw new UninitializedException("Property 'openCvJLoader' was not specified: class="
											+ getClass().getName());
		}
		if ( m_params == null ) {
			if ( m_config == null ) {
				throw new UninitializedException("Property 'config' was not specified: class="
												+ getClass().getName());
			}
			m_params = new Params(m_config);
		}
		
		m_threshold = AdaptiveImageThreshold.create(m_config.get("threshold"));
		m_threshold.setThresholdType(Imgproc.THRESH_BINARY_INV);
	}

	@Override
	public void destroy() throws Exception { }
	
	public Params getParams() {
		return m_params;
	}
	
	public List<Info> detect(Mat image) {
		if ( image.empty() ) {
			throw new OpenCvJException("gray_image is empty");
		}
		
		Mat gray = new Mat();
		Mat bwImage = new Mat();
		
		try {
			Mats.toGrayImage(image, gray);
			Imgproc.GaussianBlur(gray, gray, KERNEL_SIZE, 0);
			if ( m_params.invert ) {
				Core.absdiff(gray, new Scalar(255), gray);
			}
			
			m_threshold.detect(gray, bwImage);
//			OpenCvViewManager.show("marker", bwImage);
	//		adaptiveThreshold(blurred,  bw_image, 255, CV_ADAPTIVE_THRESH_MEAN_C, CV_THRESH_BINARY_INV, 71, 15);
			
			BlobForest forest = new BlobForest(bwImage);
			List<Info> candidates = collectMarkerCorners(gray, forest);
	
			List<Info> infos = new ArrayList<Info>();
			for ( Info candidate: candidates ) {
				OpenCvJUtils.toClockWise(candidate.corners);
				if ( (candidate.id = calcMarkerCode(gray, candidate.corners)) >= 0 ) {
					infos.add(candidate);
				}
			}
	
			return infos;
		}
		finally {
			Mats.releaseAll(bwImage, gray);
		}
	}
	
	private List<Info> collectMarkerCorners(Mat image, BlobForest forest) {
		List<Info> collecteds = new ArrayList<Info>();
		
		for ( Node node: forest.getTopLevels() ) {
			collectMarkerCorners(image, node, 0, collecteds);
		}
		
		return collecteds;
	}
	
	private void collectMarkerCorners(Mat image, BlobForest.Node node, int level, List<Info> collecteds) {
		Info info = new Info();
		
		final Blob blob = node.getBlob();
		if ( blob.area() < (MIN_SIDE_LEN*MIN_SIDE_LEN) ) {
			return;
		}
		
		if ( level % 2 == 0 ) {
			if ( checkMarkerCorners(image, blob, info)  ) {
				collecteds.add(info);
//				return;
			}
		}
		for ( Node child: node.getChildren() ) {
			collectMarkerCorners(image, child, level+1, collecteds);
		}
	}
	
	private int calcMarkerCode(Mat grayImage, Point[] corners) {
		Mat markerImage = new Mat();
		try {
			// 영상에서 찾은 마커의 영역으로부터 마커 영역만 추출한다.
			OpenCvJUtils.warpRectangleImage(grayImage, corners, markerImage, new Size(60,60));
			if ( m_params.flipCode != FlipCode.NONE ) {
				FlipCode.BOTH.flip(markerImage, markerImage);
			}
	
			// 마커 내부의 픽셀들의 합으로부터 코드 값을 추출한다.
			// 마커는 6 x 6의 코드 블록으로 구성된다.
			//
			CodeBlock code = CodeBlock.extract(markerImage);
			if ( code.checkParity() ) {
				int rotateIdx = code.getRotation();
				if ( rotateIdx >= 0 ) {
					// 마커 인식 성공!!!
	
					// 코드블록과 코너 점들에 대하여 회전된 각도를 보정해 준다.
					code = code.rotate(rotateIdx);
					rotateCorner(corners, rotateIdx);
	
					return code.calcId();
				}
			}
		}
		finally {
			markerImage.release();
		}
		
		return -1;
	}

	private static final double MIN_SIDE_LEN = (CodeBlock.ROWS * 5);
	private static final Size WIN_SIZE = new Size(2,2);
	private static final Size ZERO_ZONE = new Size(-1,-1);
	private static final TermCriteria CRIT
								= new TermCriteria(TermCriteria.EPS+TermCriteria.COUNT, 10, 0.01);
	
	private boolean checkMarkerCorners(Mat image, Blob blob, Info info) {
		// 바운딩 박스를 찾는 이유는 컨투어의 대략적인 크기를 알기 위해서다.
		// 크기에 따라 컨투어를 approximation 하는 정밀도를 조정한다.
		// 여기서는 대략 10%정도의 정밀도로 조정한다. (d*approx_param 부분)
		RotatedRect rect = blob.minAreaBox();
		Size size = rect.size;
				
		if ( size.width >= MIN_SIDE_LEN && size.height >= MIN_SIDE_LEN ) {
			double d = Math.sqrt(size.area()) * 0.1;
			Blob approx = blob.approximate(d);
			
			if ( approx.npoints() == 4 ) {
				info.corners = approx.contour();
				info.center = isMarkerRect(info.corners);
				if ( info.center != null ) {
					// 검출된 마커의 코너로부터 서브픽셀 정확도로 코너 좌표를 다시 구한다.
					//
					MatOfPoint2f mop = new MatOfPoint2f(info.corners);
					try {
						Imgproc.cornerSubPix(image, mop, WIN_SIZE, ZERO_ZONE, CRIT);
						info.corners = mop.toArray();
						
						return true;
					}
					finally {
						mop.release();
					}
				}
			}
		}
		
		return false;
	}

	private Point isMarkerRect(Point[] corners) {
		// 사각형 네 변의 길이의 비율과 사각형의 두 대각선의 교점이 사각형 중앙에
		// 오는지를 비교하여 우리가 찾는 사각형 모양인지를 판단한다.
		//
		
		// 사각형의 네 변의 길이가 서로 비슷한지 본다.
		double d1 = OpenCvJUtils.distanceL2(corners[0], corners[1]);
		double d2 = OpenCvJUtils.distanceL2(corners[1], corners[2]);
		double d3 = OpenCvJUtils.distanceL2(corners[2], corners[3]);
		double d4 = OpenCvJUtils.distanceL2(corners[3], corners[0]);
		double dm = (d1 + d2 + d3 + d4) / 4.0;	// 평균 계산
		
		// 네 변의 길이의 오차가 평균에 비해 30% 안에 들어오는지 검사
		double d_th = 0.3;	
		if (Math.abs((d1 - dm)/dm) < d_th && 
			Math.abs((d2 - dm)/dm) < d_th && 
			Math.abs((d3 - dm)/dm) < d_th && 
			Math.abs((d4 - dm)/dm) < d_th ); // 성공
		else {
			return null;
		}
		
		return OpenCvJUtils.calcCrossPoint(corners[0], corners[2], corners[1], corners[3]);
	}
	
	static void rotateCorner(Point[] corners, int angleIdx) {
		if ( angleIdx == 0 ) {
			return;
		}

		Point c[] = new Point[4];
		for ( int i=0; i<4; ++i ) {
			c[i] = corners[(i + 4 + angleIdx)%4];
		}
		for ( int i=0; i<4; ++i ) {
			corners[i] = c[i];
		}
	}
	
	public static void drawMarker(Mat convas, int code) {
		drawMarker(convas, code, OpenCvJ.BLACK, OpenCvJ.WHITE);
	}
	
	public static void drawMarker(Mat convas, int code, Scalar fgColor, Scalar bgColor) {
		int cellLen = (int)Math.round(Math.min(convas.size().width, convas.size().height)/6);

		MatConvas makerConvas = new MatConvas(convas);
		makerConvas.drawRect(new Rect(0,0,cellLen*6,cellLen*6), fgColor, Core.FILLED);
		makerConvas.drawRect(new Rect(cellLen,cellLen,cellLen*4,cellLen*4), bgColor, Core.FILLED);
		makerConvas.drawRect(new Rect(cellLen,cellLen,cellLen,cellLen), fgColor, Core.FILLED);
		
		for ( int v = code; v > 0; v = drawMarkerCell(makerConvas, v, cellLen, fgColor));
	}
	
	private static int drawMarkerCell(MatConvas convas, int value, int cellLen, Scalar color) {
		if ( value >= 1024 ) {
			drawMarkerCell(convas, 3, 4, cellLen, color);
			return value - 1024;
		}
		else if ( value >= 512 ) {
			drawMarkerCell(convas, 2, 4, cellLen, color);
			return value - 512;
		}
		else if ( value >= 256 ) {
			drawMarkerCell(convas, 3, 3, cellLen, color);
			return value - 256;
		}
		else if ( value >= 128 ) {
			drawMarkerCell(convas, 2, 3, cellLen, color);
			return value - 128;
		}
		else if ( value >= 64 ) {
			drawMarkerCell(convas, 1, 3, cellLen, color);
			return value - 64;
		}
		else if ( value >= 32 ) {
			drawMarkerCell(convas, 4, 2, cellLen, color);
			return value - 32;
		}
		else if ( value >= 16 ) {
			drawMarkerCell(convas, 3, 2, cellLen, color);
			return value - 16;
		}
		else if ( value >= 8 ) {
			drawMarkerCell(convas, 2, 2, cellLen, color);
			return value - 8;
		}
		else if ( value >= 4 ) {
			drawMarkerCell(convas, 1, 2, cellLen, color);
			return value - 4;
		}
		else if ( value >= 2 ) {
			drawMarkerCell(convas, 3, 1, cellLen, color);
			return value - 2;
		}
		else if ( value >= 1 ) {
			drawMarkerCell(convas, 2, 1, cellLen, color);
			return value - 1;
		}
		
		return 0;
	}
	
	private static void drawMarkerCell(MatConvas convas, int x, int y, int cellLen, Scalar color) {
		Point offset = new Point(x*cellLen, y*cellLen);
		convas.drawRect(new Rect(offset, new Size(cellLen, cellLen)), color, Core.FILLED);
	}
}
