package opencvj.camera;

import utils.config.ConfigNode;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface OpenCvJDepthCamera extends OpenCvJCamera {
	
	public static OpenCvJDepthCamera create(ConfigNode config) throws Exception {
		OpenCvJCamera camera = OpenCvJCamera.create(config);
		if ( camera instanceof OpenCvJDepthCamera ) {
			return (OpenCvJDepthCamera)camera;
		}
		else {
			throw new IllegalArgumentException("not OpenCvJDepthCamera, config=" + config);
		}
	}

}
