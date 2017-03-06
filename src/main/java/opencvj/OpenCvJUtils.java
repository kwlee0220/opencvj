package opencvj;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import camus.service.geo.Polygon;
import camus.service.geo.Rectangle;
import camus.service.geo.Size2d;
import camus.service.image.Color;
import camus.service.vision.Resolution;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

import com.google.common.collect.Range;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import opencvj.camera.OpenCvJCamera;
import opencvj.misc.PerspectiveTransform;


/**
 * 
 * @author Kang-Woo Lee
 */
public final class OpenCvJUtils {
	private OpenCvJUtils() {
		throw new AssertionError("Should not be called this one: " + OpenCvJUtils.class);
	}
	
	public static <T> T readJsonValue(String jsonStr, Class<T> type)
		throws JsonParseException, JsonMappingException {
		try {
			return OM.readValue(jsonStr, type);
		}
		catch ( IOException nerverHappens ) {
			throw new RuntimeException("should not be here");
		}
	}
	
	public static JsonElement readJsonFile(File jsonFile) throws FileNotFoundException, IOException {
		try ( Reader reader = new FileReader(jsonFile) ) {
			JsonElement jsonElm = new JsonParser().parse(reader);
			return 
		}
	}
	
	public static JsonElement readJsonFile(String jsonStr) throws JsonProcessingException, IOException {
		return OM.readTree(jsonStr);
	}
	
	public static void writeJsonValue(File file, Object node) throws IOException {
		OM.writerWithDefaultPrettyPrinter().writeValue(file, node);
	}
	
	public static String writeAsJsonString(Object node) throws JsonProcessingException {
		return OM.writerWithDefaultPrettyPrinter().writeValueAsString(node);
	}
	
	public static Mat getIntRangeMask(Mat mat, Range<Integer> range) {
		Mat rangeMask = new Mat();
		Core.inRange(mat, new Scalar(range.lowerEndpoint()), new Scalar(range.upperEndpoint()),
					rangeMask);
		return rangeMask;
	}
	
	public static Resolution toResolution(Size size) {
		return new Resolution((int)size.width, (int)size.height);
	}
	
	public static Size toCvSize(camus.service.geo.Size2d sz) {
		return new Size(sz.width, sz.height);
	}
	
	public static Size toCvSize(camus.service.geo.Size2f sz) {
		return new Size(sz.width, sz.height);
	}
	
	public static Size2d toSize2d(Size sz) {
		return new Size2d((int)sz.width, (int)sz.height);
	}
	
	public static Size minus(Size pt1, Size pt2) {
		return new Size(pt1.width - pt2.width, pt1.height - pt2.height);
	}
	
	public static Rect toCvRect(Rectangle rect) {
		return new Rect(toCvPoint(rect.getOffset()), toCvSize(rect.getSize()));
	}
	
	public static Rectangle toRectangle(Rect cvRect) {
		return new Rectangle(OpenCvJUtils.toPoint(cvRect.tl()),
							OpenCvJUtils.toSize2d(cvRect.size()));
	}
	
	public static Point[] getCorners(Size size) {
		return new Point[] {
			new Point(0, 0),	
			new Point(size.width-1, 0),	
			new Point(size.width-1, size.height-1),	
			new Point(0, size.height-1),	
		};
	}
	
	public static Point[] getCorners(Rect rect) {
		final Point tl = rect.tl();
		final Point br = rect.br();
		
		return new Point[]{tl, new Point(br.x, tl.y), br, new Point(tl.x, br.y)};
	}
	
	public static final String toString(Size size) {
		return String.format("%dx%d", (int)size.width, (int)size.height);
	}
	
	public static final String toString(Scalar scalar) {
		StringBuilder builder = new StringBuilder();
		for ( int i =0; i < scalar.val.length; ++i ) {
			builder.append(String.format("%.0f,", scalar.val[i]));
		}
		return builder.toString().substring(0, builder.length()-1);
	}
	
	public static Color toColor(Scalar color) {
		return new Color((int)color.val[2], (int)color.val[1], (int)color.val[0]);
	}
	
	public static Scalar toScalar(Color color) {
		return new Scalar(color.b, color.g, color.r);
	}
	
	public static Point toCvPoint(java.awt.Point pt) {
		return new Point(pt.x, pt.y);
	}
	
	public static Point toCvPoint(camus.service.geo.Point pt) {
		return new Point(pt.xpos, pt.ypos);
	}
	
	public static final String toString(Point pt) {
		return String.format("%.1f,%.1f", pt.x, pt.y);
	}
	
	public static Point plus(Point pt1, Point pt2) {
		return new Point(pt1.x+pt2.x, pt1.y+pt2.y);
	}
	
	public static Point minus(Point pt1, Point pt2) {
		return new Point(pt1.x - pt2.x, pt1.y - pt2.y);
	}
	
	public static Point[] toCvPoints(camus.service.geo.Point[] poly) {
		Point[] pts = new Point[poly.length];
		for ( int i =0; i < pts.length; ++i ) {
			pts[i] = new Point(poly[i].xpos, poly[i].ypos);
		}
		
		return pts;
	}
	
	public static Point[] toCvPoints(Size size) {
		return new Point[] { new Point(0,0), new Point(size.width-1, 0),
							new Point(size.width-1, size.height-1), new Point(0, size.height-1) };
	}
	
	public static Point[] toCvPoints(RotatedRect rrect) {
		Point[] pts = new Point[4];
		rrect.points(pts);
		
		return pts;
	}
	
	public static camus.service.geo.Point toPoint(Point cvPt) {
		return new camus.service.geo.Point((int)Math.round(cvPt.x),
											(int)Math.round(cvPt.y));
	}
	
	public static camus.service.geo.Point[] toPoints(Point[] pts) {
		camus.service.geo.Point[] poly = new camus.service.geo.Point[pts.length];
		for ( int i =0; i < pts.length; ++i ) {
			poly[i] = new camus.service.geo.Point((int)Math.round(pts[i].x), (int)Math.round(pts[i].y));
		}
		
		return poly;
	}
	
	public static Polygon toPolygon(Point[] pts) {
		return new Polygon(toPoints(pts));
	}
	
	public static Point[] toCvPoints(Rect rect) {
		final Point tl = rect.tl();
		final Point br = rect.br();
		
		return new Point[]{tl, new Point(br.x, tl.y), br, new Point(tl.x, br.y)};
	}
	
	public static Rect intersect(Rect rect1, Rect rect2) {
		Point tl = new Point(Math.max(rect1.x, rect2.x), Math.max(rect1.y, rect2.y));
		Point br = new Point(Math.min(rect1.br().x, rect2.br().x),
							Math.min(rect1.br().y, rect2.br().y));
		if ( tl.x < br.x && tl.y < br.y ) {
			return new Rect(tl, br);
		}
		else {
			return null;
		}
	}
	
	public static Rect expand(Rect rect, Size margin, Size boundary) {
		int width = (int)Math.round(margin.width);
		int height = (int)Math.round(margin.height);
		
		return expand(rect, height, height, width, width, boundary);
	}
	
	public static Rect expand(Rect rect, int upMargin, int downMargin, int leftMargin,
								int rightMargin, Size boundary) {
		Rect enlarged = new Rect();

		if ( (enlarged.x = rect.x - leftMargin) < 0 ) {
			enlarged.x = 0;
		}
		int x2 = rect.x + rect.width + rightMargin;
		if ( x2 >= boundary.width ) {
			x2 = (int)boundary.width-1;
		}
		enlarged.width = x2 - enlarged.x;

		if ( (enlarged.y = rect.y - upMargin) < 0 ) {
			enlarged.y = 0;
		}
		int y2 = rect.y + rect.height + downMargin;
		if ( y2 >= boundary.height ) {
			y2 = (int)boundary.height-1;
		}
		enlarged.height = y2 - enlarged.y;

		return enlarged;
	}
	
	public static double distanceL2(Point pt1, Point pt2) {
		double diffx = pt1.x - pt2.x;
		double diffy = pt1.y - pt2.y;

		return Math.sqrt((diffx * diffx) + (diffy * diffy));
	}
	
	public static final int eatFramesMillis(OpenCvJCamera cam, long millis) {
		int count = 0;

		Mat image = new Mat();
		try {
			long due = System.currentTimeMillis() + millis;
			while ( System.currentTimeMillis() <= due ) {
				cam.capture(image);
				++count;
			}
		}
		finally {
			image.release();
		}

		return count;
		
	}
	
	public static Rect overlap(Rect rect1, Rect rect2) {
		double tl_x = Math.min(rect1.x, rect2.x);
		double tl_y = Math.min(rect1.y, rect2.y);
		double br_x = Math.max(rect1.x+rect1.width-1, rect2.x+rect2.width-1);
		double br_y = Math.max(rect1.y+rect1.height-1, rect2.y+rect2.height-1);
		
		return new Rect(new Point(tl_x, tl_y), new Point(br_x, br_y));
	}
	
	public static Point calcCrossPoint(Point pt1, Point pt2, Point pt3, Point pt4) {
		// 이 사각형의 두 대각선 교점이 사각형 중앙에 오는지 검사한다.
		// 여기서는 두 라인 l1, l2의 교점에서 t와 u 값을 계산하는 식이다.
		// l1 = (a1,b1) + t*(x1,y1)
		// l2 = (a2,b2) + u*(x2,y2)

		double a1 = pt1.x;
		double b1 = pt1.y;
		double x1 = pt2.x - pt1.x;
		double y1 = pt2.y - pt1.y;

		double a2 = pt3.x;
		double b2 = pt3.y;
		double x2 = pt4.x - pt3.x;
		double y2 = pt4.y - pt3.y;
		
		// 관련 식의 유도는 문서 참조: 직선의 방정식과 교점.docx
		Mat A = new Mat(2, 2, CvType.CV_32F);
		A.put(0, 0, 2*(x1*x1 + y1*y1));
		A.put(0, 1, -2*(x1*x2 + y1*y2));
		A.put(1, 0, -2*(x1*x2 + y1*y2));
		A.put(1, 1, 2*(x2*x2 + y2*y2));

		Mat B = new Mat(2, 1, CvType.CV_32F);
		B.put(0,0, 2*(x1*(a2 - a1) + y1*(b2 - b1)));
		B.put(1,0, -2*(x2*(a2 - a1) + y2*(b2 - b1)));
		
		// t, u 계산
		Mat Ainv = A.inv();
		Mat x = new Mat();
		Mat beta = new Mat();
		Core.gemm(Ainv, B, 1, beta, 0, x, 0);

		float t0 = (float)x.get(0,0)[0];
		float s0 = (float)x.get(1,0)[0];
		
		A.release();
		B.release();
		Ainv.release();
		beta.release();
		x.release();

		Point crossPt = new Point();
		crossPt.x = a1 + t0 * x1;
		crossPt.y = b1 + t0 * y1;

		if ( t0 < 0.0 || t0 > 1.0 || s0 < 0.0 || s0 > 1.0 ) {
			return null;
		}
		if ( t0 == 0 && s0 == 0 ) {
			return null;
		}

		return crossPt;
	}
	
	public static void warpRectangleImage(Mat srcImage, Point[] corners, Mat dstImage,
										Size destSize) {
		if ( destSize.width < 1 || destSize.height < 1 ) {
			throw new OpenCvJException("invalid destSize (too small): size=" + destSize);
		}
		if ( corners.length != 4 ) {
			throw new OpenCvJException("invalid corners: corners must have 4 points");
		}
		

		// 추출한 마커를 저장할 이미지 상의 좌표
		Point[] destCorners = toCvPoints(destSize);
		
		// 소스 이미지에서 마커의 코너에 대한 점들을 마커 이미지 상의 점들로 매핑하기 위한 변환 행렬을 구한다.
		PerspectiveTransform trans = PerspectiveTransform.createPerspectiveTransform(corners,
																				destCorners);
		trans.perform(srcImage, dstImage, destSize);
	}
	
	public static double ccw(Point pt1, Point pt2, Point pt3) {
		return (pt1.x*pt2.y) + (pt2.x*pt3.y) + (pt3.x*pt1.y)
				- (pt1.y*pt2.x) + (pt2.y*pt3.x) + (pt3.y*pt1.x);
	}
	
	public static void toClockWise(Point[] corners) {
		Point delta1 = minus(corners[1], corners[0]);
		Point delta2 = minus(corners[3], corners[0]);
		if ( delta2.x > delta1.x ) {
			Point pt3 = corners[3];
			corners[3] = corners[1];
			corners[1] = pt3;
		}
	}

	public static Point[] orderCorners(Point[] corners) {
		// (0,0) 위치를 기준으로 가장 가까운 점을 corner contour의 시작점으로 한다.
		//
		int minDistIdx = -1;
		double minDist = 1e10;
		Point ORIGIN = new Point(0,0);
		for ( int i=0; i < corners.length; ++i ) {
			double dist = distanceL2(corners[i], ORIGIN);
			if ( dist < minDist ) {
				minDistIdx = i;
				minDist = dist;
			}
		}
		
		List<Point> cornerList = new ArrayList<Point>();
		cornerList.addAll(Arrays.asList(corners));
		if ( minDistIdx != 0 ) {
			Collections.rotate(cornerList, cornerList.size()-minDistIdx);
		}
		
		// contour가 반시계방향으로 vertex가 이루어졌다면 거꾸로해서 시계방향이 되게한다.
		Point delta1 = minus(cornerList.get(1), cornerList.get(0));
		Point delta2 = minus(cornerList.get(3), cornerList.get(0));
		if ( delta2.x > delta1.x ) {
			Point pt3 = cornerList.remove(3);
			Point pt1 = cornerList.remove(1);
			cornerList.add(1, pt3);
			cornerList.add(pt1);
		}
	
		// 세로(1->2, 3->0) 길이가 가로(0->1, 2->3) 길이보다 긴 경우는 90도 회전시킨다
		double widthx2 = distanceL2(cornerList.get(0), cornerList.get(1))
							+ distanceL2(cornerList.get(2), cornerList.get(3));
		double hightx2 = distanceL2(cornerList.get(1), cornerList.get(2))
							+ distanceL2(cornerList.get(3), cornerList.get(0));
		if ( widthx2 < hightx2 ) {
			Collections.rotate(cornerList, cornerList.size()-1);
		}
		return cornerList.toArray(new Point[corners.length]);
	}
	
	public static double norm(Point pt) {
		return Math.sqrt(pt.x*pt.x + pt.y*pt.y);
	}
	
	public static double calcCosineBtwLines(Point ptJoint, Point pt1, Point pt2) {
		Point v1 = minus(ptJoint, pt1);
		Point v2 = minus(ptJoint, pt2);

		float i1 = (float)norm(v1);
		float i2 = (float)norm(v2);

		Point npt1 = new Point(v1.x/i1, v1.y/i1);
		Point npt2 = new Point(v2.x/i2, v2.y/i2);

		return npt1.dot(npt2);
	}
	
	public static double calcRadianBtwLines(Point ptJoint, Point pt1, Point pt2) {
		return Math.acos(calcCosineBtwLines(ptJoint, pt1, pt2));
	}
	
	public static double calcDegreeBtwLines(Point ptJoint, Point pt1, Point pt2) {
		return Math.toDegrees(calcRadianBtwLines(ptJoint, pt1, pt2));
	}
}
