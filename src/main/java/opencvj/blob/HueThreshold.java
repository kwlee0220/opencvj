package opencvj.blob;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import opencvj.OpenCvJException;
import opencvj.OpenCvJUtils;
import utils.config.ConfigNode;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class HueThreshold implements ImageThreshold {
	// green: 48 -> 60
	// satuation: 30 -> 256
	// value: 10 -> 256
	
	// properties (BEGIN)
	private Scalar m_lowerHSV;
	private Scalar m_upperHSV;
	// properties (END)
	
	public static HueThreshold create(ConfigNode config) {
		HueThreshold threshold = new HueThreshold();
		threshold.setUpperHSV(OpenCvJUtils.asScalar(config.get("upper_hsv"), null));
		threshold.setLowerHSV(OpenCvJUtils.asScalar(config.get("lower_hsv"), null));
		
		return threshold;
	}
	
	public HueThreshold() { }
	
	public final void setUpperHSV(Scalar value) {
		m_upperHSV = value;
	}
	
	public final void setLowerHSV(Scalar value) {
		m_lowerHSV = value;
	}

	@Override
	public void detect(Mat image, Mat blobMask) throws OpenCvJException {
		Mat hsv = new Mat();
		try {
			Imgproc.cvtColor(image, hsv, Imgproc.COLOR_BGR2HSV);
			Core.inRange(hsv, m_lowerHSV, m_upperHSV, blobMask);
		}
		finally {
			hsv.release();
		}
	}
	
	@Override
	public String toString() {
		return String.format("%s[(%s):(%s)]", getClass().getSimpleName(), OpenCvJUtils.toString(m_lowerHSV),
				OpenCvJUtils.toString(m_upperHSV));
	}
}
