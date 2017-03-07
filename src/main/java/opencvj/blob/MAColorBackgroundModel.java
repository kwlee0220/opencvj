package opencvj.blob;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import config.Config;
import opencvj.Mats;
import opencvj.OpenCvJ;
import opencvj.OpenCvJException;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class MAColorBackgroundModel implements BackgroundModel {
	private Mat m_bgModel = new Mat();		// CV_32F
	private Mat m_bgImage = new Mat();
	private final float m_updateRate;
	
	public static MAColorBackgroundModel create(Config config) {
		return new MAColorBackgroundModel(config.getMember("update_rate").asFloat());
	}
	
	public MAColorBackgroundModel(float updateRate) {
		m_updateRate = updateRate;
	}

	@Override
	public void close() {
		m_bgModel.release();
		m_bgImage.release();
	}

	@Override
	public Mat getBackgroundModel() {
		return m_bgModel;
	}

	@Override
	public Mat getBackground() {
		return m_bgImage;
	}
	
	public void subtract(Mat image, Rect roi, Mat delta) {
		if ( image.empty() ) {
			throw new IllegalArgumentException("source image is empty");
		}
		Mats.createIfNotValid(delta, image.size(), CvType.CV_8UC1);

		// background image가 설정되지 않은 경우는 인자로 온 영상을 배경 영상으로 설정한다.
		if ( !Mats.isValid(m_bgImage) ) {
			if ( roi != null ) {
				Mat deltaRoi = new Mat(delta, roi);
				deltaRoi.setTo(OpenCvJ.ALL_255);
				deltaRoi.release();
			}
			else {
				delta.setTo(OpenCvJ.ALL_255);
			}
			
			return;
		}
		
		if ( roi != null ) {
			Mat imageRoi = new Mat(image, roi);
			Mat bgImageRoi = new Mat(m_bgImage, roi);
			Mat deltaRoi = new Mat(delta, roi);
			try {
				_subtract(imageRoi, bgImageRoi, deltaRoi);
			}
			finally {
				Mats.releaseAll(imageRoi, bgImageRoi, deltaRoi);
			}
		}
		else {
			_subtract(image, m_bgImage, delta);
		}
	}

	@Override
	public void updateBackgroundModel(Mat image) {
		updateBackgroundModel(image, m_updateRate);
	}

	public void updateBackgroundModel(Mat image, float updateRate) throws OpenCvJException {
		if ( image.empty() ) {
			throw new IllegalArgumentException("source image is empty");
		}
		if ( updateRate < 0 || updateRate > 1 ) {
			throw new IllegalArgumentException(String.format("invalid learning_rate: rate=%.3f",
												updateRate));
		}

		// background image가 설정되지 않은 경우는 인자로 온 영상을 배경 영상으로 설정한다.
		if ( !Mats.isValid(m_bgImage) ) {
			image.convertTo(m_bgModel, CvType.CV_32FC(image.channels()), 1.0, 0.0);
			image.copyTo(m_bgImage);
		}
		else {
			Mat tmp = new Mat();
			try {
				image.convertTo(tmp, CvType.CV_32FC(image.channels()));
				Imgproc.accumulateWeighted(tmp, m_bgModel, updateRate);

				m_bgModel.convertTo(m_bgImage, image.type());
			}
			finally {
				tmp.release();
			}
		}
	}

	@Override
	public void clearBackground() {
		m_bgModel.release();
		m_bgModel = new Mat();
		
		m_bgImage.release();
		m_bgImage = new Mat();
	}
	
	private void _subtract(Mat image, Mat bgImage, Mat diff) {
		Mat tmp = new Mat();
		try {
			Core.absdiff(image, bgImage, tmp);
			if ( image.type() == CvType.CV_8UC3 ) {
				Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_RGB2GRAY);
			}
			tmp.copyTo(diff);
		}
		finally {
			tmp.release();
		}
	}
}
