package opencvj.blob;

import java.util.Collections;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;

import opencvj.Mats;
import opencvj.OpenCvJ;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public abstract class AbstractForegroundDetector implements ForegroundDetector {
	protected final BlobExtractor m_filter;
	private final Mat m_tmpFgMask = new Mat();
	private final Mat m_tmpRoiMask = new Mat();
	
	protected abstract void calcForegroundMask(Mat image, Rect roi, Mat fgMask);
	
	public AbstractForegroundDetector(BlobExtractor filter) {
		m_filter = filter;
	}
	
	@Override
	public void close() {
		Mats.releaseAll(m_tmpFgMask, m_tmpRoiMask);
	}

	@Override
	public void detectForeground(Mat image, Point[] corners, Mat fgMask) {
		final Blob roiArea = (corners != null) ? new Blob(corners) : null;
		final Rect roi = (roiArea != null) ? roiArea.boundingBox() : null;
		
		calcForegroundMask(image, roi, fgMask);
		if ( roi != null ) {
			Blobs.newBlobMask(image.size(), m_tmpRoiMask, roiArea);
			Core.bitwise_and(fgMask, m_tmpRoiMask, fgMask);
		}
		
		if ( !m_filter.isRestrictable() ) {
			List<Blob> blobs = m_filter.extractBlobs(fgMask);
			fgMask.setTo(OpenCvJ.ALL_0);
			Blobs.newBlobMask(image.size(), fgMask, blobs);
		}
	}

	@Override
	public List<Blob> extractForegroundBlobs(Mat image, Point[] corners, Mat fgMask) {
		final Blob roiArea = (corners != null) ? new Blob(corners) : null;
		final Rect roi = (roiArea != null) ? roiArea.boundingBox() : null;

		calcForegroundMask(image, roi, m_tmpFgMask);
		if ( roi != null ) {
			Blobs.newBlobMask(image.size(), m_tmpRoiMask, roiArea);
			Core.bitwise_and(m_tmpFgMask, m_tmpRoiMask, m_tmpFgMask);
		}
		
		List<Blob> blobs = m_filter.extractBlobs(m_tmpFgMask);
		Blobs.newBlobMask(image.size(), fgMask, blobs);
		
		return blobs;
	}

	@Override
	public Blob extractLargestForegroundBlob(Mat image, Point[] corners, Mat fgMask) {
		List<Blob> blobs = extractForegroundBlobs(image, corners, fgMask);
		return Collections.min(blobs, Blob.AREA_COMP_DESC);
	}
	
	@Override
	public List<Blob> extractKLargestForegroundBlobs(Mat image, int k, Point[] corners, Mat fgMask) {
		List<Blob> blobs = extractForegroundBlobs(image, corners, fgMask);
		Collections.sort(blobs, Blob.AREA_COMP_DESC);
		if ( blobs.size() > k ) {
			return blobs.subList(0, k-1);
		}
		else {
			return blobs;
		}
	}
}
