package opencvj.track;

import org.apache.log4j.Logger;
import org.opencv.core.Point;

import opencvj.OpenCvJUtils;
import utils.config.ConfigNode;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class MAPointSmoother implements PointSmoother {
	private static final Logger s_logger = Logger.getLogger("OPENCV.SMOOTH.MVAVG");

	private static final float DEF_SPIKE_DISTANCE = -1;
	
	public static class Params {
		/** 검출된 커서 위치가 반영될 비율. */
		public float alpha;
		/** 검출된 커서 위치가 기존 위치에서 크게 다른 여부를 판단할 거리차. */
		public float spikeDistance;
		/** 검출된 커서 위치가 기존 위치에서 크게 다른 경우, 이를 무시하는 최대 횟수.
		 * 주어진 횟수 이상으로 연속적으로 크게 다른 경우에만 반영시킨다. */
		public int spikeIgnoreCount;

		public Params(float alpha) {
			this.alpha = alpha;
			this.spikeDistance = DEF_SPIKE_DISTANCE;
			this.spikeIgnoreCount = -1;
		}
		public Params(ConfigNode config) {
			alpha = config.get("alpha").asFloat();
			spikeDistance = config.get("spike_distance").asFloat(DEF_SPIKE_DISTANCE);
			spikeIgnoreCount = config.get("spike_ignore_count").asInt(-1);
		}
	}
	
	private Params m_params;
	private Point m_avg;
	private int m_spikeIgnoreCount;
	
	public static MAPointSmoother create(ConfigNode config) {
		return new MAPointSmoother(new Params(config));
	}
	
	public MAPointSmoother(float alpha) {
		m_params = new Params(alpha);
		m_spikeIgnoreCount = 0; 
	}
	
	public MAPointSmoother(Params params) {
		m_params = params;
		m_spikeIgnoreCount = 0; 
	}

	@Override
	public Point getLocation() {
		return m_avg;
	}

	@Override
	public void setLocation(Point pt) {
		m_avg = pt;
	}

	@Override
	public Point smooth(Point measurement) {
		if ( m_avg == null ) {
			return m_avg = measurement.clone();
		}
		
		// 커서의 위치가 이전 것보다 아주 많이 차이가 나는 경우, 일정 횟수 만큼은
		// 무시하다가 그 후에도 계속 그런 경우 반영시킨다.
		float alpha = m_params.alpha;
		if ( m_params.spikeDistance > 0
			&& OpenCvJUtils.distanceL2(m_avg, measurement) > m_params.spikeDistance ) {
			if ( ++m_spikeIgnoreCount <= m_params.spikeIgnoreCount ) {
				return m_avg;
			}
			// 차이가 많이 나는 경우에도 alpha만큼만 반영하기 때문에, 다음번에도
			// 바로 반영되기 위해서는 m_ignore_count를 0으로 설정하지 않는다.
			// 뿐만 아니라 alpha 값도 임시로 크게하여 많이 반영되도록 한다.
			s_logger.warn(String.format("ACCEPT TOO MUCH JUMP: dist=%.1f\n",
										OpenCvJUtils.distanceL2(m_avg, measurement)));
			alpha = 0.8f;
		}
		else {
			m_spikeIgnoreCount = 0;
		}
		
		return m_avg = new Point(alpha * measurement.x + (1-alpha) * m_avg.x,
									alpha * measurement.y + (1-alpha) * m_avg.y);
	}
}
