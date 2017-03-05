package opencvj.blob;


import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import opencvj.OpenCvJException;



/**
 * 
 * @author Kang-Woo Lee
 */
public interface DeltaAwareForegroundDetector extends ForegroundDetector {
	public void detectBackgroundDelta(Mat image, Point[] corners, Mat delta32f, Mat fgMask)
		throws OpenCvJException;
	
	public List<Blob> extractForegroundBlobs(Mat image, Point[] corners, Mat delta32f, Mat fgMask);
	public Blob extractLargestForegroundBlob(Mat image, Point[] corners, Mat delta32f, Mat fgMask);
	public List<Blob> extractKLargestForegroundBlobs(Mat image, int k, Point[] corners, Mat delta32f,
													Mat fgMask);
}