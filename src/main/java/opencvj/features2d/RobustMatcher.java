package opencvj.features2d;

import java.util.ArrayList;
import java.util.List;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.KeyPoint;

import opencvj.Mats;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class RobustMatcher {
	public static class Params {
		/** max ratio between 1st and 2nd NN (1st NN / 2nd NN) */
		public double dominantRatio = 0.65;
		/** confidence level (probability) */
		public double confidence = 0.9;
		/** min distance to epipolar */
		public double distanceToEpipolar =3.0;
		/** if true will refine the F matrix */
		public boolean refineF =false;
	}
	
	private Params m_params;
	private DescriptorMatcher m_matcher;
	
	public static RobustMatcher createBruteForceMatcher(Params params) {
		return new RobustMatcher(DescriptorMatcher.BRUTEFORCE, params);
	}
	
	public static RobustMatcher createHammingMatcher(Params params) {
		return new RobustMatcher(DescriptorMatcher.BRUTEFORCE_HAMMING, params);
	}
	
	public static RobustMatcher createFlannBasedMatcher(Params params) {
		return new RobustMatcher(DescriptorMatcher.FLANNBASED, params);
	}
	
	public RobustMatcher(int matcherType, Params params) {
		m_params = params;
		m_matcher = DescriptorMatcher.create(matcherType);
	}
	
	public List<DMatch> match(KeyPoint[] keypoints1, KeyPoint[] keypoints2,
								Mat descriptors1, Mat descriptors2, Mat fundamental) {
		List<DMatch> matches = match(descriptors1, descriptors2);

		// Validate matches using RANSAC
		matches = ransacMatch(keypoints1, keypoints2, matches, fundamental);

		// return the found fundemental matrix
		return matches;
	}
	
	public List<DMatch> match(Mat queryDescriptors, Mat trainDescriptors) {
		List<DMatch> symDMatches = new ArrayList<DMatch>();
		
		if ( queryDescriptors.size().height <= 0 || trainDescriptors.size().height <= 0 ) {
			return symDMatches;
		}

		//
		// 두개의 descriptor들을 이용하여 상호 방향으로 후보 2개짜리 최근접 매칭을 구한다.
		//
		List<MatOfDMatch> modms1 = new ArrayList<MatOfDMatch>();
		List<MatOfDMatch> modms2 = new ArrayList<MatOfDMatch>();
		try {
			m_matcher.knnMatch(queryDescriptors, trainDescriptors, modms1, 2);
			m_matcher.knnMatch(trainDescriptors, queryDescriptors, modms2, 2);

			//
			// 최근접 1등과 2등 사이의 격차가 많지 않은 match들을 clear시킨다.
			//
			List<DMatch> matches1 = ratioTest(modms1);
			List<DMatch> matches2 = ratioTest(modms2);

			//
			// 상호간의 최근접 매치가 아닌 경우를 제거한다.
			//
			return symmetryMatch(matches1, matches2);
		}
		finally {
			Mats.releaseAll(modms1);
			Mats.releaseAll(modms2);
		}
	}
	
	public List<DMatch> ransacMatch(KeyPoint[] queryKeypoints, KeyPoint[] trainKeypoints,
										List<DMatch> dmatches, Mat fundamental) {
		List<DMatch> rmatches = Feature2ds.ransacTest(queryKeypoints, trainKeypoints, dmatches,
											m_params.distanceToEpipolar, m_params.confidence,
											fundamental);
		if ( rmatches.size() == 0 || !m_params.refineF ) {
			return rmatches;
		}
		
		return Feature2ds.ransacTest(queryKeypoints, trainKeypoints, rmatches, Calib3d.FM_8POINT, 0, 0, fundamental);
	}
	
	private List<DMatch> ratioTest(List<MatOfDMatch> matches) {
		List<DMatch> filtereds = new ArrayList<DMatch>();
		for ( MatOfDMatch modm: matches ) {
			DMatch[] dmatches = modm.toArray();
			
			// if 2 NN has been identified
			if ( dmatches.length > 1 ) {
				// 2NN에서 1등의 거리와 2등의 거리의 격차가 일정 부분 이상인 것만 robust한 것으로
				// 간주한다.
				if (dmatches[0].distance / dmatches[1].distance <= m_params.dominantRatio) {
					filtereds.add(dmatches[0]);
				}
			}
		}

		return filtereds;
	}
	
	private List<DMatch> symmetryMatch(List<DMatch> matches1, List<DMatch> matches2) {
		List<DMatch> symDMatches = new ArrayList<DMatch>();
		
		// for all matches image 1 -> image 2
		for ( DMatch dmatch1: matches1 ) {
			// for all matches image 2 -> image 1
			for ( DMatch dmatch2: matches2 ) {
				// Match symmetry test
				if ( dmatch1.queryIdx == dmatch2.trainIdx
					&& dmatch2.queryIdx == dmatch1.trainIdx ) {
					// add symmetrical match
					symDMatches.add(dmatch1);
					break; // next match in image 1 -> image 2
				}
			}
		}
		
		return symDMatches;
	}
}
