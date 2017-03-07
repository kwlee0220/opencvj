package opencvj.track;

import java.util.Arrays;

import camus.service.FloatRange;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import opencvj.Mats;
import opencvj.OpenCvJ;
import opencvj.OpenCvJUtils;
import opencvj.misc.Histogram1D;
import utils.config.ConfigNode;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class HueBackprojector implements Backprojector {
	private static final FloatRange RANGE = new FloatRange(0, 180);
	private static final Scalar DEF_VALID_LOWER_HSV = new Scalar(0,30,10);
	private static final Scalar DEF_VALID_UPPER_HSV = new Scalar(180,256,256);
	
	private final Histogram1D m_hist;
	
	private Scalar m_lowerHSV = DEF_VALID_LOWER_HSV;
	private Scalar m_upperHSV = DEF_VALID_UPPER_HSV;
	
	public static HueBackprojector create(ConfigNode config) {
		int nbins = config.get("nbins").asInt(-1);
		Histogram1D hist = new Histogram1D(nbins, RANGE);
		
		Scalar lowerb = OpenCvJUtils.asScalar(config.get("lower_hsv"), DEF_VALID_LOWER_HSV);
		Scalar upperb = OpenCvJUtils.asScalar(config.get("upper_hsv"), DEF_VALID_UPPER_HSV);
		
		return new HueBackprojector(hist, lowerb, upperb);
	}
	
	public HueBackprojector(Histogram1D hist) {
		this(hist, DEF_VALID_LOWER_HSV, DEF_VALID_UPPER_HSV);
	}
	
	public HueBackprojector(Histogram1D hist, Scalar lowerHSV, Scalar upperHSV) {
		m_hist = hist;
		m_lowerHSV = lowerHSV;
		m_upperHSV = upperHSV;
	}

	@Override
	public void close() {
		m_hist.close();
	}

	@Override
	public void load(Mat image, Mat mask) {
		Mat hue = new Mat();
		Mat validMask = new Mat();
		Mat convas = new Mat(new Size(320,240), CvType.CV_8UC3, OpenCvJ.GAINSBORO);
		try {
			calcValidHueMask(image, hue, validMask);
			if ( Mats.isValid(mask, image.size()) ) {
				Core.bitwise_and(mask, validMask, validMask);
			}
			
			m_hist.load(hue, validMask);
//			m_hist.draw(new MatConvas(convas));
//			WindowManager.show("hist", convas);
		}
		finally {
			Mats.releaseAll(hue, validMask, convas);
		}
	}

	@Override
	public void clear() { 
		m_hist.clear();
	}

	@Override
	public void backproject(Mat image, Mat proj) {
		Mat hue = new Mat();
		Mat validMask = new Mat();
		try {
			calcValidHueMask(image, hue, validMask);
			
			m_hist.backproject(hue, proj);
			Core.bitwise_and(proj, validMask, proj);
		}
		finally {
			Mats.releaseAll(hue, validMask);
		}
	}

	@Override
	public void backproject(Mat image, Rect roi, Mat proj) {
		try {
			Mat imageRoi = new Mat(image, roi);
			try {
				backproject(imageRoi, proj);
			}
			finally {
				Mats.releaseAll(imageRoi);
			}
		}
		catch ( Throwable e ) {
			e.printStackTrace();
		}
	}

	private static final MatOfInt FROM_TO = new MatOfInt(0, 0);
	private void calcValidHueMask(Mat bgr, Mat hue, Mat mask) {
		Mat hsv = new Mat();
		try {
			Imgproc.cvtColor(bgr, hsv, Imgproc.COLOR_BGR2HSV);
			Core.inRange(hsv, m_lowerHSV, m_upperHSV, mask);
			
			Mats.createIfNotValid(hue, hsv.size(), hsv.depth());
			Core.mixChannels(Arrays.asList(hsv), Arrays.asList(hue), FROM_TO);
		}
		finally {
			hsv.release();
		}
	}
}
