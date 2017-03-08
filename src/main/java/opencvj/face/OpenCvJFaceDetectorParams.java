package opencvj.face;

import org.opencv.core.Size;

import opencvj.OpenCvJUtils;
import utils.config.ConfigNode;
import utils.config.json.JsonConfiguration;

/**
*
* @author Kang-Woo Lee
*/
public final class OpenCvJFaceDetectorParams {
	public static final String NAME_DETECT_SCALE_FACTOR = "detect_scale_factor";
	public static final String NAME_MIN_FACE_SIZE = "min_face_size";
	public static final String NAME_MAX_FACE_SIZE = "max_face_size";
	
	public static final float DEF_DETECT_SCALE_FACTOR = 1.17f;
	
	public float detectScaleFactor;
	public Size minFaceSize;
	public Size maxFaceSize;
	
	public OpenCvJFaceDetectorParams() {
		detectScaleFactor = DEF_DETECT_SCALE_FACTOR;
		minFaceSize = new Size();
		maxFaceSize = new Size();
	}
	
	public OpenCvJFaceDetectorParams(float detectScaleFactor) {
		this.detectScaleFactor = detectScaleFactor;
	}
	
	public OpenCvJFaceDetectorParams(OpenCvJFaceDetectorParams params) {
		detectScaleFactor = params.detectScaleFactor;
	}
	
	public OpenCvJFaceDetectorParams duplicate() {
		return new OpenCvJFaceDetectorParams(this);
	}
	
	public static OpenCvJFaceDetectorParams parseParamsString(String paramsStr) {
		ConfigNode config = JsonConfiguration.load(paramsStr).getRoot();
		
		OpenCvJFaceDetectorParams params = new OpenCvJFaceDetectorParams();
		params.detectScaleFactor = config.get(NAME_DETECT_SCALE_FACTOR).asFloat();
		params.minFaceSize = OpenCvJUtils.asSize(config.get(NAME_MIN_FACE_SIZE));
		params.maxFaceSize = OpenCvJUtils.asSize(config.get(NAME_MAX_FACE_SIZE));
		
		return params;
	}
	
	public String toString() {
		return String.format("FACE_PARAM[%s=.1f]", NAME_DETECT_SCALE_FACTOR,
													this.detectScaleFactor);
	}
}