package opencvj.track;

import org.opencv.core.Point;

import utils.config.ConfigNode;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class PointTracker {
	private static final int DEF_DETECT_IGNORE_COUNT = 2;
	private static final int DEF_LOST_IGNORE_COUNT = 2;
	private static final long DEF_LOST_IGNORE_MILLIS = Long.MAX_VALUE;
	
	public static class Params {
		/** 최초 검출 무시 횟수.
		 * 추적되지 않던 위치 값이 새로 검출된 경우, 이를 무시하는 횟수.
		 * 즉, 새 위치 값이 입력되어도 바로 이를 추적하지 않고, 일정 횟수 동안은
		 * 검출되지 않은 것으로 간주한다.
		 * 0이거나 음수인 경우는 바로 검출된 것으로 간주한다.
		 */
		public int detectIgnoreCount;
		/** 위치 값 검출 실패 허용 최대 시간.
		 * 위치 값 검출에 실패하도 주어지 시간 동안은 검출한 것으로 간주한다.
		 * 간주된 위치 값은 이전 영상에서 검출된 좌표를 사용한다.
		 * 0이거나  음수인 경우는 바로 검출에 실패한 것으로 간주한다.
		 */
		public int lostIgnoreCount;
		/** 위치 값 검출 실패 허용 최대 시간.
		 * 위치 값 검출에 실패하도 주어지 시간 동안은 검출한 것으로 간주한다.
		 * 간주된 위치 값은 이전 영상에서 검출된 좌표를 사용한다.
		 * 0이거나  음수인 경우는 바로 검출에 실패한 것으로 간주한다.
		 */
		public long lostIgnoreMillis;
		
		public Params() { }
		
		public Params(ConfigNode config) {
			detectIgnoreCount = config.get("detect_ignore_count").asInt(DEF_DETECT_IGNORE_COUNT);
			lostIgnoreCount = config.get("lost_ignore_count").asInt(DEF_LOST_IGNORE_COUNT);
			lostIgnoreMillis = config.get("lost_ignore_millis").asLong(DEF_LOST_IGNORE_MILLIS);
		}
	}
	
	private final Params m_params;
	private final PointSmoother m_smoother;
	
	private int m_id = -1;
	private PointTrackable m_target;
	private TrackState m_state;
	private Point m_estimated;
	
	private int m_detectIgnoreCount;
	private int m_lostCount;
	private long m_lostMillis;
	
	public PointTracker(int id, Params params, PointSmoother smoother) {
		m_params = params;
		m_smoother = smoother;
		
		m_id = id;
		m_state = TrackState.LOST;
		m_detectIgnoreCount = 0; 
		m_lostCount = 0;
	}
	
	public PointTracker(int id, Params params) {
		this(id, params, new NoActionPointSmoother());
	}
	
	public int getId() {
		return m_id;
	}
	
	public Params getParams() {
		return m_params;
	}
	
	public PointTrackable getTarget() {
		return m_target;
	}
	
	public TrackState getState() {
		return m_state;
	}
	
	public Point getLocation() {
		return m_estimated;
	}
	
	public PointTrack newPointTrack() {
		return new PointTrack(m_id, m_state, m_estimated);
	}
	
	public PointTrack setLocation(Point loc) {
		switch ( m_state ) {
			case TRACKED:	// 계속 추적중인 경우
			case TRACKED_NEW:
				break;
			case LOST:	// 새로 등장한 경우 -> ON_DECK으로 이동
				m_detectIgnoreCount = 0;
			case ON_DECK:
				m_state = (++m_detectIgnoreCount > m_params.detectIgnoreCount)
						? TrackState.TRACKED : TrackState.ON_DECK;
				break;
			case TEMP_LOST:
				m_lostCount = 0;
				m_state = TrackState.TRACKED;
				break;
			default:
				throw new IllegalStateException("invalid " + getClass().getSimpleName()
												+ " state: " + m_state);
		}
		
		m_smoother.setLocation(loc);
		m_estimated = loc;
		
		return newPointTrack();
	}

	public PointTrack track(PointTrackable target) {
		PointTrack track = track(target.getLocation());
		m_target = target;
		
		return track;
	}

	public PointTrack track(Point target) {
		TrackState prevState = m_state;	// just for Q-mark
		switch ( m_state ) {
			case TRACKED_NEW:
				m_state = TrackState.TRACKED;
			case TRACKED:	// 계속 추적중인 경우
				break;
			case LOST:	// 새로 등장한 경우 -> ON_DECK으로 이동
				m_detectIgnoreCount = 0;
			case ON_DECK:
				m_state = (++m_detectIgnoreCount > m_params.detectIgnoreCount)
						? TrackState.TRACKED_NEW : TrackState.ON_DECK;
				break;
			case TEMP_LOST:
				m_lostCount = 0;
				m_state = TrackState.TRACKED;
				break;
			default:
				throw new IllegalStateException("invalid " + getClass().getSimpleName()
												+ " state: " + m_state);
		}
//		if ( prevState != m_state ) System.out.printf("%s -> %s%n", prevState, m_state);
		
		m_estimated = m_smoother.smooth(target);
		
		return newPointTrack();
	}

	public PointTrack lost() {
		TrackState prevState = m_state;	// just for Q-mark
		switch ( m_state ) {
			case LOST:
				break;
			case TRACKED:
			case TRACKED_NEW:
				m_lostCount = 0;
				m_lostMillis = System.currentTimeMillis();
			case TEMP_LOST:
				if ( ++m_lostCount > m_params.lostIgnoreCount
					|| (System.currentTimeMillis()-m_lostMillis) > m_params.lostIgnoreMillis ) {
					m_estimated = null;
					m_state = TrackState.LOST;
				}
				else {
					m_state = TrackState.TEMP_LOST;
				}
				break;
			case ON_DECK:
				m_detectIgnoreCount = 0;
				m_state = TrackState.LOST;
				break;
			default:
				throw new IllegalStateException("invalid " + getClass().getSimpleName()
												+ " state: " + m_state);
		}
//		if ( prevState != m_state ) System.out.printf("%s -> %s%n", prevState, m_state);

		return newPointTrack();
	}
	
	public boolean isTracking() {
		switch ( m_state ) {
			case TRACKED:
			case TRACKED_NEW:
			case TEMP_LOST:
				return true;
			default:
				return false;
		}
	}
	
	@Override
	public String toString() {
		String posStr = "?";
		
		if ( m_estimated != null ) {
			posStr = String.format("%.0f,%.0f", m_estimated.x, m_estimated.y);
		}
		return String.format("%s{%s}", m_state.toString(), posStr);
	}
}
