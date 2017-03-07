package opencvj.track;

import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.KeyPoint;

import opencvj.OpenCvJException;
import opencvj.blob.Blob;
import opencvj.features2d.Feature2ds;
import opencvj.features2d.ObjectTemplateStore;
import opencvj.features2d.RobustMatcher;
import opencvj.features2d.Template;
import opencvj.misc.PerspectiveTransform;
import utils.config.ConfigNode;



/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class RegionalObjectTracker extends RegionalTracker<Blob,BlobTrackable> {
	public static final int DEF_GOOD_MATCH_COUNT = 64;
	
	public static class Params extends RegionalTracker.Params {
		public int goodMatchCount = DEF_GOOD_MATCH_COUNT;
		
		public Params(ConfigNode config) {
			super(config);
			
			goodMatchCount = config.get("good_match_count").asInt(DEF_GOOD_MATCH_COUNT);
		}
	}
	
	private ObjectTemplateStore m_store;
	private RobustMatcher m_matcher;
	private Template m_target;
	
	public static RegionalObjectTracker create(ObjectTemplateStore store,
												BlobTrackable target, ConfigNode config) {
		return new RegionalObjectTracker(store, target, new Params(config));
	}
	
	public RegionalObjectTracker(ObjectTemplateStore store, BlobTrackable target, Params params) {
		super(params);
		
		setTarget(target);
		m_store = store;
		m_target = store.getTemplate(target.getId());
		if ( m_target == null ) {
			throw new OpenCvJException("unknown target object: id=" + target.getId());
		}
		m_matcher = RobustMatcher.createBruteForceMatcher(new RobustMatcher.Params());
	}

	@Override
	protected Blob locate(Mat image) {
		Mat descriptors = new Mat();
		KeyPoint[] keypoints = m_store.extractFeature(image, null, descriptors);
		
		List<DMatch> dmatches = m_matcher.match(descriptors, m_target.descriptors);
		if ( dmatches.size() < ((Params)m_params).goodMatchCount ) {
			return null;
		}
		
		dmatches = m_matcher.ransacMatch(keypoints, m_target.keypoints, dmatches, null);
		if ( dmatches.size() < ((Params)m_params).goodMatchCount ) {
			return null;
		}
		
		dmatches = Feature2ds.selectNBestMatches(dmatches, ((Params)m_params).goodMatchCount);
		PerspectiveTransform trans = Feature2ds.createTransform(m_target.keypoints,
																keypoints, dmatches);
		return new Blob(trans.perform(m_target.corners));
	}
	

	@Override
	protected Blob locate(Mat image, Blob window) {
		Rect roi = window.boundingBox();
		Mat imageRoi = new Mat(image, roi);
		try {
			Blob location = locate(imageRoi);
			if ( location != null ) {
				location.shift(roi.tl());
			}
			return location;
		}
		finally {
			imageRoi.release();
		}
	}
}