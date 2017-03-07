package opencvj.track;

import opencvj.blob.Blob;



/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class BlobTrackable implements Trackable<Blob> {
	/** 추적 중인 blob의 식별자. */
	public String m_id;
	/** 추적된 물체의 위치 */
	public Blob m_location;
	
	public BlobTrackable(String id, Blob location) {
		m_id = id;
		m_location = location;
	}
	
	public BlobTrackable(String id) {
		m_id = id;
		m_location = null;
	}

	@Override
	public String getId() {
		return m_id;
	}
	
	public Blob getBlob() {
		return m_location;
	}

	@Override
	public Blob getLocationAsBlob() {
		return m_location;
	}

	@Override
	public Blob getLocation() {
		return m_location;
	}

	@Override
	public void setLocation(Blob location) {
		m_location = location;
	}
}
