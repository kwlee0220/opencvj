package opencvj;

import java.awt.image.BufferedImage;

import camus.service.geo.Point;
import camus.service.geo.Polygon;
import camus.service.geo.Rectangle;
import camus.service.geo.Size2d;
import camus.service.image.Color;
import camus.service.image.ImageConvas;
import camus.service.vision.Image;
import camus.service.vision.ImageEncoding;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

import opencvj.blob.Blob;
import utils.swing.ImageUtils;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class MatConvas implements ImageConvas, AutoCloseable {
	private final Mat m_mat;
	private final Size2d m_size;
	
	public MatConvas(Mat mat) {
		m_mat = mat;
		m_size = OpenCvJUtils.toSize2d(mat.size());
	}
	
	public MatConvas(Size size) {
		m_mat = Mat.zeros(size, CvType.CV_8UC3);
		m_size = OpenCvJUtils.toSize2d(size);
	}
	
	public MatConvas(MatProxy proxy) {
		m_mat = proxy.getMat();
		
		Size sz = m_mat.size();
		m_size = new Size2d((int)sz.width, (int)sz.height);
	}

	@Override
	public void close() {
		m_mat.release();
	}
	
	public Mat getMat() {
		return m_mat;
	}

	@Override
	public Size2d getConvasSize() {
		return m_size;
	}

	@Override
	public void drawImage(Image image) {
		if ( image.format.encoding != ImageEncoding.JPEG ) {
			throw new IllegalArgumentException("unsupported image encoding: encoding="
												+ image.format.encoding);
		}
		if ( !m_size.equals(image.format.getSize()) ) {
			throw new IllegalArgumentException((String.format(
											"incompatible image size: this=%s image=%s",
											m_size.toString(), image.format.getSize().toString())));
		}
		
		m_mat.put(0, 0, ImageUtils.getRasterBytes(image.toBufferedImage()));
	}

	@Override
	public void draw(BufferedImage bi) {
		Size2d biSize = new Size2d(bi.getWidth(), bi.getHeight());
		if ( !m_size.equals(biSize) ) {
			throw new IllegalArgumentException((String.format(
											"incompatible image size: this=%s convas=%s",
											m_size.toString(), biSize.toString())));
		}
		
		m_mat.put(0, 0, ImageUtils.getRasterBytes(bi));
	}
	
	public void draw(Blob blob, Scalar color, int thickness) {
		blob.draw(m_mat, color, thickness);
	}

	@Override
	public void drawLine(Point fromPt, Point toPt, Color color, int thickness) {
		Core.line(m_mat, OpenCvJUtils.toCvPoint(fromPt), OpenCvJUtils.toCvPoint(toPt),
					OpenCvJUtils.toScalar(color), thickness, Core.LINE_AA, 0);
	}
	
	public void drawLine(org.opencv.core.Point pt1, org.opencv.core.Point pt2, Scalar color,
						int thickness) {
		Core.line(m_mat, pt1, pt2, color, thickness, Core.LINE_AA, 0);
	}

	@Override
	public void drawRect(Rectangle rect, Color color, int thickness) {
		org.opencv.core.Point pt1 = new org.opencv.core.Point(rect.left, rect.top);
		org.opencv.core.Point pt2 = new org.opencv.core.Point(rect.right, rect.bottom);
		
		Core.rectangle(m_mat, pt1, pt2, OpenCvJUtils.toScalar(color), thickness);
	}
	
	public void drawRect(Rect rect, Scalar color, int thickness) {
		drawRect(m_mat, rect, color, thickness);
	}
	
	public static void drawRect(Mat convas, Rect rect, Scalar color, int thickness) {
		org.opencv.core.Point pt1 = new org.opencv.core.Point(rect.x, rect.y);
		org.opencv.core.Point pt2 = new org.opencv.core.Point(rect.x + rect.width -1,
															rect.y + rect.height -1);
		
		Core.rectangle(convas, pt1, pt2, color, thickness);
	}

	public void draw(RotatedRect rrect, Scalar color, int thickness) {
		org.opencv.core.Point[] pts = new org.opencv.core.Point[4];
		rrect.points(pts);
		drawContour(pts, color, thickness);
	}
	
	public void drawCircle(org.opencv.core.Point center, int radius, Scalar color, int thickness) {
		drawCircle(m_mat, center, radius, color, thickness);
	}
	
	public static void drawCircle(Mat convas, org.opencv.core.Point center, int radius,
									Scalar color, int thickness) {
		Core.circle(convas, center, radius, color, thickness, Core.LINE_AA, 0);
	}

	public void drawContour(org.opencv.core.Point[] pts, Scalar color, int thickness) {
		Blob.drawContour(m_mat, pts, color, thickness);
	}

	public void drawOpenContour(org.opencv.core.Point[] contour, Scalar color, int thickness) {
		for ( int i =1; i < contour.length; ++i ) {
			Core.line(m_mat, contour[i-1], contour[i], color, thickness, Core.LINE_AA, 0);
		}
	}

	@Override
	public void drawPolygon(Polygon polygon, Color color, int thickness) {
		Scalar c = OpenCvJUtils.toScalar(color);
		Blob.drawContour(m_mat, OpenCvJUtils.toCvPoints(polygon.points), c, thickness);
	}

	@Override
	public void drawString(String str, Point loc, int fontSize, Color color) {
		Core.putText(m_mat, str, OpenCvJUtils.toCvPoint(loc), Core.FONT_HERSHEY_PLAIN,
						fontSize/10.0, OpenCvJUtils.toScalar(color));
	}
	
	public void drawString(String str, org.opencv.core.Point loc, double fontScale, Scalar color) {
		Core.putText(m_mat, str, loc, Core.FONT_HERSHEY_PLAIN, fontScale, color);
	}
	
	public void drawCross(org.opencv.core.Point center, int radius, float theta,
							Scalar color, int thickness) {
		drawLine(new org.opencv.core.Point(center.x-radius,center.y),
					new org.opencv.core.Point(center.x+radius,center.y), color, thickness);
		drawLine(new org.opencv.core.Point(center.x,center.y-radius),
					new org.opencv.core.Point(center.x,center.y+radius), color, thickness);
	}

	@Override
	public void clear() {
		m_mat.setTo(OpenCvJ.BLACK);
	}
}
