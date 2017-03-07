package opencvj.track;

import opencvj.OpenCvJException;
import utils.config.ConfigNode;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class ConfigPointTrackerFactory implements PointTrackerFactory {
	private final ConfigNode m_config;
	
	public ConfigPointTrackerFactory(ConfigNode config) {
		m_config = config;
	}

	@Override
	public PointTracker create(int id) {
		PointSmoother smoother;
		
		String type = m_config.get("type").asString();
		if ( "mvavg".equals(type) ) {
			smoother = MAPointSmoother.create(m_config);
		}
		else if ( "kalman".equals(type) ) {
			smoother = KalmanPointSmoother.create(m_config);
		}
		else {
			throw new OpenCvJException("unknown PointSmoother: type=" + type);
		}
		
		return new PointTracker(id, new PointTracker.Params(m_config), smoother);
	}

	public static PointTracker create(int id, ConfigNode config) {
		return new ConfigPointTrackerFactory(config).create(id);
	}
	
	public static PointTracker createMATracker(int id, float alpha, int detectIgnoreCount,
												int lostIgnoreCount) {
		PointTracker.Params params = new PointTracker.Params();
		params.detectIgnoreCount = detectIgnoreCount;
		params.lostIgnoreCount = lostIgnoreCount;
		params.lostIgnoreMillis = Long.MAX_VALUE;
		
		return new PointTracker(id, params, new MAPointSmoother(alpha));
	}
}
