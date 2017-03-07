package opencvj.track;

import org.opencv.core.Point;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class NoActionPointSmoother implements PointSmoother {
	private Point m_pt;

	@Override
	public Point smooth(Point measurement) {
		return m_pt = measurement;
	}

	@Override
	public Point getLocation() {
		return m_pt;
	}

	@Override
	public void setLocation(Point pt) {
		m_pt = pt;
	}
}
