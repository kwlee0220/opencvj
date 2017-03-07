package opencvj.features2d.maze;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import com.google.common.collect.Lists;

import opencvj.features2d.ImageEntry;
import opencvj.features2d.ImageStore;
import opencvj.features2d.ObjectTemplateMatcher;
import opencvj.features2d.ObjectTemplateMatcher.Match;
import opencvj.features2d.ObjectTemplateStore;
import utils.config.ConfigNode;
import utils.io.IOUtils;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class MazeMatcher {
	private ObjectTemplateMatcher m_matcher;
	
	public static MazeMatcher create(ConfigNode config) throws Exception {
		ImageStore imageStore = new ImageStore();
		imageStore.setStoreDir(config.get("maze_dir").asFile());
		imageStore.initialize();
		
		ConfigNode tmpltConfig = config.get("maze_template");
		
		ObjectTemplateStore tmpltStore = new ObjectTemplateStore();
		tmpltStore.setImageStore(imageStore);
		tmpltStore.setTemplateStore(tmpltConfig.get("db").asFile());
		tmpltStore.setFeatureDetector(tmpltConfig.get("feature").asString());
		tmpltStore.setDescriptorExtractor(tmpltConfig.get("descriptor").asString());
		tmpltStore.initialize();
		if ( tmpltStore.isUpdated() ) {
			tmpltStore.save();
		}
		
		ObjectTemplateMatcher tmpltMatcher = new ObjectTemplateMatcher();
		tmpltMatcher.setObjectTemplateStore(tmpltStore);
		tmpltMatcher.setDescriptorMatcher(tmpltConfig.get("matcher").asString());
		tmpltMatcher.setParams(ObjectTemplateMatcher.Params.create(tmpltConfig));
		tmpltMatcher.initialize();
		tmpltMatcher.setCandidatesAll();
		
		return new MazeMatcher(tmpltMatcher);
	}
	
	private MazeMatcher(ObjectTemplateMatcher matcher) {
		m_matcher = matcher;
	}
	
	public ObjectTemplateStore getObjectTemplateStore() {
		return m_matcher.getObjectTemplateStore();
	}
	
	public MazeInfo match(Mat image) throws IOException {
		List<Match> matches = m_matcher.match(image);
		if ( matches.size() == 0 ) {
			return null;
		}
		
		final Match match = matches.get(0);
		
		MazeInfo info = new MazeInfo();
		info.m_id = match.m_template.id;
		info.m_corners = match.m_corners;
		info.m_imageEntry = m_matcher.getImageStore().get(info.m_id);
		info.m_solutionPath = parseMazePath(info.m_imageEntry);

		return info;
	}
	
	public List<MazeInfo> matchAll(Mat image) throws IOException {
		List<MazeInfo> infoList = Lists.newArrayList();
		
		List<Match> matches = m_matcher.match(image);
		if ( matches.size() == 0 ) {
			return infoList;
		}
		
		for ( Match match: matches ) {
			try {
				MazeInfo info = new MazeInfo();
				info.m_id = match.m_template.id;
				info.m_corners = match.m_corners;
				info.m_imageEntry = m_matcher.getImageStore().get(info.m_id);
				info.m_solutionPath = parseMazePath(info.m_imageEntry);
				
				infoList.add(info);
			}
			catch ( Exception ignored ) { }
		}
		
		return infoList;
	}
	
	private Point[] parseMazePath(ImageEntry ent) throws IOException {
		File solutionFile = new File(ent.imageFile.getParentFile(),
									String.format("%s_solution", ent.id));
		StringWriter writer = new StringWriter();
		Reader reader = new BufferedReader(new FileReader(solutionFile));
		try {
			char[] buffer = new char[4096];
			int nread;
			while ( (nread = reader.read(buffer)) >= 0 ) {
				writer.write(buffer, 0, nread);
			}
		}
		finally {
			IOUtils.closeQuietly(reader);
		}
		String[] parts = writer.toString().replaceAll("[\\t\\n\\r]+", " ").split(", ");
		
		Point[] points = new Point[parts.length/2];
		for ( int i =0; i < points.length; ++i ) {
			points[i] = new Point(Integer.parseInt(parts[2*i]), Integer.parseInt(parts[2*i+1]));
		}
		
		return points;
	}
}
