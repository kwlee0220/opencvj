package opencvj.marker;

import java.util.Arrays;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import opencvj.MatConvas;
import opencvj.OpenCvJLoader;
import opencvj.track.MultiPointTracker;
import opencvj.track.PointTrack;
import utils.config.ConfigNode;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class BoardTracker {
	private final BoardDetector m_detector;
	private final MultiPointTracker m_tracker;
	
	public static BoardTracker create(OpenCvJLoader loader, ConfigNode config) throws Exception {
		BoardDetector detector = BlackFrameBoardDetector.create(loader, config.get("board"));
		MultiPointTracker ptTracker = MultiPointTracker.create(config);
		return new BoardTracker(detector, ptTracker);
	}
	
	public BoardTracker(BoardDetector detector, MultiPointTracker tracker) {
		m_detector = detector;
		m_tracker = tracker;
	}

	public Point[] track(Mat image) {
		return track(image, null);
	}

	public Point[] track(Mat image, MatConvas debugConvas) {
		Point[] pts = m_detector.detect(image);
		return pts != null ? track(pts, debugConvas) : track(new Point[0], debugConvas);
	}

	public Point[] track(Point[] pts) {
		return track(pts, null);
	}

	public Point[] track(Point[] pts, MatConvas debugConvas) {
		List<PointTrack> tracks = m_tracker.trackPoints(Arrays.asList(pts), debugConvas);
		PointTrack.sortByTrackId(tracks);
		pts = PointTrack.toPoints(tracks);

		return (pts.length > 0 && pts[0] != null) ? pts : null;
	}
}
