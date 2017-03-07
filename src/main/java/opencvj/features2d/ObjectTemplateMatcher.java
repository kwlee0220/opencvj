package opencvj.features2d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.Point;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.KeyPoint;

import opencvj.Mats;
import opencvj.OpenCvJException;
import opencvj.misc.PerspectiveTransform;
import utils.Initializable;
import utils.UninitializedException;
import utils.config.ConfigNode;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class ObjectTemplateMatcher implements Initializable {
	private static final int MIN_QUERY_KEYPOINTS = 30;
	private static final int DEF_RANSAC_MATCH_COUNT = 30;
	private static final List<Match> EMPTY_MATCH = Collections.unmodifiableList(
																Collections.<Match>emptyList());
	
	public static class Params {
		/**
		 * 두 특징점 사이의 최대 허용 거리. 이 거리보다 같거나 가깝게 매치된 특징점들
		 * 만을 매치된 것으로 간주한다. 일반적으로 320x240 크기의 이미지를 사용하는 경우는
		 * 2.0~2.8 사이의 값을 사용하고, 640x480 크기의 이미지를 사용하는 경우는 2.0보다
		 * 같거나 작은 값을 사용한다.
		 */
		public float maxKeypointDistance;

		/**
		 * 물체 템플릿 매칭으로 인정하기 위한 최소 점수.
		 * 주어진 영상을 물체 DB내 등록된 각 물체들의 특징점 템플릿들과의 매칭 점수를 구하여
		 * 이 점수보다 같거나 높은 템플릿들만 후반 매칭 과정의 대상 후보로  간주한다.
		 * 물론 이 점수보다 높은 경우도 후반 과정을 통해 매칭되지 않은 것으로 결정될 수 있다.
		 * 이 값은 특징점 기술자 종류에 따라 값이 달라지므로, 사용하는 특징점 기술자에 따라
		 * 적절히 지정되어야 한다.
		 */
		public float matchScore;

		public int ransacMatchCount = DEF_RANSAC_MATCH_COUNT;

		public float definiteMatchScore;

		/**
		 * 주어진 영상 특징점과 가장 거리가 가까운 학습 영상 특징점 두개를 구한 후,
		 * 그 두 거리의 최대 비율 (1stNN / 2ndNN)로 0부터 1사이의 수.
		 * 만일 이 비율보다 작은 비율을 보이는 경우는 가장 가까운 특징점이 두번째로 가까운
		 * 특징점보다 월등히 질의 영상 특징점과 가깝다는 것을 의미하기 때문에, 두번째 매치는
		 * 버린다. 즉, 이 값을 작게 설정하면 큰 격차를 보인 경우만 두번째 특징점 매치를 버리게 된다.
		 */
		public float keypointDistanceRatio =0.85f;
		
		public static Params create(ConfigNode config) {
			Params params = new Params();
			params.maxKeypointDistance = config.get("max_keypoint_distance").asFloat();
			params.matchScore = config.get("match_score").asFloat();
			params.definiteMatchScore = config.get("definite_match_store").asFloat();
			params.ransacMatchCount = config.get("ransac_match_count").asInt(DEF_RANSAC_MATCH_COUNT);
			
			return params;
		}
	}
	
	public static class Match {
		public Template m_template;
		public double m_score;
		public Point[] m_corners;
		public List<DMatch> m_dmatches;
		
		Match(Template tmplt, double score, List<DMatch> dmatches) {
			m_template = tmplt;
			m_score = score;
			m_dmatches = dmatches;
		}
	}
	
	// properties (BEGIN)
	private ObjectTemplateStore m_store;
	private String m_matcherType;
	private DescriptorMatcher m_matcher;
	// properties (END)
	
	private Params m_params;
	private Template[] m_candidates =new Template[0];
	
	public final void setObjectTemplateStore(ObjectTemplateStore store) {
		m_store = store;
	}
	
	public final void setDescriptorMatcher(String type) {
		type = type.trim();
		if ( "bruteforce".equals(type) ) {
			m_matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);
		}
		else if ( "flann-based".equals(type) ) {
			m_matcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);
		}
		else if ( "bruteforce-hamming".equals(type) ) {
			m_matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
		}
		else {
			throw new OpenCvJException("undefined DescriptorMatcher type=" + type);
		}
		
		m_matcherType = type;
	}
	
	public final void setParams(Params params) {
		m_params = params;
	}

	@Override
	public void initialize() throws Exception {
		if ( m_store == null ) {
			throw new UninitializedException("Property 'objectTemplateStore' was not specified: class="
											+ getClass().getName());
		}
		if ( m_matcher == null ) {
			throw new UninitializedException("Property 'descriptorMatcher' was not specified: class="
											+ getClass().getName());
		}
		if ( m_params == null ) {
			throw new UninitializedException("Property 'params' was not specified: class="
											+ getClass().getName());
		}
	}

	@Override
	public void destroy() throws Exception { }
	
	public ImageStore getImageStore() {
		return m_store.getImageStore();
	}
	
	public ObjectTemplateStore getObjectTemplateStore() {
		return m_store;
	}
	
	public List<Match> match(Mat query) {
		if ( m_candidates.length == 0 ) {
			return EMPTY_MATCH;
		}
		
		Mat descriptors = new Mat();
		try {
			KeyPoint[] keypoints = m_store.extractFeature(query, null, descriptors);
			if ( keypoints.length <= MIN_QUERY_KEYPOINTS ) {
				return EMPTY_MATCH;
			}
			
			return match(keypoints, descriptors);
		}
		finally {
			descriptors.release();
		}
	}
	
	public void setCandidates(String... objectIds) {
		List<Mat> descriptorsList = new ArrayList<Mat>();
		m_candidates = new Template[objectIds.length];
		for ( int i=0; i < objectIds.length; ++i ) {
			final String id = objectIds[i];
			
			Template tmplt = m_store.getTemplate(id);
			if ( tmplt == null ) {
				throw new OpenCvJException("unknown object id=" + id);
			}
			
			m_candidates[i] = tmplt;
			descriptorsList.add(tmplt.descriptors);
		}
		
		if ( objectIds.length > 0 ) {
			m_matcher.add(descriptorsList);
			m_matcher.train();
		}
	}
	
	public void setCandidatesAll() {
		Collection<Template> tmplts = m_store.getTemplateAll();
		
		List<Mat> descriptorsList = new ArrayList<Mat>(tmplts.size());
		m_candidates = new Template[tmplts.size()];
		
		int i =0;
		for ( Template tmplt: tmplts ) {
			m_candidates[i] = tmplt;
			descriptorsList.add(tmplt.descriptors);
			
			++i;
		}
		
		if ( m_candidates.length > 0 ) {
			m_matcher.add(descriptorsList);
			m_matcher.train();
		}
	}
	
	private static class ScorePage {
		float score;
		float ransacScore;
		List<DMatch> dmatches;
		
		ScorePage() {
			score = 0;
			ransacScore = -1;
			dmatches = new ArrayList<DMatch>();
		}
	}
	
	
	private List<Match> match(KeyPoint[] keypoints, Mat descriptors) {
		// 주어진 특징 기술자를 이용하여 training image들 중에서 근접한 이미지를 찾는다.
		ScorePage[] scorePages = scoreImages(descriptors);
		
		List<Match> matches = new ArrayList<Match>();

		// 'm_params.definite_match_score' 보다 크거나 같은 템플릿은 무조건 인식된 것으로 간주한다.
		for ( int i =0; i < scorePages.length; ++i ) {
			final ScorePage page = scorePages[i];

			// 매칭 점수가 'm_params.match_score'보다 낮으면 무시한다.
			if ( page.score < m_params.matchScore ) {
				continue;
			}

			// 매칭 점수가 'm_params.definite_match_score'보다 낮으면 ransac 점수를 측정하여
			// 매칭 여부를 결정한다.
			if ( page.score < m_params.definiteMatchScore ) {
				List<DMatch> rmatches = Feature2ds.ransacTest(keypoints, m_candidates[i].keypoints,
																		page.dmatches, 3.0, 0.9, null);
				page.ransacScore = (float)rmatches.size() / page.dmatches.size();
				if ( rmatches.size() < m_params.ransacMatchCount ) {
					continue;
				}
				
				page.dmatches = rmatches;
			}

			Match match = new Match(m_candidates[i], page.score, page.dmatches);
			matches.add(match);

			if ( m_candidates[i].corners.length > 2 ) {
				// 가장 좋은 RANSAC_POINT_COUNT개의 매칭점을 이용하여 인식된 물체의 대략의 위치를 유추한다.
				List<DMatch> selecteds = Feature2ds.selectNBestMatches(page.dmatches, DEF_RANSAC_MATCH_COUNT);
				// 매치되는 두 이미지의 특징점 매핑을 이용하여 perspective mapping을 구한다.
				PerspectiveTransform trans = Feature2ds.createTransform(match.m_template.keypoints,
																		keypoints, selecteds);
				match.m_corners = trans.perform(m_candidates[i].corners);
			}
		}
		
		return matches;
	}
	
	private ScorePage[] scoreImages(Mat queryDescriptors) {
		int ncanidates = m_candidates.length;

		// 각 질의 특징에 대해 매치 후보 이미지내에서 거리가 m_params.max_keypoint_distance보다
		// 가깝게 매칭된 특징점들을 구한다.
		List<MatOfDMatch> modmList = new ArrayList<MatOfDMatch>();
		try {
			m_matcher.radiusMatch(queryDescriptors, modmList, m_params.maxKeypointDistance);
	
			//
			// 각 질의 특징점과 근접하게 매칭되는 training image의 출현횟수 누적한 후, training image의
			// 특징점 수로 나눈 평균 값을 이용하여 매칭 점수로 사용한다.
			// 이 매칭 점수가 m_params.min_match_score보다 큰 이미지를 찾는다.
			//
	
			// 매치 후보 이미지 갯수만큼 점수표를 초기화한다.
			ScorePage[] scorePage = new ScorePage[ncanidates];
			for ( int i =0; i < scorePage.length; ++i ) {
				scorePage[i] = new ScorePage();
			}
	
			for ( MatOfDMatch modm: modmList ) {
				// 각 질의 특징점과 매치된 N개의 이미지 특징점에 대해서 작업 수행.
				// 'ranks'는 각 질의 특징점에 대해 이것과 매치되는 학습 이미지들의 특징점들에 대한
				// 정보가 매치 정도가 멀어지는 순서대로 저장된다.
				final DMatch[] ranks = modm.toArray(); 
				if ( ranks.length == 0 ) {
					continue;
				}
	
				int[] candidateMatchCounter = new int[ncanidates];
				Arrays.fill(candidateMatchCounter, 0);
				
				float distThreshold = ranks[0].distance / m_params.keypointDistanceRatio;
				for ( int i = 0; i < ranks.length; ++i ) {
					// 가장 가까운 학습 특징점 거리보다 상대적으로 많이 먼 경우 매치된 것으로
					// 간주하지 않는다.
					if ( ranks[i].distance > distThreshold ) {
						break;
					}
	
					// 질의 이미지의 한 특징점이 동일 학습 이미지의 복수개의 특징점들과 매치되는
					// 경우가 있기 때문에 이때는 한번만 카운팅한다.
					int imgIdx = ranks[i].imgIdx;
					if ( ++candidateMatchCounter[imgIdx] == 1 ) {
						scorePage[imgIdx].dmatches.add(ranks[i]);
					}
				}
				for ( int i = 0; i < ncanidates; ++i ) {
					//	각 학습 이미지 별로 수집된 점수를 해당 학습 이미지의 전체 특징점 수로 나눈다.
					// 학습 이미지의 특징점 수를 이용하여 해당 학습 이미지로 매치된 특징점 수를 normalize시킨다.
					scorePage[i].score = (float)scorePage[i].dmatches.size()
												/ m_candidates[i].keypoints.length;
				}
			}
			
			return scorePage;
		}
		finally {
			Mats.releaseAll(modmList);
		}
	}
}
