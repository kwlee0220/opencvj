package opencvj.track;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface PointTrackerFactory {
	public PointTracker create(int id);
}