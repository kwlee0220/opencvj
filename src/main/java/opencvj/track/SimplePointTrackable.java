package opencvj.track;

import org.opencv.core.Point;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class SimplePointTrackable implements PointTrackable {
	private Point m_pt;
	
	public SimplePointTrackable(Point pt) {
		m_pt = pt;
	}

	@Override
	public Point getLocation() {
		return m_pt;
	}
	
	@Override
	public String toString() {
		int id = -1;
		return String.format("%d{%.0f,%.0f}", id, m_pt.x, m_pt.y);
	}
}