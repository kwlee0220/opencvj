package opencvj.camera;

import utils.config.ConfigNode;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface OpenCvJCameraLoader {
	public OpenCvJCamera load(ConfigNode config) throws Exception;
}
