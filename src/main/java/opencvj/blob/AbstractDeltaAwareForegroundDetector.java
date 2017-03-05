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
public abstract class AbstractDeltaAwareForegroundDetector extends AbstractForegroundDetector
														implements DeltaAwareForegroundDetector {
	protected abstract void calcForegroundMask(Mat image, Rect roi, Mat delta32f, Mat fgMask);
	
	public AbstractDeltaAwareForegroundDetector(BlobExtractor filter) {
		super(filter);
	}

	@Override
	public void detectBackgroundDelta(Mat image, Point[] corners, Mat delta32f, Mat fgMask) {
		final Blob roiArea = (corners != null) ? new Blob(corners) : null;
		final Rect roi = (roiArea != null) ? roiArea.boundingBox() : null;

		Mat roiMask = new Mat();
		try {
			calcForegroundMask(image, roi, delta32f, fgMask);
			if ( roi != null ) {
				Blobs.newBlobMask(image.size(), roiMask, roiArea);
				Core.bitwise_and(fgMask, roiMask, fgMask);
			}
			
			if ( !m_filter.isRestrictable() ) {
				List<Blob> blobs = m_filter.extractBlobs(fgMask);
				fgMask.setTo(OpenCvJ.ALL_0);
				Blobs.newBlobMask(image.size(), fgMask, blobs);
			}
		}
		finally {
			roiMask.release();
		}
	}

	@Override
	public List<Blob> extractForegroundBlobs(Mat image, Point[] corners, Mat delta32f, Mat fgMask) {
		final Blob roiArea = (corners != null) ? new Blob(corners) : null;
		final Rect roi = (roiArea != null) ? roiArea.boundingBox() : null;
		
		Mat roiMask = new Mat();
		Mat tmp = new Mat();
		try {
			calcForegroundMask(image, roi, delta32f, tmp);
			if ( roi != null ) {
				Blobs.newBlobMask(image.size(), roiMask, roiArea);
				Core.bitwise_and(tmp, roiMask, tmp);
			}
			
			List<Blob> blobs = m_filter.extractBlobs(tmp);
			Blobs.newBlobMask(image.size(), fgMask, blobs);
			
			return blobs;
		}
		finally {
			Mats.releaseAll(roiMask, tmp);
		}
	}

	@Override
	public Blob extractLargestForegroundBlob(Mat image, Point[] corners, Mat delta32f, Mat fgMask) {
		List<Blob> blobs = extractForegroundBlobs(image, corners, delta32f, fgMask);
		return Collections.min(blobs, Blob.AREA_COMP_DESC);
	}
	
	@Override
	public List<Blob> extractKLargestForegroundBlobs(Mat image, int k, Point[] corners, Mat delta32f,
														Mat fgMask) {
		List<Blob> blobs = extractForegroundBlobs(image, corners, delta32f, fgMask);
		Collections.sort(blobs, Blob.AREA_COMP_DESC);
		if ( blobs.size() > k ) {
			return blobs.subList(0, k-1);
		}
		else {
			return blobs;
		}
	}
}
