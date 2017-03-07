package opencvj.misc;

import java.util.Arrays;

import camus.service.FloatRange;

import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import config.Config;
import opencvj.MatConvas;
import opencvj.Mats;
import opencvj.OpenCvJ;
import opencvj.OpenCvJException;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class Histogram1D implements AutoCloseable {
	private static final int DEF_CHANNEL = 0;
	
	protected final Mat m_hist;
	private final MatOfInt m_channels;
	private final int m_nbins;
	private final MatOfInt m_nbinsMat;
	private final MatOfFloat m_ranges;
	private float[] m_binValues;
	
	public static Histogram1D create(Config config) {
		int nbins = config.getAsInt("nbins", -1);
		int channel = config.getAsInt("channel", DEF_CHANNEL);
		FloatRange range = config.get("range").asFloatRange();
		
		return new Histogram1D(nbins, channel, range);
	}
	
	/**
	 * 1차원 히스토그램 객체를 생성한다.
	 *
	 * @param[in] nbins	생성할 히스토그램의 bin의 갯수.
	 * @param[in] begin_value	히스토그램 대상이 되는 최소 값.
	 * @param[in] end_value		히스토그램의 대상이 되는 최대 값.
	 */
	public Histogram1D(int nbins, int channel, FloatRange range) {
		m_hist = new Mat();
		m_nbins = nbins;
		m_nbinsMat = new MatOfInt(nbins);
		m_channels = new MatOfInt(channel);
		m_ranges = new MatOfFloat(range.low, range.high);
		m_binValues = new float[nbins];
		Arrays.fill(m_binValues, 0);
	}
	
	/**
	 * 1차원 히스토그램 객체를 생성한다.
	 *
	 * @param[in] nbins	생성할 히스토그램의 bin의 갯수.
	 * @param[in] begin_value	히스토그램 대상이 되는 최소 값.
	 * @param[in] end_value		히스토그램의 대상이 되는 최대 값.
	 */
	public Histogram1D(int nbins, FloatRange range) {
		this(nbins, DEF_CHANNEL, range);
	}

	@Override
	public void close() {
		m_hist.release();
	}

	/**
	 * 주어진Mat 객체에서 히스토그램을 생성한다.
	 *
	 * @param[in] image	히스토그램을 구할Mat 객체.
	 */
	public void load(Mat image, Mat mask) {
		if ( image.empty() ) {
			throw new OpenCvJException("target image is empty");
		}
		
		if ( mask == null ) {
			mask = Mats.EMPTY;
		}
		
		Imgproc.calcHist(Arrays.asList(image), m_channels, mask, m_hist, m_nbinsMat, m_ranges);
		m_hist.get(0, 0, m_binValues);
	}
	
	public void clear() {
		m_hist.setTo(OpenCvJ.ALL_0);
	}
	
	public int nbins() {
		return m_nbins;
	}
	
	public MatOfFloat range() {
		return m_ranges;
	}
	
	public Mat getHistogram() {
		return m_hist;
	}

	/**
	 * 지정된 bin의 히스토그램 값을 반환한다.
	 *
	 * @param[in] idx	값을 얻고자 하는 bin 번호.
	 * @return	해당 bin의 히스토그램 값.
	 */
	public float getBinValue(int idx) {
		return m_binValues[idx];
	}
	
	public void backproject(Mat image, Mat projection) {
		Imgproc.calcBackProject(Arrays.asList(image), m_channels, m_hist, projection, m_ranges, 1);
	}
	
	public void draw(MatConvas convas) {
		draw(convas, OpenCvJ.BLACK, OpenCvJ.BLACK, OpenCvJ.GAINSBORO);
	}
	
	public void draw(MatConvas convas, Scalar fillColor, Scalar borderColor, Scalar bgColor) {
		Mat normalized = new Mat();
		
		try {
			convas.getMat().setTo(bgColor);
			
			Core.normalize(m_hist, normalized, 0, 255, Core.NORM_MINMAX);
			
			int nbins = normalized.rows();
			float[] values = new float[nbins];
			normalized.get(0, 0, values);
			
			Size image_size = convas.getMat().size();
			int hpt = (int)Math.round(0.9 * image_size.height);
			int step = (int)Math.round(image_size.width / nbins);
	
			MinMaxLocResult minMax = Core.minMaxLoc(normalized);
			for ( int h =0; h < nbins; ++h ) {
				float v = values[h];
				int iv = (int)Math.round((v * hpt)/minMax.maxVal);
	
				Point pt1 = new Point(h*step, image_size.height-iv);
				Point pt2 = new Point(h*step + step, image_size.height);
				convas.drawRect(new Rect(pt1, pt2), fillColor, Core.FILLED);
				convas.drawRect(new Rect(pt1, pt2), borderColor, 1);
			}
//			convas.drawString(String.format("min=%.1f max=%.1f", minMax.minVal, minMax.maxVal),
//								new Point(10, 13), 1.0, OpenCvJ.RED);
		}
		finally {
			normalized.release();
		}
	}
}
