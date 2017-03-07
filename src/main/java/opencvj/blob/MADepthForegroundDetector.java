package opencvj.blob;

import java.util.List;

import camus.service.IntRange;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;

import config.Config;
import opencvj.Mats;
import opencvj.OpenCvJ;
import opencvj.OpenCvJSystem;
import opencvj.OpenCvJUtils;
import utils.UninitializedException;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class MADepthForegroundDetector extends AbstractDeltaAwareForegroundDetector
										implements DeltaAwareForegroundDetector {
	public static final int BG_MODEL_VALID_VALID = 0x01;
	public static final int BG_MODEL_INVALID_VALID = 0x02;
	public static final int BG_MODEL_ANY_INVALID = 0x04;
	private static final IntRange DEFAULT_VALID_DEPTH = new IntRange(1, Short.MAX_VALUE);
	
	public static class Params {
		IntRange validDepthRange = new IntRange(1, Short.MAX_VALUE);
		IntRange backgroundDepthDelta;
		int fgModelFlags = BG_MODEL_VALID_VALID + BG_MODEL_INVALID_VALID;
		
		public static Params create(Config config) {
			Params params = new Params();

			params.validDepthRange = OpenCvJUtils.asIntRange(config.getMember("valid_depth"),
															DEFAULT_VALID_DEPTH);
			params.backgroundDepthDelta = OpenCvJUtils.asIntRange(
														config.getMember("bg_depth_delta"), null);
			params.fgModelFlags = config.getMember("fg_model_flags")
										.asInt(BG_MODEL_VALID_VALID + BG_MODEL_INVALID_VALID);
			
			return params;
		}
	};
	
	private Params m_params;
	private MADepthBackgroundModel m_bgModel;
	private final Mat m_tmpDelta32f = new Mat();
	
	public static MADepthForegroundDetector create(Config config) {
		MADepthBackgroundModel bgModel = (MADepthBackgroundModel)OpenCvJSystem
																	.getBackgroundModel(config);
		BlobExtractor ext = BlobExtractor.create(config);
		return new MADepthForegroundDetector(bgModel, ext, Params.create(config));
	}
	
	public MADepthForegroundDetector(MADepthBackgroundModel bgModel, BlobExtractor filter,
									Params params) {
		super(filter);
		
		m_params = params;
		m_bgModel = bgModel;
	}
	
	@Override
	public void close() {
		m_tmpDelta32f.release();
	}
	
	public Params getParams() {
		return m_params;
	}

	@Override
	public BackgroundModel getBackgroundModel() {
		return m_bgModel;
	}

	@Override
	protected void calcForegroundMask(Mat image, Rect roi, Mat fgMask) {
		subtract(image, roi, m_tmpDelta32f, fgMask);
	}

	@Override
	protected void calcForegroundMask(Mat image, Rect roi, Mat delta32f, Mat fgMask) {
		subtract(image, roi, delta32f, fgMask);
	}
	
	private void subtract(Mat image, Rect roi, Mat delta32f, Mat fgMask) {
		Mat bgMat = m_bgModel.getBackgroundModel();
		Mat blindBgMask = m_bgModel.getBlindBackgroundMask();
		
		if ( roi != null ) {
			Mats.createIfNotValid(delta32f, image.size(), CvType.CV_32FC1, OpenCvJ.ALL_0);
			Mats.createIfNotValid(fgMask, image.size(), CvType.CV_8UC1, OpenCvJ.ALL_0);
			
			Mat imageRoi = new Mat(image, roi);
			Mat bgModelRoi = new Mat(bgMat, roi);
			Mat fgMaskRoi = new Mat(fgMask, roi);
			Mat delta32fRoi = new Mat(delta32f, roi);
			Mat blindBgMaskRoi = new Mat(blindBgMask, roi);
			
			try {
				subtractRoi(imageRoi, bgModelRoi, blindBgMaskRoi, delta32fRoi, fgMaskRoi);
			}
			finally {
				Mats.releaseAll(imageRoi, bgModelRoi, blindBgMaskRoi, delta32fRoi, fgMaskRoi);
			}
		}
		else {
			subtractRoi(image, bgMat, blindBgMask, delta32f, fgMask);
		}

		if ( m_filter != null ) {
			List<Blob> blobs = m_filter.extractBlobs(fgMask);
			fgMask.setTo(OpenCvJ.ALL_0);
			Blobs.newBlobMask(image.size(), fgMask, blobs);
		}
	}

	/**
	 * @param image		배경을 제거할 대상 이미지 (CV_16SC1 타입)
	 * @param bgModel	제거 대상 배경을 설명하는 모델 (CV_32FC1 타입)
	 * @param blindBgMask	배경 학습과정에서 배경 거리 값이 획득되지 못한 영역 mask
	 * @param delta32f	배경과의 차이(bgModel - image) 값이 저장될 mat.
	 * @param fgMask	배경 제어 후 전경으로 간주된 영역이 저장될 mask.
	 */
	private void subtractRoi(Mat image, Mat bgModel, Mat blindBgMask, Mat delta32f,
										Mat fgMask) {
		if ( m_params.validDepthRange == null ) {
			throw new UninitializedException("Property 'validDepthRange' was not specified: class="
											+ getClass().getName());
		}
		
		Mat image32f = new Mat();
		Mat bgMask = new Mat(image.size(), CvType.CV_8UC1);
		Mat validDepthMask = new Mat(image.size(), CvType.CV_8UC1);
		Mat invalid2ValidMask = new Mat(image.size(), CvType.CV_8UC1);
		
		try {
			// 주어진 거리 맵을 내부적으로 유지하는 background_model의 거리 값과
			// 입력된 거리 값의 차이를 구한다. 즉, bgmodel - image를 구한다.
			image.convertTo(image32f, CvType.CV_32F);
			Core.subtract(bgModel, image32f, delta32f);
			
			// 배경과의 거리 차이가 지정된 범위내에 있는 영역을 검출해서 영역 밖에 있는
			// 영역을 전경으로 간주한다.
			Mats.calcRangeMask(delta32f, m_params.backgroundDepthDelta, bgMask);
			Core.bitwise_not(bgMask, fgMask);
			
			// 입력 영상에서 invalid한 거리값을 갖는 픽셀을 전경으로 간주하지 않는 경우는
			// 해당 영역을 전경 영역에서 제외시킨다.
			Mats.calcRangeMask(image, m_params.validDepthRange, validDepthMask);
			if ( (m_params.fgModelFlags & BG_MODEL_ANY_INVALID) == 0 ) {
				Core.bitwise_and(fgMask, validDepthMask, fgMask);
			}
			
			// 배경 학습시 invalid 거리 값을 가졌던 영역을 전경에서 제거시킨다.
			fgMask.setTo(OpenCvJ.ALL_0, blindBgMask);
	
			// background에서는 invalid된 pixel이지만, 입력 거리 맵에서는 valid한 경우를
			// 전경으로 간주하는 경우, 전경 mask에 추가시킨다.
			// 이때의 거리 차이 값은 정확히 알 수 없기 때문에,
			// 'm_params.backgroundDepthDelta.high + 1'로 설정한다.
			if ( (m_params.fgModelFlags & BG_MODEL_INVALID_VALID) != 0 ) {
				Core.bitwise_and(blindBgMask, validDepthMask, invalid2ValidMask);
				Core.bitwise_or(fgMask, invalid2ValidMask, fgMask);
				
				delta32f.setTo(Scalar.all(255), blindBgMask);
			}
		}
		finally {
			Mats.releaseAll(image32f, bgMask, validDepthMask, invalid2ValidMask);
		}
	}
}
