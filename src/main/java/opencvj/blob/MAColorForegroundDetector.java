package opencvj.blob;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import config.Config;
import opencvj.OpenCvJSystem;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class MAColorForegroundDetector extends AbstractForegroundDetector
										implements ForegroundDetector {
	private static final int DEFAULT_INTENSITY_THRESHOLD = 50;
	
	public static class Params {
		public int intensityThreshold =DEFAULT_INTENSITY_THRESHOLD;
		
		public static Params create(Config config) {
			Params params = new Params();
			params.intensityThreshold = config.getMember("intensity_threshold")
												.asInt(DEFAULT_INTENSITY_THRESHOLD);
			
			return params;
		}
	};
	
	private final Params m_params;
	private MAColorBackgroundModel m_bgModel;
	
	public static MAColorForegroundDetector create(Config config) {
		Config bgModelConfig = config.getMember("bgmodel");
		bgModelConfig = (bgModelConfig.isMissing()) ? config : bgModelConfig.asReference();
		MAColorBackgroundModel bgModel = (MAColorBackgroundModel)OpenCvJSystem
																.getBackgroundModel(bgModelConfig);
		BlobExtractor ext = BlobExtractor.create(config);
		return new MAColorForegroundDetector(bgModel, ext, Params.create(config));
	}
	
	public MAColorForegroundDetector(MAColorBackgroundModel bgModel, BlobExtractor filter, Params params) {
		super(filter);
		
		m_params = params;
		m_bgModel = bgModel;
	}

	@Override
	public MAColorBackgroundModel getBackgroundModel() {
		return m_bgModel;
	}

	@Override
	protected void calcForegroundMask(Mat image, Rect roi, Mat fgMask) {
		m_bgModel.subtract(image, roi, fgMask);
		Imgproc.threshold(fgMask, fgMask, m_params.intensityThreshold, 255, Imgproc.THRESH_BINARY);
	}
}
