package opencvj.face;

import java.io.File;

import camus.service.geo.Rectangle;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.objdetect.CascadeClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import opencvj.OpenCvJLoader;
import opencvj.OpenCvJUtils;
import utils.Initializable;
import utils.UninitializedException;
import utils.config.ConfigNode;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class SimpleOpenCvJFaceDetector implements AutoCloseable, Initializable {
	private static final Logger s_logger = LoggerFactory.getLogger("OPENCV.FACE.SIMPLE");
	
	public static final String NAME_DETECT_SCALE_FACTOR = "detect_scale_factor";
	public static final String NAME_MIN_FACE_SIZE = "min_face_size";
	public static final String NAME_MAX_FACE_SIZE = "max_face_size";

	public static final float DEF_DETECT_SCALE_FACTOR = 1.17f;
	
	// properties (BEGIN)
	private volatile OpenCvJLoader m_loader;
	private volatile CascadeClassifier m_detector;
	private volatile float m_detectScaleFactor = DEF_DETECT_SCALE_FACTOR;
	private volatile Size m_minFaceSize = new Size();
	private volatile Size m_maxFaceSize = new Size();
	// properties (END)

	public static SimpleOpenCvJFaceDetector create(OpenCvJLoader loader, ConfigNode config)
		throws Exception {
		SimpleOpenCvJFaceDetector detector = new SimpleOpenCvJFaceDetector();
		detector.setOpenCvJLoader(loader);
		detector.setCascadeFile(config.get("cascade_filepath").asFile(null));
		detector.setDetectScaleFactor(config.get(NAME_DETECT_SCALE_FACTOR).asFloat(DEF_DETECT_SCALE_FACTOR));
		detector.setMinFaceSize(OpenCvJUtils.asSize(config.get("min_face_size"), new Size()));
		detector.setMaxFaceSize(OpenCvJUtils.asSize(config.get("max_face_size"), new Size()));
		
		return detector;
	}
	
	public SimpleOpenCvJFaceDetector() {
	}

	public final void setOpenCvJLoader(OpenCvJLoader loader) {
		m_loader = loader;
	}

	public final void setCascadeFile(File cascadeFile) {
		if ( cascadeFile != null ) {
			m_detector = new CascadeClassifier(cascadeFile.getAbsolutePath());
		}
		else {
			m_detector = null;
		}
	}
	
	public final void setDetectScaleFactor(float scale) {
		m_detectScaleFactor = scale;
	}
	
	public final void setMinFaceSize(Size size) {
		m_minFaceSize = size;
	}
	
	public final void setMaxFaceSize(Size size) {
		m_maxFaceSize = size;
	}

	@Override
	public synchronized void initialize() throws Exception {
		if ( m_loader == null ) {
			throw new UninitializedException("Property 'opencvJavaLoader' was not specified: class="
											+ getClass().getName());
		}
		if ( m_loader == null ) {
			throw new UninitializedException("Property 'cascadeFile' was not specified: class="
											+ getClass().getName());
		}
		if ( m_minFaceSize == null ) {
			throw new UninitializedException("Property 'minFaceSize' was not specified: class="
											+ getClass().getName());
		}
		if ( m_maxFaceSize == null ) {
			throw new UninitializedException("Property 'maxFaceSize' was not specified: class="
											+ getClass().getName());
		}
	}

	@Override
	public void destroy() throws Exception {
		close();
	}

	@Override
	public void close() { }
	
	public Rectangle[] detectFace(Mat image, Rect roi) {
		final Mat roiImage = roi != null ? new Mat(image, roi) : image;
		MatOfRect founds = new MatOfRect();
		try {
			m_detector.detectMultiScale(image, founds, m_detectScaleFactor, 3, 0,
										m_minFaceSize, m_maxFaceSize);
			Rect[] rects = founds.toArray();

			Rectangle[] faces = new Rectangle[rects.length];
			camus.service.geo.Point offset = new camus.service.geo.Point(0, 0);
			if ( roi != null ) {
				offset = OpenCvJUtils.toPoint(roi.tl());
			}
			for ( int i =0; i < rects.length; ++i ) {
				faces[i] = OpenCvJUtils.toRectangle(rects[i]).shift(offset);
			}
			
			return faces;
		}
		finally {
			founds.release();
			if ( roi != null ) {
				roiImage.release();
			}
		}
	}
}
