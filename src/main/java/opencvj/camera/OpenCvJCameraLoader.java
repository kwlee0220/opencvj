package opencvj.camera;

import opencvj.Config;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface OpenCvJCameraLoader {
	public OpenCvJCamera load(Config config) throws Exception;
}
