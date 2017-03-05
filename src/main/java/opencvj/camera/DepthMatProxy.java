package opencvj.camera;

import camus.service.camera.DepthMapProxy;
import camus.service.camera.ImageProxy;
import camus.service.geo.Size2d;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import opencvj.MatProxy;
import opencvj.Mats;
import opencvj.OpenCvJ;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class DepthMatProxy extends MatProxy implements DepthMapProxy {
	public DepthMatProxy(Mat mat) {
		super(mat);
	}

	@Override
	public MatProxy toColoredGray() {
		Mat colored = new Mat();
		Mats.scaleDepthToColoredGray(getMat(), OpenCvJ.RED, colored);
		return new MatProxy(colored);
	}

	public static DepthMatProxy toDepthMatProxy(ImageProxy proxy) {
		if ( proxy instanceof DepthMatProxy ) {
			return (DepthMatProxy)proxy;
		}
		else {
			Size2d size = proxy.getImageFormat().getSize();
			short[] data = proxy.getDataShorts();
			
			Mat mat = new Mat(size.height, size.width, CvType.CV_16SC1);
			mat.put(0, 0, data);
			return new DepthMatProxy(mat);
		}
	}
	
	private static final Scalar ONE = new Scalar(1);
	private static final Scalar DEPTH_MAX = new Scalar(Short.MAX_VALUE);
	public static Mat getNonZeroMask(Mat mat, Mat mask) {
		if ( mask == null ) {
			mask = new Mat();
		}
		Core.inRange(mat, ONE, DEPTH_MAX, mask);
		return mask;
	}
}
