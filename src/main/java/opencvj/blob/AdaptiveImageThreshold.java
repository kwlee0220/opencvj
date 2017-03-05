package opencvj.blob;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import opencvj.Config;
import opencvj.OpenCvJException;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class AdaptiveImageThreshold implements ImageThreshold {
//	private static final int DEFAULT_BLOCK_SIZE = 71;
	private static final int DEFAULT_BLOCK_SIZE = 91;
//	private static final int DEFAULT_BLOCK_SIZE = 131;
	private static final int DEFAULT_SUBTRACTION_CONSTANT = 15;
	private static final int DEF_THRESHOLD_TYPE = Imgproc.THRESH_BINARY;
	
	// properties (BEGIN)
	private volatile int m_blockSize = DEFAULT_BLOCK_SIZE;
	private volatile int m_C = DEFAULT_SUBTRACTION_CONSTANT;
	private volatile int m_thresholdType = Imgproc.THRESH_BINARY;
	// properties (END)
	
	public static AdaptiveImageThreshold create(Config config) {
		AdaptiveImageThreshold threshold = new AdaptiveImageThreshold();
		threshold.setBlockSize(config.get("block_size").asInt(DEFAULT_BLOCK_SIZE));
		threshold.setSubtractionConstant(config.get("subtraction_constant").asInt(DEFAULT_SUBTRACTION_CONSTANT));
		threshold.setThresholdType(config.get("threshold_type").asInt(DEF_THRESHOLD_TYPE));
		
		return threshold;
	}
	
	public AdaptiveImageThreshold() { }
	
	public final void setBlockSize(int blockSize) {
		m_blockSize = blockSize;
	}
	
	public final void setSubtractionConstant(int c) {
		m_C = c;
	}
	
	public final void setThresholdType(int type) {
		m_thresholdType = type;
	}

	@Override
	public void detect(Mat image, Mat blobMask) throws OpenCvJException {
		if ( image.type() == CvType.CV_8UC3 ) {
			Mat gray = new Mat();
			try {
				Imgproc.cvtColor(image, gray, Imgproc.COLOR_RGB2GRAY);
				Imgproc.adaptiveThreshold(gray, blobMask, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C,
											m_thresholdType, m_blockSize, m_C);
			}
			finally {
				gray.release();
			}
		}
		else if ( image.type() == CvType.CV_8UC1 ) {
			Imgproc.adaptiveThreshold(image, blobMask, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C,
										m_thresholdType, m_blockSize, m_C);
		}
		else {
			throw new OpenCvJException("unsupported image type: " + image.type());
		}
	}
	
	@Override
	public String toString() {
		return String.format("%s[block_size=%d,C=%d]", getClass().getSimpleName(), m_blockSize, m_C);
	}
}
