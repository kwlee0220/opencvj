package opencvj.camera;

import utils.config.ConfigNode;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface ColorDepthCompositeLoader {
	public ColorDepthComposite load(ConfigNode config) throws Exception;
}
