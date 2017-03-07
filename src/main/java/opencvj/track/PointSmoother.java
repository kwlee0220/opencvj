package opencvj.track;

import org.opencv.core.Point;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface PointSmoother {
	public Point smooth(Point measurement);
	
	public Point getLocation();
	public void setLocation(Point pt);
}
