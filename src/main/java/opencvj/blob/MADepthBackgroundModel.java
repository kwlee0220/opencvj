package opencvj.blob;

import camus.service.IntRange;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import opencvj.Mats;
import opencvj.OpenCvJUtils;
import utils.config.ConfigNode;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class MADepthBackgroundModel implements BackgroundModel {
	private Mat m_bgModel =null;			// CV_32F
	private Mat m_bgBlindAreaGauge =null;
	private Mat m_background = null;		// null: un-assigned
	private Mat m_blindBgMask =null;
	private final float m_updateRate;
	
	public static MADepthBackgroundModel create(ConfigNode config) {
		return new MADepthBackgroundModel(config.get("update_rate").asFloat());
	}
	
	public MADepthBackgroundModel(float updateRate) {
		m_updateRate = updateRate;
	}

	@Override
	public void close() {
		if ( m_bgModel != null ) {
			m_bgModel.release();
		}
		if ( m_bgBlindAreaGauge != null ) {
			m_bgBlindAreaGauge.release();
		}
		if ( m_background != null ) {
			m_background.release();
		}
		if ( m_blindBgMask != null ) {
			m_blindBgMask.release();
		}
	}

	@Override
	public Mat getBackgroundModel() {
		return m_bgModel;
	}

	@Override
	public Mat getBackground() {
		if ( m_bgModel == null ) {
			throw new IllegalStateException("Background has not been learned");
		}
		if ( m_background == null ) {
			m_background = new Mat();
			m_bgModel.convertTo(m_background, CvType.CV_16SC1);
			
			final Mat mask = getBlindBackgroundMask();
			m_background.setTo(ZERO, mask);
		}
		
		return m_background;
	}

	@Override
	public void updateBackgroundModel(Mat image) {
		update(image, m_updateRate);
	}
	
	public void updateBackgroundModel(Mat image, float rate) {
		update(image, rate);
	}

	@Override
	public void clearBackground() {
		if ( m_bgModel != null ) {
			m_bgModel.release();
			m_bgModel = null;
			
			m_bgBlindAreaGauge.release();
			m_bgBlindAreaGauge = null;
		}
		if ( m_blindBgMask != null ) {
			m_blindBgMask.release();
			m_blindBgMask = null;
		}
	}
	
	private static final Scalar ZERO = Scalar.all(0);
	private static final Scalar ONE = new Scalar(1);
	private static final Scalar MIDDLE = new Scalar(128);

	private void update(Mat image, float updateRate) {
		if ( image.empty() ) {
			throw new IllegalArgumentException("source image is empty");
		}
		if ( m_bgModel != null && !m_bgModel.size().equals(image.size()) ) {
			throw new IllegalArgumentException(
						String.format("invalid depth frame size: depth.size=%s <-> background.size=%s",
										OpenCvJUtils.toString(image.size()),
										OpenCvJUtils.toString(m_bgModel.size())));
		}
		
		Mat nonBlindMask = new Mat();
		Mat depthImage32f = new Mat();
		Mat tmp = new Mat();
		
		try {
			// 입력 값의 validity에 따라 guage 값에 반영한다.
			//
			updateBlindBackgroundGauge(image, nonBlindMask);
	
			image.convertTo(depthImage32f, CvType.CV_32FC1);
			if ( m_bgModel == null ) {
				// background image가 설정되지 않은 경우는 인자로 온 영상을 배경 영상으로 설정한다.
				m_bgModel = new Mat();
				depthImage32f.copyTo(m_bgModel, nonBlindMask);
			}
			else {
				// 입력 영상 중 valid 입력 값을 가진 부분을 model에 learningRate 만큼 반영시킨다.
				Imgproc.accumulateWeighted(depthImage32f, m_bgModel, updateRate, nonBlindMask);
				
				// 지금까지 bgModel에서는 blind 영역이었지만, 입력 영상에는 non-blind한 영역이 있다면
				// 입력 영상 값으로 초기화 시킨다.
				Mats.calcRangeMask(m_bgModel, new IntRange(0,0), tmp);
				Core.bitwise_and(tmp, nonBlindMask, tmp);
				depthImage32f.copyTo(m_bgModel, tmp);
			}
		}
		finally {
			Mats.releaseAll(nonBlindMask, depthImage32f, tmp);
		}
		
		// background model이 수정되었기 때문에, cache 역할을 하는 'm_background'는 invalid시킨다.
		if ( m_background != null ) {
			m_background.release();
			m_background = null;
		}
	}

	private void updateBlindBackgroundGauge(Mat image, Mat nonBlindMask) {
		Mat blindMask = new Mat();
		try {
			// 거리 값이 획득되지 못한 구역과 그렇지 않은 영역의 매스크 획득
			Core.inRange(image, ZERO, ZERO, blindMask);	// blind 영역
			Core.bitwise_not(blindMask, nonBlindMask);	// non-blind 영역
			
			if ( m_bgBlindAreaGauge == null ) {
				m_bgBlindAreaGauge = new Mat(image.size(), CvType.CV_8UC1, MIDDLE);
			}
			Core.subtract(m_bgBlindAreaGauge, ONE, m_bgBlindAreaGauge, blindMask);
			Core.add(m_bgBlindAreaGauge, ONE, m_bgBlindAreaGauge, nonBlindMask);
			
			// m_bgBlindAreaGauge가 수정되었기 때문에 cache에 해당하는 m_blindBgMask를
			// invalidate 시킨다.
			if ( m_blindBgMask != null ) {
				m_blindBgMask.release();
				m_blindBgMask = null;
			}
		}
		finally {
			blindMask.release();
		}
	}
	
	public Mat getBlindBackgroundMask() {
		if ( m_blindBgMask == null ) {
			if ( m_bgBlindAreaGauge == null ) {
				throw new IllegalStateException("background has not been learned");
			}
			
			m_blindBgMask = new Mat();
			Core.inRange(m_bgBlindAreaGauge, ZERO, new Scalar(127), m_blindBgMask);
		}
		
		return m_blindBgMask;
	}
}
