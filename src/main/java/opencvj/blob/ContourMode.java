package opencvj.blob;

import org.opencv.imgproc.Imgproc;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public enum ContourMode {
	CV_RETR_EXTERNAL(Imgproc.RETR_EXTERNAL),
	CV_RETR_LIST(Imgproc.RETR_LIST),
	CV_RETR_CCOMP(Imgproc.RETR_CCOMP),
	CV_RETR_TREE(Imgproc.RETR_TREE);
	
	private int m_code;
	
	private ContourMode(int code) {
		m_code = code;
	}
	
	public int getCode() {
		return m_code;
	}
}
