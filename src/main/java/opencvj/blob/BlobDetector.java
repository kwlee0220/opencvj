package opencvj.blob;


import org.opencv.core.Mat;

import opencvj.OpenCvJException;



/**
 * 
 * @author Kang-Woo Lee
 */
public interface BlobDetector {
	public void detect(Mat image, Mat blobMask) throws OpenCvJException;
}