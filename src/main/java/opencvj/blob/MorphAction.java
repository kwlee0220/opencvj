package opencvj.blob;

import config.Config;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public enum MorphAction {
	MORPH_ACT_CLOSE,
	MORPH_ACT_OPEN,
	MORPH_ACT_NONE;
	
	public static MorphAction from(Config config, MorphAction defValue) {
		if ( config.isMissing() ) {
			return defValue;
		}
		else {
			String act = config.asString();
			
			if ( act.equalsIgnoreCase("none") ) {
				return MORPH_ACT_NONE;
			}
			else if ( act.equalsIgnoreCase("open") ) {
				return MORPH_ACT_OPEN;
			}
			else if ( act.equalsIgnoreCase("close") ) {
				return MORPH_ACT_CLOSE;
			}
			
			throw new RuntimeException("unknown action=" + act);
		}
	}
}
