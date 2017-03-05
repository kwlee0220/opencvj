package opencvj.blob;

import java.util.List;

import camus.service.SizeRange;
import camus.service.image.ImageView;

import org.apache.log4j.Logger;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

import opencvj.Config;
import opencvj.Mats;
import opencvj.OpenCvJ;
import opencvj.OpenCvViewManager;
import opencvj.camera.OpenCvJCamera;
import utils.io.IOUtils;



/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public final class Blobs {
	private Blobs() {
		throw new AssertionError("Should not be called this one: " + Blobs.class);
	}
	
	public static final void shift(List<Blob> blobs, Point offset) {
		for ( Blob blob: blobs ) {
			blob.shift(offset);
		}
	}
	
	public static final int learnBackground(OpenCvJCamera camera, BackgroundModel model,
											long millis) {
		return learnBackground(camera, model, millis, null);
	}
	
	public static final int learnBackground(OpenCvJCamera camera, BackgroundLearnable model,
											long millis, ImageView debugWindow) {
		final Mat image = new Mat();
		
		try {
			int count = 0;
			
			long due = System.currentTimeMillis() + millis;
			while ( System.currentTimeMillis() <= due ) {
				camera.capture(image);
				model.updateBackgroundModel(image);
				
				if ( debugWindow != null ) {
					debugWindow.draw(Mats.toBufferedImage(model.getBackground()));
					debugWindow.updateView();
				}
				
				++count;
			}
	
			return count;
		}
		finally {
			image.release();
		}
	}
	
	public static final int learnBackground(OpenCvJCamera camera, BackgroundModelAware model,
											long millis, ImageView debugWindow) {
		return learnBackground(camera, model.getBackgroundModel(), millis, debugWindow);
	}
	
	public static final int learnBackground(OpenCvJCamera camera, BackgroundLearnable bgModel,
											Config config, Logger logger) {
		int nframes = 0;
		
		long learningMillis = config.traverse("period").asDuration("0");
		if ( learningMillis > 0 ) {
			if ( logger != null && logger.isInfoEnabled() ) {
				logger.info("learning depth background: period=" + config.traverse("period").asString());
			}
			
			String winname = null;
			camera.open();
			try {
				camera.dropFrames(10);
	    		
				ImageView debugConvas = null;
	    		winname = config.traverse("winname").asString(null);
	    		if ( winname != null ) {
	    			debugConvas = OpenCvViewManager.getView(winname, camera.getSize(), true, false);
	    		}
	    		
	    		nframes = Blobs.learnBackground(camera, bgModel, learningMillis, debugConvas);
				if ( logger != null && logger.isInfoEnabled() ) {
					logger.info("depth background learned: nframes=" + nframes);
				}
			}
			finally {
				IOUtils.closeQuietly(camera);
				
				if ( winname != null ) {
					OpenCvViewManager.destroyView(winname);
				}
			}
		}
		
		return nframes;
	}
	
	private static final Scalar MAX = new Scalar(255);
	public static void newBlobMask(Size size, Mat mask, List<Blob> blobs) {
		Mats.createIfNotValid(mask, size, CvType.CV_8UC1, OpenCvJ.ALL_0);
		for ( Blob blob: blobs ) {
			blob.draw(mask, MAX, Core.FILLED);
		}
	}
	public static void newBlobMask(Size size, Mat mask, Blob... blobs) {
		Mats.createIfNotValid(mask, size, CvType.CV_8UC1, OpenCvJ.ALL_0);
		for ( Blob blob: blobs ) {
			blob.draw(mask, MAX, Core.FILLED);
		}
	}
	
	public static BlobFilter newSizeRangeBlobFilter(SizeRange range) {
		return new SizeRangeFilter(range);
	}
	
	public static BlobFilter newSizeRangeBlobFilter(Config config, boolean ignoreHoles) {
		return new SizeRangeFilter(config.asSizeRange(), ignoreHoles);
	}
	
	private static class SizeRangeFilter implements BlobFilter {
		private final SizeRange m_range;
		private final boolean m_ignoreHoles;
		
		SizeRangeFilter(SizeRange range, boolean ignoreHoles) {
			m_range = range;
			m_ignoreHoles = ignoreHoles;
		}
		
		SizeRangeFilter(SizeRange range) {
			this(range, true);
		}
		
		@Override
		public boolean apply(Blob blob, Mat image) {
			int size;
			if ( m_ignoreHoles ) {
				size = (int)Math.round(blob.area());
			}
			else {
				Rect bbox = blob.boundingBox();
				Mat roi = new Mat(image, bbox);
				size = Core.countNonZero(roi);
				roi.release();
			}
			
			return m_range.isIn(size);
		}
		
		@Override
		public String toString() {
			return "size=" + m_range;
		}
	}
}
