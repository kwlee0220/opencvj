package opencvj.features2d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.KeyPoint;

import opencvj.misc.PerspectiveTransform;




/**
 * 
 * @author Kang-Woo Lee
 */
public final class Feature2ds {
	private Feature2ds() {
		throw new AssertionError("Should not be called this one: " + Feature2ds.class);
	}
	
	public static PerspectiveTransform createTransform(KeyPoint[] trainKeypoints,
													KeyPoint[] queryKeypoints, List<DMatch> dmatches) {
		// Convert keypoints into Point
		Point[] points1 = new Point[dmatches.size()];
		Point[] points2 = new Point[dmatches.size()];
		
		for ( int i=0; i < dmatches.size(); ++i ) {
			final DMatch dmatch = dmatches.get(i);

			// Get the position of train keypoints
			points1[i] = trainKeypoints[dmatch.trainIdx].pt;
			// Get the position of query keypoints
			points2[i] = queryKeypoints[dmatch.queryIdx].pt;
		}
		
		return PerspectiveTransform.createRansacHomography(points1, points2);
	}

	public static List<DMatch> ransacTest(KeyPoint[] queryKeypoints, KeyPoint[] trainKeypoints,
											List<DMatch> dmatches, double distToEpipolar,
											double confidence, Mat fundamental) {
		return Feature2ds.ransacTest(queryKeypoints, trainKeypoints, dmatches, Calib3d.FM_RANSAC,
							distToEpipolar, confidence, fundamental);
	}

	public static List<DMatch> ransacTest(KeyPoint[] queryKeypoints, KeyPoint[] trainKeypoints,
											List<DMatch> dmatches, int method, double distToEpipolar,
											double confidence, Mat fundamental) {
		List<DMatch> ransacMatches = new ArrayList<DMatch>();
		if ( dmatches.size() == 0 ) {
			return ransacMatches;
		}
		
		// Convert keypoints into Point2f
		Point[] points1 = new Point[dmatches.size()];
		Point[] points2 = new Point[dmatches.size()];
		
		for ( int i=0; i < dmatches.size(); ++i ) {
			final DMatch dmatch = dmatches.get(i);
			
			// Get the position of query keypoints
			points1[i] = trainKeypoints[dmatch.trainIdx].pt;
			// Get the position of train keypoints
			points2[i] = queryKeypoints[dmatch.queryIdx].pt;
		}
	
		// Compute F matrix using RANSAC
		MatOfPoint2f mop1 = new MatOfPoint2f(points1);
		MatOfPoint2f mop2 = new MatOfPoint2f(points2);
		MatOfByte mobInliners = new MatOfByte();
		Mat fund = null;
			
		try {
			fund = Calib3d.findFundamentalMat(mop1, mop2, method, distToEpipolar, confidence,
												mobInliners);
			
			// extract the surviving (inliers) matches
			byte[] inliners = mobInliners.toArray();
			for ( int i =0; i < inliners.length; ++i ) {
				if ( inliners[i] != 0 ) {
					ransacMatches.add(dmatches.get(i));
				}
			}
			if ( fundamental != null ) {
				fund.copyTo(fundamental);
			}
			
			return ransacMatches;
		}
		finally {
			mop1.release();
			mop2.release();
			mobInliners.release();
			
			if ( fund != null ) {
				if ( fundamental != null ) {
					fundamental.copyTo(fund);
				}
				fund.release();
			}
		}
	}
	
	private static final Comparator<DMatch> DISTANCE_COMPARATOR = new Comparator<DMatch>() {
		@Override
		public int compare(DMatch dmatch1, DMatch dmatch2) {
			return Float.compare(dmatch1.distance, dmatch2.distance);
		}
	};
	
	public static List<DMatch> selectNBestMatches(List<DMatch> dmatches, int count) {
		Collections.sort(dmatches, DISTANCE_COMPARATOR);
		return dmatches.subList(0, count);
	}
}
