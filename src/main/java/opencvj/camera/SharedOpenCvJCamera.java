package opencvj.camera;

import org.opencv.core.Mat;
import org.opencv.core.Size;

import opencvj.OpenCvJException;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class SharedOpenCvJCamera implements OpenCvJCamera {
	private final OpenCvJCameraFactoryImpl m_cameraFact;
	
	SharedOpenCvJCamera(OpenCvJCameraFactoryImpl cameraFact) {
		m_cameraFact = cameraFact;
	}

	@Override
	public void open() {
		m_cameraFact.onSharedCameraOpened(this);
	}

	@Override
	public void close() {
		m_cameraFact.onSharedCameraClosed(this);
	}

	@Override
	public Size getSize() {
		return m_cameraFact.getSize();
	}

	@Override
	public void capture(Mat image) {
		try {
			m_cameraFact.capture(image);
		}
		catch ( InterruptedException e ) {
			throw new OpenCvJException("image capturing interrupted");
		}
	}

}
