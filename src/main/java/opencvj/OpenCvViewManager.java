package opencvj;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import camus.service.geo.Size2d;

import etri.service.image.SwingBasedImageView;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

import opencvj.blob.Blob;
import opencvj.camera.DepthMatProxy;
import utils.Initializable;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public final class OpenCvViewManager {
	private static final Map<String,OpenCvView> s_windows
													= new HashMap<String,OpenCvView>();
	
	public static void shutdown() {
		s_windows.values().stream().forEach(Initializable::destroyQuietly);
		s_windows.clear();
	}
	
	public static void destroyView(String id) {
		SwingBasedImageView window = s_windows.get(id);
		if ( window != null ) {
			s_windows.remove(id);
			window.destroyQuietly();
		}
	}
	
	public static void show(String id, Mat mat, boolean resizeable) {
		OpenCvView view = getView(id, mat.size(), true, resizeable);
		view.draw(mat);
		view.updateView();
	}
	
	public static void show(String id, Mat mat) {
		show(id, mat, false);
	}
	
	public static void show(String id, MatConvas convas) {
		show(id, convas.getMat());
	}
	
	public static void show(String id, MatProxy proxy) {
		show(id, proxy.getMat());
	}
	
	public static void show(String id, Blob blob, Size size, Scalar color) {
		Mat convas = new Mat(size, CvType.CV_8UC3, OpenCvJ.BLACK);
		try {
			blob.draw(convas, color, Core.FILLED);
			show(id, convas);
		}
		finally {
			convas.release();
		}
	}
	
	public static void show(String id, Blob blob, Scalar color, Scalar holeColor, Size size) {
		Mat convas = new Mat(size, CvType.CV_8UC3, OpenCvJ.BLACK);
		try {
			blob.draw(convas, color, holeColor, Core.FILLED);
			show(id, convas);
		}
		finally {
			convas.release();
		}
	}
	
	public static void showDepthMap(String id, DepthMatProxy proxy) {
		try ( MatProxy colored = proxy.toColoredGray() ) {
			show(id, colored);
		}
		catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public static OpenCvView getView(String id, Size size) {
		return getView(id, size, true, false);
	}
	
	public static OpenCvView getView(String id, Size size, boolean visible,
													boolean resizable) {
		return getView(id, OpenCvJUtils.toSize2d(size), visible, resizable);
	}
	
	public static OpenCvView getView(String id, Size2d size, boolean visible,
													boolean resizable) {
		OpenCvView window = s_windows.get(id);
		if ( window == null ) {
			try {
				window = new OpenCvView();
				window.setTitle(id);
				window.setViewSize(size);
				window.setResizable(resizable);
				window.initialize();
				window.setVisible(visible);
				
				s_windows.put(id, window);
			}
			catch ( Exception e ) {
				throw new RuntimeException("" + e);
			}
		}
		
		return window;
	}
}
