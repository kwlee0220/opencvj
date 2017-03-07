package opencvj.track;

import org.opencv.core.Point;

import utils.config.ConfigNode;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class KalmanPointSmoother implements PointSmoother, AutoCloseable {
	public static class Params {
		public float processNoiseCov = KalmanFilterXY.DEFAULT_PROCESS_NOISE_COV;
		public float measurementNoiseCov = KalmanFilterXY.DEFAULT_MEASUREMENT_NOISE_COV;

		Params() { }

		Params(float processNoiseCov, float measurementNoiseCov) {
			this.processNoiseCov = processNoiseCov;
			this.measurementNoiseCov = measurementNoiseCov;
		}

		Params(ConfigNode config) {
			processNoiseCov = config.get("process_noise_cov")
											.asFloat(KalmanFilterXY.DEFAULT_PROCESS_NOISE_COV);
			measurementNoiseCov = config.get("measurement_noise_cov")
											.asFloat(KalmanFilterXY.DEFAULT_MEASUREMENT_NOISE_COV);
		}
	}

	private Params m_params;
	private final KalmanFilterXY m_filter = new KalmanFilterXY();
	private Point m_estimated;
	
	public static KalmanPointSmoother create(ConfigNode config) {
		return new KalmanPointSmoother(new Params(config));
	}
	
	public KalmanPointSmoother() {
		this(new Params());
	}
	
	public KalmanPointSmoother(float processNoiseCov, float measurementNoiseCov) {
		this(new Params(processNoiseCov, measurementNoiseCov));
	}
	
	public KalmanPointSmoother(Params params) {
		m_params = params;
	}

	@Override
	public void close() {
		m_filter.close();
	}
	
	public Params getParams() {
		return m_params;
	}

	@Override
	public Point getLocation() {
		return m_estimated;
	}

	@Override
	public void setLocation(Point pt) {
		for ( int i =0; i < 4; ++i ) {
			smooth(pt);
		}
	}

	@Override
	public Point smooth(Point pos) {
		m_filter.predict();
		return m_estimated = m_filter.correct(pos);
	}
}
