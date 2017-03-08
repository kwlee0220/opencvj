package opencvj.camera;

import org.opencv.core.Size;

import utils.config.ConfigNode;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface OpenCvJCameraFactory {
	public OpenCvJCamera createCamera();
	public Size getSize();
	
	public static OpenCvJCameraFactory create(ConfigNode config) throws Exception {
		OpenCvJCameraFactoryImpl cameraFact = new OpenCvJCameraFactoryImpl();
		cameraFact.setSourceCamera(OpenCvJCamera.create(config));
		cameraFact.setConfig(config);
		cameraFact.initialize();
		
		return cameraFact;
	}
}
