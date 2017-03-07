package opencvj.track;

import opencvj.blob.Blob;



/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface Trackable<L> {
	public String getId();
	
	public Blob getLocationAsBlob();
	
	public L getLocation();
	public void setLocation(L location);
}
