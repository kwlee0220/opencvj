package opencvj.track;

import org.opencv.core.Mat;
import org.opencv.core.Rect;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface Backprojector extends AutoCloseable {
	public void load(Mat image, Mat mask);
	public void clear();
	
	public void backproject(Mat image, Mat proj);
	public void backproject(Mat image, Rect roi, Mat proj);
}