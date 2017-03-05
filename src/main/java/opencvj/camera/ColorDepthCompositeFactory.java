package opencvj.camera;

import org.opencv.core.Size;



/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface ColorDepthCompositeFactory {
	public ColorDepthComposite createColorDepthComposite();
	
	public OpenCvJCameraFactory getColorCameraFactory();
	public OpenCvJCameraFactory getDepthCameraFactory();
	
	public Size getColorImageSize();
	public Size getDepthImageSize();
	
	public DepthToColorMapper getDepthToColorMapper();
	public ColorToDepthMapper getColorToDepthMapper();
}
