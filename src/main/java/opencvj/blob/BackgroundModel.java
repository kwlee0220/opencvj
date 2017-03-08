package opencvj.blob;


import org.opencv.core.Mat;

import opencvj.OpenCvJException;
import utils.config.ConfigNode;



/**
 * 
 * @author Kang-Woo Lee
 */
public interface BackgroundModel extends BackgroundLearnable, AutoCloseable {
	public Mat getBackgroundModel();

	public static BackgroundModel create(ConfigNode config) {
		BackgroundModel bgModel;
		
		ConfigNode typeNode = config.get("type");
		if ( typeNode.isMissing() ) {
			throw new OpenCvJException("unknown BackgroundModel: config=" + config);
		}
		
		String type = typeNode.asString();
		if ( type.equals("mvavg_depth") ) {
			bgModel = MADepthBackgroundModel.create(config);
		}
		else if ( type.equals("mvavg_color") ) {
			bgModel = MAColorBackgroundModel.create(config);
		}
		else {
			throw new OpenCvJException("unknown BackgroundModel: type=" + type);
		}
		
		return bgModel;
	}
}