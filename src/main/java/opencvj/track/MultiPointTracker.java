package opencvj.track;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opencv.core.Point;

import opencvj.MatConvas;
import opencvj.OpenCvJ;
import opencvj.OpenCvJUtils;
import utils.Permutation;
import utils.config.ConfigNode;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class MultiPointTracker {
	public static class Params {
		public float m_distThreshold;

		public Params(float dist) {
			m_distThreshold = dist;
		}
		
		public Params(ConfigNode config) {
			m_distThreshold = config.get("distance_threshold").asFloat(-1);
		}
	};
	
	public static class Match {
		public PointTracker m_tracker;
		public PointTrackable m_trackable;
		
		Match(PointTracker tracker, PointTrackable trackable) {
			m_tracker = tracker;
			m_trackable = trackable;
		}
	};

	private final Params m_params;
	private final PointTrackerFactory m_trackerFact;
	private List<PointTracker> m_trackers = new ArrayList<PointTracker>();
	private int m_seqno = -1;
	
	public static MultiPointTracker create(ConfigNode config) {
		return new MultiPointTracker(new Params(config),
										new ConfigPointTrackerFactory(config));
	}
	
	public MultiPointTracker(Params params, PointTrackerFactory trackerFact) {
		m_params = params;
		m_trackerFact = trackerFact;
	}
	
	public MultiPointTracker(float distThreshold, PointTrackerFactory trackerFact) {
		this(new Params(distThreshold), trackerFact);
	}
	
	public MultiPointTracker(PointTrackerFactory trackerFact) {
		this(-1, trackerFact);
	}
	
	public List<PointTrack> trackPoints(List<Point> targets, MatConvas convas) {
		List<SimplePointTrackable> trackables = new ArrayList<SimplePointTrackable>();
		for ( Point target: targets ) {
			trackables.add(new SimplePointTrackable(target));
		}
		
		return track(trackables, convas);
	}
	
	public List<PointTrack> track(List<? extends PointTrackable> targets, MatConvas convas) {
		// 이전 손에 해당하는 손가락과 새로 검출된 손의 손가락 사이의 매핑을 구한다.
		List<Match> matches = matchTargets(m_trackers, targets, m_params.m_distThreshold);
		
		List<PointTrack> tracks = new ArrayList<PointTrack>();
		for ( Match match: matches ) {
			PointTracker tracker = match.m_tracker;
			final PointTrackable target = match.m_trackable;

			final Point lastPt = (tracker != null) ? tracker.getLocation() : null;
			
			if ( tracker != null && target != null ) {
				// 이전 영상까지 추적되던 대상이 다시 새 영상에서도 검출되어 매칭된 경우.
				// 주어진 대상 물체의 위치를 추적기에 반영하여 추정치를 다시 계산한다.
				//
				tracks.add(tracker.track(target));
			}
			else if ( tracker != null && target == null ) {
				// 기존 손가락이 새 영상에서는 검출되지 않은 경우.
				// 새 영상에서 검출되지 않더라도, 설정에 따라 일정기간 동안 손가락이 존재하는 것으로
				// 간주하는 경우는  'targets'에 해당 추적 물체의 위치 정보를 추가한다.
				PointTrack track = tracker.lost();
				if ( track.getState() == TrackState.LOST ) {
					m_trackers.remove(tracker);
				}
				tracks.add(track);
			}
			else {
				// 이전 영상에는 없었지만, 새 영상에서는 검출된 손가락의 경우 처리
				//
				match.m_tracker = tracker = m_trackerFact.create(++m_seqno);
				m_trackers.add(tracker);
				tracks.add(tracker.track(target));
			}
			
			if ( convas != null ) {
				final String id = ""+tracker.getId();
				final Point pos = tracker.getLocation();
				final Point idPos = (pos != null) ? new Point(pos.x-id.length(), pos.y) : null;
					
				switch ( tracker.getState() ) {
					case TRACKED:
					case TRACKED_NEW:
						convas.drawLine(lastPt, pos, OpenCvJ.GREEN, 1);
						convas.drawCircle(pos, 4, OpenCvJ.GREEN, 2);
						convas.drawString(id, idPos, 1.2, OpenCvJ.RED);
						break;
					case ON_DECK:
						if ( lastPt != null ) {
							convas.drawLine(lastPt, pos, OpenCvJ.WHITE, 1);
						}
						convas.drawCircle(pos, 4, OpenCvJ.WHITE, 2);
						convas.drawString(id, idPos, 1.2, OpenCvJ.RED);
						break;
					case TEMP_LOST:
						convas.drawLine(lastPt, pos, OpenCvJ.YELLOW, 1);
						convas.drawCircle(pos, 4, OpenCvJ.YELLOW, 2);
						convas.drawString(id, idPos, 1.2, OpenCvJ.RED);
						break;
					case LOST:
						break;
				}
			}
		}
		
		return tracks;
	}
	
	public static <T extends PointTrackable>
	List<Match> matchTargets(List<PointTracker> trackers, List<T> targets, float distThreshold) {
		Point[] fromPts = new Point[trackers.size()];
		for ( int i =0; i < fromPts.length; ++i ) {
			fromPts[i] = trackers.get(i).getLocation();
		}
		
		Point[] toPts = new Point[targets.size()];
		for ( int i =0; i < toPts.length; ++i ) {
			toPts[i] = targets.get(i).getLocation();
		}
		
		int[] mapping = findBestMatch(fromPts, toPts, distThreshold);
		
		boolean[] visiteds = allocateArray(targets.size(), false);
		List<Match> matches = new ArrayList<Match>();
		
		for ( int i =0; i < mapping.length; ++i ) {
			final PointTracker track = trackers.get(i);
			
			if ( mapping[i] >= 0 ) {
				matches.add(new Match(track, targets.get(mapping[i])));
				visiteds[mapping[i]] = true;
			}
			else {
				matches.add(new Match(track, null));
			}
		}
		for ( int i =0; i < targets.size(); ++i ) {
	 		if ( !visiteds[i] ) {
				matches.add(new Match(null, targets.get(i)));
			}
		}
		
		return matches;
	}
	
	public static <T extends PointTrackable> List<T> toTrackables(List<Match> matches) {
		List<T> trackables = new ArrayList<T>();
		for ( Match match: matches ) {
			switch ( match.m_tracker.getState() ) {
				case LOST:
				case ON_DECK:
					break;
				default:
					trackables.add((T)match.m_trackable);
					break;
			}
		}
		
		return trackables;
	}
	
	public static <T extends PointTrackable> Point[] toPoints(List<Match> matches) {
		List<Point> ptList = new ArrayList<Point>();
		for ( Match match: matches ) {
			switch ( match.m_tracker.getState() ) {
				case LOST:
				case ON_DECK:
					break;
				default:
					ptList.add(match.m_tracker.getLocation());
					break;
			}
		}
		
		return ptList.toArray(new Point[ptList.size()]);
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for ( PointTracker tracker: m_trackers ) {
			builder.append(tracker.toString()).append(' ');
		}
		builder.setLength(builder.length()-1);
		
		return builder.toString();
	}
	
	private static int[] findBestMatch(Point[] fromPts, Point[] toPts, float distThreshold) {
		boolean[] flags1 = findGoodPoints(fromPts, toPts, distThreshold);
		boolean[] flags2 = findGoodPoints(toPts, fromPts, distThreshold);

		Point[] pts1 = collectPoints(fromPts, flags1);
		Point[] pts2 = collectPoints(toPts, flags2);
		int[] tmp_mappings = findBestMatchSub(pts1, pts2, distThreshold);
		
		int[] mappings = allocateArray(fromPts.length, -1); 
		for ( int i =0; i < tmp_mappings.length; ++i ) {
			int idx1 = getIndex(i, flags1);
			int idx2 = tmp_mappings[i] >= 0 ? getIndex(tmp_mappings[i], flags2) : -1;
			mappings[idx1] = idx2;
		}

		return mappings;
	}
	
	private static int[] findBestMatchSub(Point[] fromPts, Point[] toPts, float distThreshold) {
		if ( fromPts.length == 0 ) {
			return new int[0];
		}
		else if ( toPts.length == 0 ) {
			return allocateArray(fromPts.length, -1);
		}

		boolean swapped = fromPts.length > toPts.length;
		int[] best = (swapped) ? findBestMatchPermuation(toPts, fromPts, distThreshold)
								: findBestMatchPermuation(fromPts, toPts, distThreshold);
		if ( swapped ) {
			int[] tmp = allocateArray(fromPts.length, -1); 
			for ( int i =0; i < best.length; ++i ) {
				if ( best[i] >= 0 ) {
					tmp[best[i]] = i;
				}
			}
			best = tmp;
		}
		
		return best;
	}
	
	private static int[] findBestMatchPermuation(Point[] pts1, Point[] pts2, float distThreshold) {
		int[] indexes = new int[pts2.length];
		for ( int i =0; i < indexes.length; ++i ) {
			indexes[i] = i;
		}

		double minSum = 1e10;
		int[] minPermuation = new int[indexes.length];
		for ( int[] p: Permutation.create(indexes) ) {
			double sum = 0;
			for ( int i =0; i < pts1.length; ++i ) {
				double dist = OpenCvJUtils.distanceL2(pts1[i], pts2[p[i]]);
				if ( distThreshold > 0 && dist > distThreshold ) {
					sum = 1e10;
					break;
				}
				sum += dist;
			}
			if ( sum < minSum ) {
				minPermuation = p;
				minSum = sum;
			}
		}
		
		if ( minSum == 1e10 ) {
			Arrays.fill(minPermuation, -1);
		}
		return Arrays.copyOf(minPermuation, pts1.length);
	}
	
	private static boolean[] findGoodPoints(Point[] targets, Point[] pts, float distThreshold) {
		if ( distThreshold <= 0 ) {
			return allocateArray(targets.length, true); 
		}
		
		boolean[] validityBits = allocateArray(targets.length, false); 
		for ( int i =0; i < targets.length; ++i ) {
			Point target = targets[i];
			if ( target != null ) {
				for ( int j =0; j < pts.length; ++j ) {
					if ( pts[j] != null
						&& OpenCvJUtils.distanceL2(target, pts[j]) <= distThreshold ) {
						validityBits[i] = true;
						break;
					}
				}
			}
		}

		return validityBits;
	}
	
	private static Point[] collectPoints(Point[] pts, boolean[] validity) {
		List<Point> collecteds = new ArrayList<Point>();
		for ( int i =0; i < validity.length; ++i ) {
			if ( validity[i] ) {
				collecteds.add(pts[i]);
			}
		}
		
		return collecteds.toArray(new Point[collecteds.size()]);
	}
	
	private static int getIndex(int idx, boolean[] flags) {
		for ( int i =0; i < flags.length; ++i ) {
			if ( flags[i] ) {
				if ( --idx < 0 ) {
					return i;
				}
			}
		}
		return -1;
	}
	
	private static int[] allocateArray(int length, int initValue) {
		int[] array = new int[length];
		Arrays.fill(array, initValue);
		
		return  array;
	}
	
	private static boolean[] allocateArray(int length, boolean initValue) {
		boolean[] array = new boolean[length];
		Arrays.fill(array, initValue);
		
		return  array;
	}
	
//	private void draw(MatConvas convas, List<Track> lastTracks) {
//		for ( Track track: tracks ) {
//			final PointTracker tracker = track.m_tracker;
//			final Point pt = track.m_fingertip.m_tipPos;
//
//			PointTracker.State state = tracker.getState();
//			if ( state == State.TRACK_TRACKED || state == State.TRACK_TEMP_LOST ) {
//				Scalar color = (state == State.TRACK_TRACKED) ? OpenCvJ.GREEN : OpenCvJ.YELLOW;
//				int idx = findTrack(track.m_fingertip.m_id, lastTracks);
//				if ( idx >= 0 ) {	// 이전에서 이동한 경우
//					convas.drawLine(lastTracks.get(idx).m_fingertip.m_tipPos, pt, color, 2);
//					convas.drawCircle(pt, 4, color, 2);
////					cv::circle(convas, tracks[i].m_tip, params.dist_threshold, YELLOW, 1, CV_AA);
//				}
//				else {	// 새로 추적되기 시작한 경우
//					convas.drawCircle(pt, 12, OpenCvJ.RED, 5);
//					convas.drawCircle(pt, 4, OpenCvJ.RED, 2);
//				}
//			}
//			else if ( state == State.TRACK_ON_DECK ) {
//				convas.drawCircle(tracker.getLocation(), 4, OpenCvJ.WHITE, 2);
//			}
//		}
//	}
}
