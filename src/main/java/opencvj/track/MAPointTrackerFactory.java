package opencvj.track;

import utils.config.ConfigNode;



/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class MAPointTrackerFactory implements PointTrackerFactory {
	private final float m_alpha;
	private final PointTracker.Params m_params;
	
	public MAPointTrackerFactory(float alpha, int detectIgnoreCount, int lostIgnoreCount,
									long lostIgnoreMillis) {
		m_alpha = alpha;
		m_params = new PointTracker.Params();
		m_params.detectIgnoreCount = detectIgnoreCount;
		m_params.lostIgnoreCount = lostIgnoreCount;
		m_params.lostIgnoreMillis = lostIgnoreMillis;
	}
	
	public MAPointTrackerFactory(float alpha, PointTracker.Params params) {
		m_alpha = alpha;
		m_params = params;
	}

	@Override
	public PointTracker create(int id) {
		return new PointTracker(id, m_params, new MAPointSmoother(m_alpha));
	}
	
	public static PointTracker create(int id, float alpha, int detectIgnoreCount,
												int lostIgnoreCount, long lostIgnoreMillis) {
		return new MAPointTrackerFactory(alpha, detectIgnoreCount, lostIgnoreCount,
										lostIgnoreMillis).create(id);
	}
	
	public static PointTracker create(int id, ConfigNode config) {
		return new PointTracker(id, new PointTracker.Params(config),
								MAPointSmoother.create(config));
	}
}
