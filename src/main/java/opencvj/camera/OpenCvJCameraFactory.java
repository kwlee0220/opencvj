package opencvj.camera;

import org.opencv.core.Size;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface OpenCvJCameraFactory {
	public OpenCvJCamera createCamera();
	public Size getSize();
}
