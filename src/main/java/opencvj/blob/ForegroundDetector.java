package opencvj.blob;

import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.Point;



/**
 * 
 * @author Kang-Woo Lee
 */
public interface ForegroundDetector extends BackgroundModelAware, AutoCloseable {
	public void detectForeground(Mat image, Point[] corners, Mat fgMask);
	
	public List<Blob> extractForegroundBlobs(Mat image, Point[] corners, Mat fgMask);
	public Blob extractLargestForegroundBlob(Mat image, Point[] corners, Mat fgMask);
	public List<Blob> extractKLargestForegroundBlobs(Mat image, int k, Point[] corners, Mat fgMask);
}