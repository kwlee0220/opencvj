package opencvj.camera;

import org.opencv.core.Mat;
import org.opencv.core.Point;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface DepthToColorMapper {
	public Point map(Point depthCoord, Mat depthImage);
	public Point[] map(Point[] depthCoords, Mat depthImage);
	public void mapMask(Mat depthMask, Mat colorMask, Mat depthImage);
}
