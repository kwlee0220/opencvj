package opencvj.features2d;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.features2d.KeyPoint;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class Template implements AutoCloseable {
	/** 등록 이미지의 식별자. */
	public String id;
	/** DB 등록 영상에서의 물체의 모서리 좌표 값들 */
	public Point[] corners;
	/** 등록 이미지의 크기(해상도). */
	public Size imageSize;
	/** 물체의 특징점들. */
	public KeyPoint[] keypoints;
	/** 각 특징점들의 기술자. */
	public Mat descriptors;
	
	@Override
	public String toString() {
		return String.format("%s:%dx%d:%d", id, (int)imageSize.width,
							(int)imageSize.height, keypoints.length);
	}

	@Override
	public void close() {
		if ( descriptors != null ) {
			descriptors.release();
			descriptors = null;
		}
	}
}