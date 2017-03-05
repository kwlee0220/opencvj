package opencvj;

import camus.service.image.Color;

import etri.service.image.SwingBasedImageView;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;

import opencvj.blob.Blob;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class OpenCvView extends SwingBasedImageView {
	public void draw(Mat mat) {
		draw(Mats.toBufferedImage(mat));
	}
	
	public void draw(MatConvas convas) {
		draw(convas.getMat());
	}
	
	public void draw(Point[] pts, Color color, int thickness) {
		drawPolygon(OpenCvJUtils.toPolygon(pts), color, thickness);
	}
	
	public void draw(String str, Point pt, int fontSize, Color color) {
		drawString(str, OpenCvJUtils.toPoint(pt), fontSize, color);
	}
	
	public void draw(Blob blob, Color color, int thickness, Size size) {
		try ( MatConvas convas = new MatConvas(Mat.zeros(size, CvType.CV_8UC3)) ) {
			convas.draw(blob, OpenCvJUtils.toScalar(color), thickness);
			draw(convas);
		}
	}
	
	public void draw(Blob blob, Color color, Color holeColor, Size size) {
		try ( MatConvas convas = new MatConvas(Mat.zeros(size, CvType.CV_8UC3)); ) {
			convas.draw(blob, OpenCvJUtils.toScalar(color), -1);
			for ( Point[] hole: blob.getHoles() ) {
				convas.drawContour(hole, OpenCvJUtils.toScalar(holeColor), -1);
			}
			
			draw(convas);
		}
	}
}
