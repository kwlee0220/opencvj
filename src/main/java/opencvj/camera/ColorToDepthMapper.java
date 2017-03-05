package opencvj.camera;

import org.opencv.core.Mat;
import org.opencv.core.Point;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface ColorToDepthMapper {
	public Point map(Point colorCoord, Mat depthImage);
	public Point[] map(Point[] colorCoords, Mat depthImage);
	public void mapMask(Mat colorMask, Mat depthMask, Mat depthImage);
}
