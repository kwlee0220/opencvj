package opencvj.projector;

import org.opencv.core.Mat;
import org.opencv.core.Size;

import opencvj.camera.OpenCvJCamera;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class ProjectionCamera implements OpenCvJCamera {
	// properties (BEGIN)
	private final Size m_imageSize;
	private final CameraProjectorBinding m_binding;
	private final OpenCvJCamera m_srcCamera;
	
	public ProjectionCamera(CameraProjectorComposite cpc) {
		m_imageSize = cpc.getCameraFactory().getSize();
		
		m_binding = cpc.getCameraProjectorBinding();
		m_srcCamera = cpc.getCameraFactory().createCamera();
	}

	@Override
	public void open() {
		m_srcCamera.open();
	}

	@Override
	public void close() throws Exception {
		m_srcCamera.close();
	}

	@Override
	public Size getSize() {
		return m_imageSize;
	}

	@Override
	public void capture(Mat image) {
		Mat srcImage = new Mat();
		try {
			m_srcCamera.capture(srcImage);
			m_binding.extractProjectionImage(srcImage, image);
		}
		finally {
			srcImage.release();
		}
	}
}
