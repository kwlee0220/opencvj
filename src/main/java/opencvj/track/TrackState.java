package opencvj.track;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public enum TrackState {
	/** 추적 대상 물체의 위치가 파악되었지만 아직 확실하지 않은 상태. */
	ON_DECK,
	TRACKED_NEW,
	/** 추적 대상 물체의 위치가 파악된 상태. */
	TRACKED,
	/** 추적 대상 물체의 위치가 일시적으로 놓친 상태. */
	TEMP_LOST,
	/** 추적 대상 물체의 위치를 놓친 상태. */
	LOST
}
