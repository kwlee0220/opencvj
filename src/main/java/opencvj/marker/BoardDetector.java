package opencvj.marker;

import org.opencv.core.Mat;
import org.opencv.core.Point;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface BoardDetector {
	public Point[] detect(Mat image);
}
