package opencvj.track;

import org.opencv.core.Mat;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface Tracker<L, T extends Trackable<L>> {
	/**
	 * 주어진 이미지내에서 대상 물체 위치를 추출한다.
	 *
	 * @param image		물체를 추적하고자 하는 대상 이미지.
	 * @return	물체 추적 정보.
	 */
	TrackState track(Mat image);

	T getTarget();
	TrackState getTrackState();
}
