package opencvj.blob;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import camus.service.SizeRange;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import opencvj.Config;
import opencvj.Mats;
import opencvj.OpenCvJException;



/**
 * 
 * @author Kang-Woo Lee
 */
public class BlobExtractor {
//	private static final Mat DUMMY_HIERARCHY = new Mat();
	private static final Mat KERNEL;
	static {
		KERNEL = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5,5));
	}
	
	private float m_approx_poly_epsilon = 0;
	private MorphAction m_morph_action = MorphAction.MORPH_ACT_NONE;
	private ContourMode m_contour_mode = ContourMode.CV_RETR_LIST;
	private BlobFilter m_filter = null;
	
	public static BlobExtractor create(Config config) {
		BlobExtractor ext = new BlobExtractor();

		ext.m_approx_poly_epsilon = config.get("approx_poly_epsilon").asFloat(0);
		ext.m_morph_action = MorphAction.from(config.get("morph_action"), MorphAction.MORPH_ACT_NONE);
		ext.m_contour_mode = parseContourMode(config.get("contour_mode").asString("list"));
		
		boolean ignoreHoles = config.get("ignore_hole").asBoolean(true);
		Config sizeConfig = config.get("size");
		if ( sizeConfig.isContainer() ) {
			ext.setFilter(Blobs.newSizeRangeBlobFilter(sizeConfig, ignoreHoles));
		}
		
		return ext;
	}
	
	public BlobExtractor() {
	}
	
	public BlobExtractor(float approx, MorphAction morph, SizeRange size) {
		m_approx_poly_epsilon = approx;
		m_morph_action = morph;
		
		if ( size != null ) {
			setFilter(Blobs.newSizeRangeBlobFilter(size));
		}
	}
	
	public void setFilter(BlobFilter filter) {
		m_filter = filter;
	}
	
	public boolean isRestrictable() {
		return m_filter == null && m_approx_poly_epsilon == 0
				&& m_morph_action == MorphAction.MORPH_ACT_NONE; 
	}
	
	public List<Blob> extractBlobs(Mat image) throws OpenCvJException {
		Mat copied = new Mat();
		Mat hier = new Mat();
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		try {
			switch ( m_morph_action ) {
				case MORPH_ACT_CLOSE:
					Imgproc.dilate(image, copied, KERNEL);
					Imgproc.erode(copied, copied, KERNEL);
					break;
				case MORPH_ACT_OPEN:
					Imgproc.erode(image, copied, KERNEL);
					Imgproc.dilate(copied, copied, KERNEL);
					break;
				case MORPH_ACT_NONE:
					image.copyTo(copied);
					break;
			}

			Imgproc.findContours(copied, contours, hier, m_contour_mode.getCode(),
									Imgproc.CHAIN_APPROX_SIMPLE);
			List<Blob> blobs = new ArrayList<Blob>();
			if ( contours.size() == 0 ) {
				return blobs;
			}
			
			int[] hierarchy = new int[(int)hier.total() * hier.channels()];
			hier.get(0, 0, hierarchy);

			for ( int i =0; i < contours.size(); ++i ) {
				Point[] pts = contours.get(i).toArray();
				
				if ( pts.length < 3 ) {
					continue;
				}
				
				if ( m_approx_poly_epsilon > 0 ) {
					MatOfPoint2f approx = new MatOfPoint2f();
					try {
						Imgproc.approxPolyDP(new MatOfPoint2f(pts), approx, m_approx_poly_epsilon, true);
						pts = approx.toArray();
						if ( pts.length < 3 ) {
							continue;
						}
					}
					finally {
						approx.release();
					}
				}
				
				Blob blob = null;
				switch ( m_contour_mode ) {
					case CV_RETR_LIST:
					case CV_RETR_EXTERNAL:
						blob = new Blob(pts);
						break;
					case CV_RETR_CCOMP:
					case CV_RETR_TREE:
						List<Point[]> holeList = new ArrayList<Point[]>();
						for ( int idx = hierarchy[i*4 + 2]; idx >= 0 ; idx = hierarchy[idx*4] ) {
							holeList.add(contours.get(idx).toArray());
						}
						blob = new Blob(pts, holeList);
						break;
				}
				
				if ( m_filter == null || m_filter.apply(blob, image) ) {
					blobs.add(blob);
				}
			}
			
			return blobs;
		}
		catch ( Exception e ) {
			System.err.println("UNEXPECTED FAILURE!! -> IGNORED: cause=" + e);
//			e.printStackTrace(System.err);
			return Collections.<Blob>emptyList();
		}
		finally {
			Mats.releaseAll(copied, hier);
			
			for ( MatOfPoint mop: contours ) {
				mop.release();
			}
		}
	}
	
	public List<Blob> extractKLargestBlobs(Mat image, int k) throws OpenCvJException {
		List<Blob> blobs = extractBlobs(image);
		Collections.sort(blobs, Blob.AREA_COMP_DESC);
		if ( blobs.size() > k ) {
			return blobs.subList(0, k-1);
		}
		else {
			return blobs;
		}
	}
	
	public Blob extractLargestBlob(Mat image) throws OpenCvJException {
		List<Blob> blobs = extractBlobs(image);
		return Collections.min(blobs, Blob.AREA_COMP_DESC);
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if ( m_approx_poly_epsilon > 0 ) {
			builder.append(String.format("approx=%.1f,", m_approx_poly_epsilon));
		}
		if ( m_morph_action != MorphAction.MORPH_ACT_NONE ) {
			builder.append("morph=" + m_morph_action + ",");
		}
		if ( m_filter != null ) {
			builder.append(m_filter.toString() + ",");
		}
		
		if ( builder.length() > 0 ) {
			builder.setLength(builder.length()-1);
		}
		return builder.toString();
	}
	
	private static ContourMode parseContourMode(String str) {
		if ( "list".equalsIgnoreCase(str) ) {
			return ContourMode.CV_RETR_LIST;
		}
		else if ( "ccomp".equalsIgnoreCase(str) ) {
			return ContourMode.CV_RETR_CCOMP;
		}
		
		throw new UnsupportedOperationException("contour.mode=" + str);
	}
}