package opencvj.blob;

import org.opencv.core.Mat;



/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface BlobFilter {
	public boolean apply(Blob blob, Mat image);
}
