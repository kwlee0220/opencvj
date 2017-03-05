package opencvj.camera;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public enum FlipCode {
	NONE, VERTICAL, HORIZONTAL, BOTH;
	
	public static FlipCode from(int code) {
		return values()[code];
	}
	
	public static FlipCode from(String str) {
		return valueOf(str.toUpperCase());
	}
	
	public void flip(Mat image, Mat flipped) {
		switch ( this ) {
			case NONE:
				if ( image == flipped ) {
					image.copyTo(flipped);
				}
				break;
			case BOTH:
				Core.flip(image, flipped, -1);
				break;
			case VERTICAL:
				Core.flip(image, flipped, 0);
				break;
			case HORIZONTAL:
				Core.flip(image, flipped, 1);
				break;
			default:
				throw new RuntimeException();
		}
	}
	
	public Point flip(Point pt, Size boundary) {
		switch ( this ) {
			case NONE:
				return new Point(pt.x, pt.y);
			case VERTICAL:
				return new Point(pt.x, boundary.height - pt.y - 1);
			case HORIZONTAL:
				return new Point(boundary.width - pt.x - 1, pt.y);
			case BOTH:
				return new Point(boundary.width - pt.x - 1, boundary.height - pt.y - 1);
			default:
				throw new RuntimeException();
		}
	}
	
	public List<Point> flip(List<Point> srcPts, Size boundary) {
		List<Point> dstPts = new ArrayList<Point>(srcPts.size());
		for ( Point pt: srcPts ) {
			dstPts.add(flip(pt, boundary));
		}
		
		return dstPts;
	}
	
	public Point[] flip(Point[] srcPts, Size boundary) {
		Point[] dstPts = new Point[srcPts.length];
		for ( int i =0; i < srcPts.length; ++i ) {
			dstPts[i] = flip(srcPts[i], boundary);
		}
		
		return dstPts;
	}
	
	public Point[] reorder(Point[] pts) {
		Point[] reordereds = new Point[pts.length];
		if ( this == BOTH ) {
//			for ( int i =0; i < pts.length; ++i ) {
//				reordereds[i] = pts[pts.length-i-1];
//			}
			reordereds[0] = pts[2];
			reordereds[1] = pts[3];
			reordereds[2] = pts[0];
			reordereds[3] = pts[1];
		}
		else if ( this == VERTICAL ) {
			reordereds[0] = pts[3];
			reordereds[1] = pts[2];
			reordereds[2] = pts[1];
			reordereds[3] = pts[0];
		}
		else if ( this == HORIZONTAL ) {
			reordereds[0] = pts[1];
			reordereds[1] = pts[0];
			reordereds[2] = pts[2];
			reordereds[3] = pts[3];
		}
		
		return reordereds;
	}
}
