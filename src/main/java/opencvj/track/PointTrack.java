package opencvj.track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.opencv.core.Point;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class PointTrack {
	private int m_id;
	private TrackState m_state;
	private Point m_pt;
	
	public PointTrack(int id, TrackState state, Point pt) {
		m_id = id;
		m_state = state;
		m_pt = pt;
	}
	
	public int getId() {
		return m_id;
	}
	
	public TrackState getState() {
		return m_state;
	}
	
	public Point getLocation() {
		return m_pt;
	}
	
	@Override
	public String toString() {
		String posStr = "?";
		if ( m_pt != null ) {
			posStr = String.format("%.0f,%.0f", m_pt.x, m_pt.y);
		}
		return String.format("%s:{%d, %s}", m_state.toString(), m_id, posStr);
	}
	
	public static List<SimplePointTrackable> toTrackables(List<Point> pts) {
		List<SimplePointTrackable> trackables = new ArrayList<SimplePointTrackable>();
		for ( Point pt: pts ) {
			trackables.add(new SimplePointTrackable(pt));
		}
		
		return trackables;
	}
	
	public static List<SimplePointTrackable> toTrackables(Point... pts) {
		List<SimplePointTrackable> trackables = new ArrayList<SimplePointTrackable>();
		for ( Point pt: pts ) {
			trackables.add(new SimplePointTrackable(pt));
		}
		
		return trackables;
	}
	
	public static Point[] toPoints(List<PointTrack> tracks) {
		Point[] pts = new Point[tracks.size()];
		for ( int i =0; i < pts.length; ++i ) {
			pts[i] = tracks.get(i).m_pt;
		}
		
		return pts;
	}
	
	public static void sortByTrackId(List<PointTrack> targets) {
		Collections.sort(targets, TRACK_COMP);
	}
	
	private final static Comparator<PointTrack> TRACK_COMP = new Comparator<PointTrack>() {
		@Override
		public int compare(PointTrack o1, PointTrack o2) {
			return o1.m_id - o2.m_id;
		}
	};
}
