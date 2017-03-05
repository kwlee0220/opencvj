package opencvj.features2d;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.KeyPoint;

import opencvj.OpenCvJException;
import opencvj.features2d.OpenCvInputStream.ObjectReader;
import opencvj.features2d.OpenCvOutputStream.ObjectWriter;
import utils.Initializable;
import utils.UninitializedException;
import utils.io.IOUtils;



/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class ObjectTemplateStore implements Initializable {
	private static final int MAGIC_NUMBER = 5685;
	
	// properties (BEGIN)
	private File m_tmpltFile;
	private ImageStore m_store;
	private String m_featureType;
	private String m_descriptorType;
	private FeatureDetector m_detector;
	private DescriptorExtractor m_extractor;
	private boolean m_autoSave = true;
	// properties (END)
	
	private boolean m_updated = false;
	private Map<String,Template> m_tmplts = new HashMap<String,Template>();
	
	public ObjectTemplateStore() { }
	
	public final void setTemplateStore(File file) {
		m_tmpltFile = file;
	}
	
	public final void setImageStore(ImageStore store) {
		m_store = store;
	}
	
	public final void setFeatureDetector(String type) {
		type = type.trim();
		if ( "sift".equals(type) ) {
			m_detector = FeatureDetector.create(FeatureDetector.SIFT);
		}
		else if ( "orb".equals(type) ) {
			m_detector = FeatureDetector.create(FeatureDetector.ORB);
		}
		else if ( "surf".equals(type) ) {
			m_detector = FeatureDetector.create(FeatureDetector.SURF);
		}
		else {
			throw new OpenCvJException("undefined FeatureDetector type=" + type);
		}
		
		m_featureType = type;
	}
	
	public final void setDescriptorExtractor(String type) {
		type = type.trim();
		if ( "sift".equals(type) ) {
			m_extractor = DescriptorExtractor.create(DescriptorExtractor.SIFT);
		}
		else if ( "orb".equals(type) ) {
			m_extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
		}
		else if ( "surf".equals(type) ) {
			m_extractor = DescriptorExtractor.create(DescriptorExtractor.SURF);
		}
		else {
			throw new OpenCvJException("undefined DescriptorExtractor type=" + type);
		}
		
		m_descriptorType = type;
	}
	
	public final void setAutoSave(boolean save) {
		m_autoSave = save;
	}

	@Override
	public void initialize() throws Exception {
		if ( m_tmpltFile != null ) {
			if ( m_tmpltFile.exists() ) {
				load(m_tmpltFile);
			}
		}
		if ( m_detector == null ) {
			throw new UninitializedException("Property 'featureDetector' was not specified: class="
											+ getClass().getName());
		}
		if ( m_extractor == null ) {
			throw new UninitializedException("Property 'descriptorExtractor' was not specified: class="
											+ getClass().getName());
		}

		if ( m_store != null ) {
			for ( ImageEntry e: m_store.entries() ) {
				if ( !m_tmplts.containsKey(e.id) ) {
					addTemplate(e.id, e.loadImage(), e.corners);
				}
			}
		}
	}

	@Override
	public void destroy() throws Exception {
		if ( m_updated ) {
			save();
		}
	}
	
	public ImageStore getImageStore() {
		return m_store;
	}
	
	public Template getTemplate(String id) {
		return m_tmplts.get(id);
	}
	
	public Collection<Template> getTemplateAll() {
		return Collections.unmodifiableCollection(m_tmplts.values());
	}
	
	public Template addTemplate(String id, Mat image, Point[] corners) {
		Template tmplt = new Template();
		tmplt.id = id;
		tmplt.imageSize = image.size();
		tmplt.corners = corners;
		
		try {
			tmplt.descriptors = new Mat();
			tmplt.keypoints = extractFeature(image, null, tmplt.descriptors);
		}
		catch ( Throwable e ) {
			tmplt.descriptors.release();
			tmplt.descriptors = null;
		}
		
		Template prev = m_tmplts.put(id, tmplt);
		if ( prev != null ) {
			m_tmplts.put(id, prev);
			throw new OpenCvJException("template already exists: id=" + id);
		}
		m_updated = true;
		
		return tmplt;
	}
	
	public void removeTemplate(String id) {
		m_tmplts.remove(id);
		m_updated = true;
	}
	
	public boolean isUpdated() {
		return m_updated;
	}
	
	public void save() throws IOException {
		if ( m_tmpltFile != null ) {
			Files.createDirectories(m_tmpltFile.getParentFile().toPath());

			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(m_tmpltFile));
			OpenCvOutputStream oos = new OpenCvOutputStream(new DataOutputStream(bos));
			try {
				oos.writeInt(MAGIC_NUMBER);
				oos.writeString(m_featureType);
				oos.writeString(m_descriptorType);
				oos.writeCollection(m_tmplts.values(), TEMPLATE_WRITER);
				
				m_updated = false;
			}
			catch ( IOException e ) {
				throw e;
			}
			finally {
				IOUtils.closeQuietly(oos);
				m_tmpltFile.delete();
			}
		}
		else {
			throw new OpenCvJException("TemplateStore file has not been defined");
		}
	}
	
	public KeyPoint[] extractFeature(Mat image, Mat mask, Mat descriptors) {
		MatOfKeyPoint mokp = new MatOfKeyPoint();
		try {
			if ( mask != null && mask.size().equals(image.size()) ) {
				m_detector.detect(image, mokp, mask);
			}
			else {
				m_detector.detect(image, mokp);
			}
			if ( descriptors != null ) {
				m_extractor.compute(image, mokp, descriptors);
			}
			
			return mokp.toArray();
		}
		finally {
			mokp.release();
		}
	}
	
	private void load(File storeFile) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(storeFile));
		OpenCvInputStream ois = new OpenCvInputStream(new DataInputStream(bis));
		try {
			int magicCode = ois.readInt();
			if ( magicCode != MAGIC_NUMBER ) {
				throw new OpenCvJException("store might be corrupted, invalid magic code");
			}
			
			String featureType = ois.readString();
			String descriptorType = ois.readString();
			
			if ( m_detector == null ) {
				setFeatureDetector(featureType);
			}
			if ( m_extractor == null ) {
				setDescriptorExtractor(descriptorType);
			}
			
			Template[] tmplts = ois.readArray(Template.class, TEMPLATE_READER);
			for ( Template tmplt: tmplts ) {
				m_tmplts.put(tmplt.id, tmplt);
			}
		}
		finally {
			IOUtils.closeQuietly(ois);
		}
	}
	
	private final ObjectReader<Template> TEMPLATE_READER = new ObjectReader<Template>() {
		@Override
		public Template read(OpenCvInputStream istream) throws IOException {
			Template tmplt = new Template();
			tmplt.id = istream.readString();
			tmplt.imageSize = istream.readSize2i();
			tmplt.corners = istream.readPoint2fArray();
			tmplt.keypoints = istream.readKeyPoints();
			tmplt.descriptors = new Mat();
			istream.readMat(tmplt.descriptors);
			
			return tmplt;
		}
	};
	
	private final ObjectWriter<Template> TEMPLATE_WRITER = new ObjectWriter<Template>() {
		@Override
		public void write(OpenCvOutputStream os, Template tmplt) throws IOException {
			os.writeString(tmplt.id);
			os.writeSize2i(tmplt.imageSize);
			os.writePoint2fArray(tmplt.corners);
			os.writeKeyPoints(tmplt.keypoints);
			os.writeMat(tmplt.descriptors);
		}
	};
}