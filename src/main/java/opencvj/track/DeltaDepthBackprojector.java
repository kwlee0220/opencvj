package opencvj.track;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;

import opencvj.Config;
import opencvj.Mats;
import opencvj.OpenCvJ;
import opencvj.OpenCvJUtils;
import opencvj.blob.BackgroundModel;
import opencvj.blob.BackgroundModelAware;
import opencvj.blob.DeltaAwareForegroundDetector;
import opencvj.misc.Histogram1D;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class DeltaDepthBackprojector implements Backprojector, BackgroundModelAware {
	private DeltaAwareForegroundDetector m_fgDetector;
	private final Histogram1D m_hist;
	
	public static DeltaDepthBackprojector create(DeltaAwareForegroundDetector fgDetector, Config config) {
		Histogram1D hist = Histogram1D.create(config);
		return new DeltaDepthBackprojector(fgDetector, hist);
	}
	
	public DeltaDepthBackprojector(DeltaAwareForegroundDetector fgDetector, Histogram1D hist) {
		m_fgDetector = fgDetector;
		m_hist = hist;
	}

	@Override
	public void close() {
		m_hist.close();
	}

	@Override
	public void load(Mat image, Mat mask) {
		Mat delta32f = new Mat();
		Mat fgMask = new Mat();
		Mat convas = new Mat(new Size(320,240), CvType.CV_8UC3, OpenCvJ.WHITE);
		try {
			m_fgDetector.detectBackgroundDelta(image, null, delta32f, fgMask);
			Core.bitwise_and(mask, fgMask, fgMask);
			
			m_hist.load(delta32f, fgMask);
//			m_hist.draw(new MatConvas(convas), OpenCvJ.BLACK, OpenCvJ.RED);
//			WindowManager.show("hist", convas);
		}
		finally {
			Mats.releaseAll(delta32f, fgMask, convas);
		}
	}

	@Override
	public void clear() { 
		m_hist.clear();
	}

	@Override
	public BackgroundModel getBackgroundModel() {
		return m_fgDetector.getBackgroundModel();
	}

	@Override
	public void backproject(Mat image, Mat proj) {
		Mat delta32f = new Mat();
		Mat fgMask = new Mat();
		try {
			m_fgDetector.detectBackgroundDelta(image, null, delta32f, fgMask);
			m_hist.backproject(delta32f, proj);
		}
		finally {
			Mats.releaseAll(delta32f, fgMask);
		}
	}

	@Override
	public void backproject(Mat image, Rect roi, Mat proj) {
		Mat delta32f = new Mat();
		Mat fgMask = new Mat();
		try {
			m_fgDetector.detectBackgroundDelta(image, OpenCvJUtils.toCvPoints(roi), delta32f, fgMask);
			
			Mat delta32fRoi = new Mat(delta32f, roi);
			m_hist.backproject(delta32fRoi, proj);
			delta32fRoi.release();
		}
		finally {
			Mats.releaseAll(delta32f, fgMask);
		}
	}
}
