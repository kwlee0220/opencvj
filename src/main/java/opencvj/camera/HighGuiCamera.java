package opencvj.camera;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import opencvj.OpenCvJException;
import opencvj.OpenCvJLoader;
import opencvj.OpenCvJUtils;
import utils.Initializable;
import utils.UninitializedException;
import utils.config.ConfigNode;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class HighGuiCamera implements OpenCvJCamera, Initializable {
    private static final Logger s_logger = LoggerFactory.getLogger("OPENCV.HIGHGUI");

    private static final int DEF_DEVICE_INDEX = 0;
    public static class Params {
    	public int deviceIndex =DEF_DEVICE_INDEX;
    	public Size imageSize;
    	
    	public Params(ConfigNode config) {
    		deviceIndex = config.get("device_index").asInt(DEF_DEVICE_INDEX);
    		imageSize = OpenCvJUtils.asSize(config.get("image_size"));
    	}
    }
    
	// properties (BEGIN)
	private volatile OpenCvJLoader m_loader;
	private volatile ConfigNode m_config;
	// properties (END)
	
	private Params m_params;
	private VideoCapture m_capture;
	
	public static HighGuiCamera create(OpenCvJLoader loader, ConfigNode config) throws Exception {
		HighGuiCamera camera = new HighGuiCamera();
		camera.setOpenCvJLoader(loader);
		camera.setConfig(config);
		camera.initialize();
		
		return camera;
	}
	
	public HighGuiCamera() { }

	public final void setOpenCvJLoader(OpenCvJLoader loader) {
		m_loader = loader;
	}

	public final void setConfig(ConfigNode config) {
		m_config = config;
	}
	
//	public final void setConfig(String configStr) {
//		m_config = new OpenCvJConfig(configStr);
//	}
	
	public final void setParams(Params params) {
		m_params = params;
	}

	@Override
	public synchronized void initialize() throws Exception {
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
		m_capture = new VideoCapture();
		
		s_logger.info("initialized: {}", toString());
	}

	@Override
	public void destroy() throws Exception {
		close();
	}

	public OpenCvJLoader getOpenCvJLoader() {
		return m_loader;
	}

	@Override
	public void open() {
		if ( m_capture.isOpened() ) {
			throw new OpenCvJException(getClass().getSimpleName() + " already opened");
		}
		
		if ( !m_capture.open(m_params.deviceIndex) ) {
			throw new OpenCvJException("fails to open " + getClass().getSimpleName() + "[index=" + m_params.deviceIndex + "]");
		}

		try {
			if ( !m_capture.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, m_params.imageSize.width) ) {
				throw new OpenCvJException(String.format("fails to set resolution: width=%.0f",
														m_params.imageSize.width));
			}
			if ( !m_capture.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, m_params.imageSize.height) ) {
				throw new OpenCvJException(String.format("fails to set resolution: height=%.0f",
														m_params.imageSize.height));
			}
		}
		catch ( Throwable e ) {
			s_logger.error("fails to open a camera: " + this, e);
		}
	}

	@Override
	public void close() {
		if ( m_capture != null ) {
			if ( m_capture.isOpened() ) {
				m_capture.release();
			}
			m_capture = null;
		}
	}
	
	public Size getSize() {
		return m_params.imageSize;
	}

	@Override
	public void capture(Mat image) {
		if ( !m_capture.isOpened() ) {
			throw new OpenCvJException(getClass().getSimpleName() + " is not open");
		}
		if ( !m_capture.read(image) ) {
			throw new OpenCvJException("fails to capture image");
		}
	}
	
	@Override
	public String toString() {
		return String.format("%s[device_index=%d, size=%s]", getClass().getSimpleName(),
							m_params.deviceIndex, OpenCvJUtils.toString(m_params.imageSize));
	}
}
