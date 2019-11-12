package opencvj.blob;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.concurrent.GuardedBy;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import opencvj.MatConvas;
import opencvj.Mats;
import opencvj.OpenCvJ;
import opencvj.OpenCvJUtils;
import opencvj.OpenCvViewManager;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class Blob {
	private static final Mat DEFAULT_HIERARCHY = new Mat();
	private static final Point DEFAULT_OFFSET = new Point(0,0);
	
	private final Point[] m_pts;
	private final List<Point[]> m_holes;
	@GuardedBy("this") private double m_area =-1;
	@GuardedBy("this") private Moments m_moments =null;
	@GuardedBy("this") private volatile Rect m_bbox =null;
	@GuardedBy("this") private Point m_centroid =null;
	
	public Blob(final Point[] pts, List<Point[]> holes) {
		m_pts = pts;
		m_holes = holes;
	}
	
	public Blob(final Point[] pts) {
		this(pts, new ArrayList<Point[]>());
	}
	
	public Blob(final Rect rect) {
		this(OpenCvJUtils.toCvPoints(rect));
	}
	
	public int npoints() {
		return m_pts.length;
	}
	
	public Point[] contour() {
		return m_pts;
	}
	
	public boolean hasHole() {
		return m_holes.size() > 0;
	}
	
	public List<Point[]> getHoles() {
		return m_holes;
	}
	
	public synchronized double area() {
		if ( m_area < 0 ) {
			m_area = Imgproc.contourArea(new MatOfPoint(m_pts));
		}
		
		return m_area;
	}
	
	public Rect boundingBox() {
		if ( m_bbox == null ) {
			m_bbox = new Rect();
			if ( m_pts.length > 0 ) {
				MatOfPoint mop = new MatOfPoint(m_pts);
				try {
					m_bbox = Imgproc.boundingRect(mop);
				}
				finally {
					mop.release();
				}
			}
		}
		
		return m_bbox;
	}
	
	public RotatedRect minAreaBox() {
		if ( m_pts.length > 0 ) {
			MatOfPoint2f mop = new MatOfPoint2f(m_pts);
			try {
				return Imgproc.minAreaRect(mop);
			}
			finally {
				mop.release();
			}
		}
		else {
			return null;
		}
	}
	
	public synchronized Point centroid() {
		if ( m_centroid == null ) {
			if ( m_pts.length > 0 ) {
				MatOfPoint mop = new MatOfPoint(m_pts);
				m_moments = Imgproc.moments(mop, false);
				m_centroid = new Point(m_moments.get_m10()/m_moments.get_m00(),
										m_moments.get_m01()/m_moments.get_m00());
				mop.release();
			}
			else {
				m_centroid = new Point(-1,-1);
			}
		}
		
		return m_centroid;
	}
	
	public double perimeter() {
		MatOfPoint2f mop = new MatOfPoint2f(m_pts);
		try {
			return Imgproc.arcLength(mop, true);
		}
		finally {
			mop.release();
		}
	}
	
	public double roundness() {
		double p = perimeter();
		
		return (4 * Math.PI * area()) / (p*p);
	}
	
	public boolean isConvextContour() {
		MatOfPoint mop = new MatOfPoint(m_pts);
		try {
			return Imgproc.isContourConvex(mop);
		}
		finally {
			mop.release();
		}
	}
	
	public double distanceTo(Point pt, boolean measureDist) {
		MatOfPoint2f mop = new MatOfPoint2f(m_pts);
		try {
			return Imgproc.pointPolygonTest(mop, pt, measureDist);
		}
		finally {
			mop.release();
		}
	}
	
	public boolean contains(Point pt) {
		return testIn(pt) >= 0;
	}
	
	public boolean isOnEdge(Point pt) {
		return testIn(pt) == 0;
	}
	
	public double distanceToEdge(Point pt) {
		MatOfPoint2f mop = new MatOfPoint2f(m_pts);
		try {
			return Imgproc.pointPolygonTest(mop, pt, true);
		}
		finally {
			mop.release();
		}
	}
	
	public int testIn(Point pt) {
		MatOfPoint2f mop = new MatOfPoint2f(m_pts);
		try {
			return (int)Imgproc.pointPolygonTest(mop, pt, false);
		}
		finally {
			mop.release();
		}
	}
	
	public void shift(Point offset) {
		for ( int i =0; i < m_pts.length; ++i ) {
			m_pts[i].x += offset.x;
			m_pts[i].y += offset.y;
		}
	}
	
	public Blob approximate(double epsilon) {
		MatOfPoint2f curve = new MatOfPoint2f(m_pts);
		MatOfPoint2f approxCurve = new MatOfPoint2f();
		try {
			Imgproc.approxPolyDP(curve, approxCurve, epsilon, true);
			return new Blob(approxCurve.toArray());
		}
		finally {
			approxCurve.release();
			curve.release();
		}
	}
	
	public static Blob intersect(Blob blob1, Blob blob2, Size bound) {
		Mat mask1 = new Mat(bound, CvType.CV_8UC1, OpenCvJ.ALL_0);
		Mat mask2 = new Mat(bound, CvType.CV_8UC1, OpenCvJ.ALL_0);
		try {
			blob1.draw(mask1, OpenCvJ.ALL_255, Core.FILLED);
			blob2.draw(mask2, OpenCvJ.ALL_255, Core.FILLED);
			Core.bitwise_and(mask1, mask2, mask1);
			
			OpenCvViewManager.show("mask", mask1);
			
			return new BlobExtractor().extractLargestBlob(mask1);
		}
		finally {
			Mats.releaseAll(mask1, mask2);
		}
	}
	
	public Mat crop(final Mat image, Mat cropped) {
		if ( cropped.empty() ) {
			cropped.create(image.size(), image.type());
			cropped.setTo(Scalar.all(0));
		}
		
		Mat mask = Mat.zeros(image.size(), CvType.CV_8UC1);
		try {
			draw(mask, OpenCvJ.WHITE, Core.FILLED);
			image.copyTo(cropped, mask);
			
			return cropped;
		}
		finally {
			mask.release();
		}
	}
	
	public void draw(Mat convas, Scalar color, int thickness) {
		drawContour(convas, m_pts, color, thickness);
	}
	
	public void draw(MatConvas convas, Scalar color, int thickness) {
		drawContour(convas.getMat(), m_pts, color, thickness);
	}
	
	public void draw(Mat convas, Scalar color, Scalar holeColor, int thickness) {
		draw(convas, color, thickness);
		for ( Point[] hole: m_holes ) {
			drawContour(convas, hole, holeColor, thickness);
		}
	}
	
	public static void drawContour(Mat convas, Point[] cont, Scalar color, int thickness) {
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		MatOfPoint mop = new MatOfPoint(cont);
		try {
			contours.add(mop);
			
			Imgproc.drawContours(convas, contours, -1, color, thickness, Core.LINE_AA,
								DEFAULT_HIERARCHY, 0, DEFAULT_OFFSET);
		}
		finally {
			mop.release();
		}
	}
	
	public Blob scale(double scaleX, double scaleY) {
		return scale(scaleX, scaleY, centroid());
	}
	
	public Blob scale(double scaleX, double scaleY, Point anchor) {
		AffineTransform trans1 = new AffineTransform();
		trans1.translate(-anchor.x, -anchor.y);
		
		AffineTransform trans2 = new AffineTransform();
		trans2.scale(scaleX, scaleY);
		trans2.concatenate(trans1);
		
		AffineTransform trans = new AffineTransform();
		trans.translate(anchor.x, anchor.y);
		trans.concatenate(trans2);
		
		Point2D[] tars = new Point2D[m_pts.length];
		Point2D[] srcs = new Point2D[m_pts.length];
		for ( int i =0; i < srcs.length; ++i ) {
			srcs[i] = new Point2D.Double(m_pts[i].x, m_pts[i].y);
		}
		
		trans.transform(srcs, 0, tars, 0, srcs.length);
		
		Point[] tarPts = new Point[m_pts.length];
		for ( int i =0; i < m_pts.length; ++i ) {
			tarPts[i] = new Point(tars[i].getX(), tars[i].getY());
		}
		return new Blob(tarPts);
	}
	
	public static Blob getLargestBlob(List<Blob> blobs) {
		return Collections.min(blobs, Blob.AREA_COMP_DESC);
	}
	
	public static List<Blob> toBlobList(List<Point[]> contours) {
		List<Blob> blobList = new ArrayList<Blob>(contours.size());
		for ( Point[] contour: contours ) {
			blobList.add(new Blob(contour));
		}
		
		return blobList;
	}
	
	public static Comparator<Blob> AREA_COMP_DESC = new Comparator<Blob>() {
		@Override
		public int compare(Blob o1, Blob o2) {
			return Double.compare(o2.area(), o1.area());
		}
	};
	
	public Shape detectShape() {
		Blob approx = approximate(perimeter() * 0.02);
		
		int nvtcs = approx.npoints();
		double area = approx.area();
		
		if ( area < 100 || !approx.isConvextContour() ) {
			return Shape.UNKNOWN;
		}
		else if ( nvtcs == 3 ) {
			return Shape.TRIANGLE;
		}
		else if ( nvtcs <= 6 ) {
			Point[] points = approx.contour();
			
			// get the cosines of all corners
			double[] cosAngles = new double[nvtcs];
			for ( int i =0; i < nvtcs; ++i ) {
				double cos = OpenCvJUtils.calcCosineBtwLines(points[i], points[(i-1+nvtcs)%nvtcs],
															points[(i+1)%nvtcs]);
				cosAngles[i] = cos;
			}
			Arrays.sort(cosAngles);
			
			double minCos = cosAngles[0];
			double maxCos = cosAngles[nvtcs-1];

			// Use the degrees obtained above and the number of vertices
			// to determine the shape of the contour
			if (nvtcs == 4 && minCos >= -0.1 && maxCos <= 0.3) {
				return Shape.RECTANGLE;
			}
			else if (nvtcs == 5 && minCos >= -0.34 && maxCos <= -0.27) {
				return Shape.PENTAGON;
			}
			else if (nvtcs == 6 && minCos >= -0.55 && maxCos <= -0.45) {
				return Shape.HEXAGON;
			}
		}
		else {
			// Detect and label circles
			Rect bbox = boundingBox();
			double radius = bbox.width / 2.0;
			
			if ( Math.abs(1 - ((double)bbox.width / bbox.height)) <= 0.2
				&& Math.abs(1 - (area / (Math.PI * radius*radius))) <= 0.2 ) {
				return Shape.CIRCLE;
			}
		}
		
		return Shape.UNKNOWN;
	}
	
	@Override
	public String toString() {
		return String.format("npts=%d size=%.0f", m_pts.length, area());
	}
}
