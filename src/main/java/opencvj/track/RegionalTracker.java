package opencvj.track;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;

import opencvj.OpenCvJException;
import opencvj.OpenCvJUtils;
import opencvj.blob.Blob;
import utils.config.ConfigNode;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public abstract class RegionalTracker<L,T extends Trackable<L>> implements Tracker<L,T> {
	public static final float DEF_SEARCH_WINDOW_SCALE = 1.25f;
	public static final int DEF_MAX_MISSING_COUNT = 4;
	
	public static class Params {
		/** 이전 추적에서 검출된 물체의 위치에서 어느 정도 확장된 영역에서 새 이미지에서 해당 물체의
		 * 위치를 찾는다.
		 * 이전 이미지에서 검출된 물체의 boudning box에서 가로와 세로 길이를 주어진 ratio 만큼 확장한
		 * box 영역에서만 대상 물체의 위치를 검색한다.
		 * 예를들어 앞선 bounding box의 넓이가 w 높이가 h이고, ratio가 각각 wr, hr인 경우,
		 * 새로운 검색 영역은 넓이가 w*wr, 높이가 h*hr인 사각영역에서 물체 추적이 시행된다.
		 */
		public float searchWindowScale =DEF_SEARCH_WINDOW_SCALE;
		public int maxMissingCount =DEF_MAX_MISSING_COUNT;
		
		public Params(ConfigNode config) {
			searchWindowScale = config.get("searchMarginRatio")
												.asFloat(DEF_SEARCH_WINDOW_SCALE);
			maxMissingCount = config.get("maxMissingCount")
												.asInt(DEF_MAX_MISSING_COUNT);
		}
	}
	
	protected Params m_params;
	private T m_target;
	protected TrackState m_state;
	private Blob m_window;
	private int m_lostCount;
	
	protected abstract L locate(Mat image);
	protected abstract L locate(Mat image, Blob window);
	
	protected RegionalTracker(Params params) {
		m_params = params;
	}
	
	public T getTarget() {
		return m_target;
	}
	
	public void setTarget(T target) {
		m_target = target;
		m_window = target.getLocationAsBlob();
		m_state = (m_window != null) ? TrackState.TRACKED : TrackState.LOST;
		m_lostCount = 0;
	}
	
	public TrackState getTrackState() {
		return m_state;
	}
	
	public Blob getSearchWindow() {
		return m_window;
	}

	@Override
	public TrackState track(Mat image) {
		if ( m_target == null ) {
			throw new IllegalStateException("target has not been set");
		}
		if ( m_params.searchWindowScale < 0 ) {
			throw new OpenCvJException(String.format("invalid parameter: 'searchMarginRatio' "
													+ "must be greater than or equal to 0: (%.2f)",
													m_params.searchWindowScale));
		}
		
		if ( m_state == TrackState.LOST ) {
			m_window = new Blob(new Rect(0, 0, (int)image.size().width, (int)image.size().height));
		}
		else {
			m_window = calcSearchWindow(image);
		}
		
		Size imgSize = image.size();
		Size winSize = m_window.boundingBox().size();
		winSize.width = Math.min(winSize.width, imgSize.width);
		winSize.height = Math.min(winSize.height, imgSize.height);

		L location;
		if ( !winSize.equals(imgSize) ) {
			location = locate(image, m_window);
		}
		else {
			location = locate(image);
		}

		if ( location != null ) {
			m_target.setLocation(location);
			m_state = TrackState.TRACKED;
			m_lostCount = 0;
		}
		else if ( ++m_lostCount >= m_params.maxMissingCount ) {
			m_state = TrackState.LOST;
		}
		else if ( m_state == TrackState.TRACKED ) {
			m_state = TrackState.TEMP_LOST;
		}

		return m_state;
	}
	
	private Blob calcSearchWindow(Mat image) {
		Blob window = (m_state == TrackState.TRACKED)
				? m_target.getLocationAsBlob() : m_window;
		window = window.scale(m_params.searchWindowScale, m_params.searchWindowScale);
		
		Size imgSize = image.size();
		
		boolean fullyContains = true;
		for ( Point pt: window.contour() ) {
			if ( pt.x < 0 || pt.x >= imgSize.width || pt.y < 0 && pt.y >= imgSize.height ) {
				fullyContains = false;
				break;
			}
		}
		if ( fullyContains ) {
			return window;
		}
		else {
			return Blob.intersect(window, new Blob(OpenCvJUtils.toCvPoints(imgSize)), imgSize);
		}
				
//		int margin = (int)Math.round(m_params.searchWindowScale
//									* Math.sqrt(box.width * box.height));
//		return OpenCvJUtils.expand(box, margin, margin, margin, margin, image.size());
	}
	
}
