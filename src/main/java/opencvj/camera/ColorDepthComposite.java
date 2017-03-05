package opencvj.camera;

import org.opencv.core.Mat;
import org.opencv.core.Size;



/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface ColorDepthComposite extends AutoCloseable {
	public void open();
	
	public Size getColorImageSize();
	public Size getDepthImageSize();
	
	public OpenCvJCamera getColorCamera();
	public OpenCvJCamera getDepthCamera();
	
	public void captureSynched(Mat colorImage, Mat depthImage);
	
	public DepthToColorMapper getDepthToColorMapper();
	public ColorToDepthMapper getColorToDepthMapper();
}
