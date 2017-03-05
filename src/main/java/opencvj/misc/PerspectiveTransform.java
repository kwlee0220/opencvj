package opencvj.misc;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import opencvj.Mats;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class PerspectiveTransform implements AutoCloseable {
	private Mat m_mat = null;
	
	public static PerspectiveTransform createPerspectiveTransform(Point[] srcPts, Point[] dstPts) {
		PerspectiveTransform trans = new PerspectiveTransform();
		trans.setPerspectiveTransform(srcPts, dstPts);
		
		return trans;
	}
	
	public static PerspectiveTransform createRansacHomography(Point[] srcPts, Point[] dstPts) {
		PerspectiveTransform trans = new PerspectiveTransform();
		trans.setRansacHomography(srcPts, dstPts);
		
		return trans;
	}
	
	public PerspectiveTransform() {
		m_mat = new Mat();
	}
	
	public PerspectiveTransform(Mat mat) {
		m_mat = mat.clone();
	}
	
	public PerspectiveTransform inv() {
		return new PerspectiveTransform(m_mat.inv());
	}
	
	public Mat getTransformMatrix() {
		return m_mat;
	}
	
	public void setPerspectiveTransform(Point[] srcPts, Point[] dstPts) {
		MatOfPoint2f src = new MatOfPoint2f(srcPts);
		MatOfPoint2f dst = new MatOfPoint2f(dstPts);

		Mat saved = m_mat;
		try {
			m_mat = Imgproc.getPerspectiveTransform(src, dst);
		}
		finally {
			src.release();
			dst.release();
			
			if ( saved != null ) {
				saved.release();
			}
		}
	}
	
	public static Mat calcRansacHomography(Point[] srcPts, Point[] dstPts) {
		MatOfPoint2f src = new MatOfPoint2f(srcPts);
		MatOfPoint2f dst = new MatOfPoint2f(dstPts);
		
		try {
			return Calib3d.findHomography(src, dst, Calib3d.RANSAC, 3);
		}
		finally {
			src.release();
			dst.release();
		}
	}
	
	public void setRansacHomography(Point[] srcPts, Point[] dstPts) {
		m_mat.release();
		m_mat = calcRansacHomography(srcPts, dstPts);
	}
	
	public Point[] perform(Point[] src) {
		MatOfPoint2f srcMat = new MatOfPoint2f(src);
		MatOfPoint2f dstMat = new MatOfPoint2f();
		
		try {
			Core.perspectiveTransform(srcMat, dstMat, m_mat);
			return dstMat.toArray();
		}
		finally {
			srcMat.release();
			dstMat.release();
		}
	}
	
	public Point perform(Point src) {
		return perform(new Point[]{src})[0];
	}
	
	public void perform(Mat srcImage, Mat dstImage, Size dstSize) {
		Imgproc.warpPerspective(srcImage, dstImage, m_mat, dstSize);
	}
	
	public static PerspectiveTransform product(PerspectiveTransform trans1, PerspectiveTransform trans2) {
		Mat mat = new Mat();
		Mats.product(trans1.m_mat, trans2.m_mat, mat);
		
		return new PerspectiveTransform(mat);
	}

	@Override
	public void close() {
		if ( m_mat != null ) {
			m_mat.release();
			m_mat = null;
		}
	}
}
