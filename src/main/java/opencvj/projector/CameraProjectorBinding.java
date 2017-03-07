package opencvj.projector;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.Point;
import org.opencv.core.Size;

import com.google.gson.Gson;

import opencvj.Mats;
import opencvj.OpenCvJUtils;
import opencvj.misc.PerspectiveTransform;
import utils.config.ConfigNode;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public final class CameraProjectorBinding implements AutoCloseable {
	/** 카메라 캡쳐 이미지의 크기 (해상도). */
	public Size m_cameraSize;
	/** 프로젝터를 통해 투사되는 스크린 이미지의 크기 (해상도). */
	public Size m_projectorSize;
	public Point[] m_projectionCorners;
	public PerspectiveTransform m_transScreenToCamera;
	public PerspectiveTransform m_transCameraToScreen;
	
	public static CameraProjectorBinding create(ConfigNode config) {
		CameraProjectorBinding param = new CameraProjectorBinding();
		
		param.m_cameraSize = OpenCvJUtils.asSize(config.get("image_size"));
		param.m_projectorSize = OpenCvJUtils.asSize(config.get("screen_size"));
		param.m_projectionCorners = OpenCvJUtils.asPoints(config.get("projection_corners"));
		param.m_transScreenToCamera = new PerspectiveTransform(
										OpenCvJUtils.asMat(config.get("screen_to_camera_matrix")));
		param.m_transCameraToScreen = param.m_transScreenToCamera.inv();
		
		return param;
	}
	
	public CameraProjectorBinding() {
		m_transScreenToCamera = new PerspectiveTransform();
		m_transCameraToScreen = new PerspectiveTransform();
	}

	@Override
	public void close() {
		m_transScreenToCamera.close();
		m_transCameraToScreen.close();
	}

	public Point toCameraCoordinates(Point projectorCoords) {
		return m_transScreenToCamera.perform(projectorCoords);
	}

	public Point[] toCameraCoordinates(Point[] projectorCoords) {
		return m_transScreenToCamera.perform(projectorCoords);
	}

	public Point toProjectorCoordinates(Point cameraCoords) {
		return m_transCameraToScreen.perform(cameraCoords);
	}

	public Point[] toProjectorCoordinates(Point[] cameraCoords) {
		return m_transCameraToScreen.perform(cameraCoords);
	}
	
	public void extractProjectionImage(Mat cameraImage, Mat extracted) {
		Mats.createIfNotValid(extracted, m_projectorSize, cameraImage.type());
		m_transCameraToScreen.perform(cameraImage, extracted, m_projectorSize);
	}
	
	public void write(File file) throws IOException {
		Map<String,Object> data = new HashMap<String,Object>();
		
		data.put("image_size", new double[]{m_cameraSize.width, m_cameraSize.height});
		data.put("screen_size", new double[]{m_projectorSize.width, m_projectorSize.height});
		data.put("projection_corners", toDoubleArray(m_projectionCorners));
		data.put("screen_to_camera_matrix", toMap(m_transScreenToCamera.getTransformMatrix()));
		
		try ( FileWriter writer = new FileWriter(file) ) {
			new Gson().toJson(data, writer);
		}
	}
	
	private static double[] toDoubleArray(Point[] pts) {
		double[] vals = new double[pts.length*2];
		for ( int i =0; i < pts.length; ++i ) {
			vals[i*2] = pts[i].x;
			vals[i*2+1] = pts[i].y;
		}
		
		return vals;
	}
	
	private static Map<String,Object> toMap(Mat mat) {
		Map<String,Object> map = new HashMap<String,Object>();

		int rows = mat.rows();
		int cols = mat.cols();
		
		map.put("rows", rows);
		map.put("cols", cols);
		map.put("type", mat.type());
		
		switch ( mat.type() ) {
			case CvType.CV_64F:
				double[] doubleData = new double[rows * cols];
				mat.get(0, 0, doubleData);
				map.put("data", doubleData);
				break;
			case CvType.CV_32F:
				MatOfFloat mof = new MatOfFloat(mat);
				map.put("data", mof.toArray());
				mof.release();
				break;
			default:
				throw new java.lang.UnsupportedOperationException("unsupported mat type=" + mat.type());
		}
		
		return map;
	}
}
