package opencvj.blob;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import opencvj.Config;
import opencvj.OpenCvJException;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class SimpleImageThreshold implements ImageThreshold {
	private static final int DEF_INTENSITY_THRESHOLD = 50;
	private static final int DEF_THRESHOLD_TYPE = Imgproc.THRESH_BINARY;
	
	// properties (BEGIN)
	private volatile int m_intensity =DEF_INTENSITY_THRESHOLD;
	private volatile int m_thresholdType = Imgproc.THRESH_BINARY;
	// properties (END)
	
	public static SimpleImageThreshold create(Config config) {
		SimpleImageThreshold threshold = new SimpleImageThreshold();
		threshold.setIntensity(config.get("intensity_threshold").asInt(DEF_INTENSITY_THRESHOLD));
		threshold.setThresholdType(config.get("threshold_type").asInt(DEF_THRESHOLD_TYPE));
		
		return threshold;
	}
	
	public SimpleImageThreshold() { }
	
	public final void setIntensity(int intensity) {
		m_intensity = intensity;
	}
	
	public final void setThresholdType(int type) {
		m_thresholdType = type;
	}

	@Override
	public void detect(Mat image, Mat blobMask) throws OpenCvJException {
		Mat gray = new Mat();
		try {
			Imgproc.cvtColor(image, gray, Imgproc.COLOR_RGB2GRAY);
			Imgproc.threshold(gray, blobMask, m_intensity, 255, m_thresholdType);
		}
		finally {
			gray.release();
		}
	}
	
	@Override
	public String toString() {
		return String.format("%s[intensity=%d]", getClass().getSimpleName(), m_intensity);
	}
}
