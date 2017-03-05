package opencvj.features2d;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.opencv.core.Point;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import opencvj.OpenCvJException;
import utils.Initializable;
import utils.UninitializedException;
import utils.xml.XmlUtils;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class ImageStore implements Initializable {
    private static final Logger s_logger = Logger.getLogger("OPENCVJ.IMAGE_STORE");
    
	// properties (BEGIN)
	private File m_storeDir;
	// properties (END)
	
	private Map<String,ImageEntry> m_entries = new HashMap<String,ImageEntry>();
	
	public ImageStore() { }
	
	public final void setStoreDir(File storeDir) {
		m_storeDir = storeDir;
	}

	@Override
	public void initialize() throws Exception {
		if ( m_storeDir == null ) {
			throw new UninitializedException("Property 'storeDir' was not specified: class="
											+ getClass().getName());
		}
		
		if ( s_logger.isInfoEnabled() ) {
			s_logger.info(String.format("loading image files from " + m_storeDir.getCanonicalPath()));
		}
		load(m_storeDir);
	}

	@Override
	public void destroy() throws Exception { }
	
	public ImageEntry get(String id) {
		return m_entries.get(id);
	}
	
	public boolean exists(String id) {
		return m_entries.containsKey(id);
	}
	
	public Collection<ImageEntry> entries() {
		return m_entries.values();
	}
	
	public void load(File rootDir) {
		List<ImageEntry> entryList = new ArrayList<ImageEntry>();
		visitDir(rootDir, entryList);
		
		m_entries.clear();
		for ( ImageEntry e: entryList ) {
			ImageEntry prev = m_entries.put(e.id, e);
			if ( prev != null ) {
				throw new OpenCvJException("duplicated image entry: id=" + e.id);
			}
		}
	}
	
	private int visitDir(File dir, List<ImageEntry> entryList) {
		int count = 0;

		File descFile = new File(dir, "image_descriptors.xml");
		if ( descFile.canRead() ) {
			count += loadDescriptor(descFile, entryList);
		}

		File[] subDirs = dir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});
		if ( subDirs != null ) {
			for ( File subDir: subDirs ) {
				count += visitDir(subDir, entryList);
			}
		}
		
		return count;
	}
	
	private int loadDescriptor(File descFile, List<ImageEntry> entryList) {
		if ( !descFile.exists() ) {
			throw new OpenCvJException(String.format("cannot open image_descriptor: path="
													+ descFile.getAbsolutePath()));
		}

        Document doc = null;
        try {
            doc = XmlUtils.parse(descFile);
        }
        catch ( SAXException e ) {
            throw new OpenCvJException("Invalid image_descriptor file format: "
                    							+ descFile.getAbsolutePath() + ", details=" + e);
        } catch (IOException e) {
            throw new OpenCvJException("Unable to read image_descriptor file: "
    											+ descFile.getAbsolutePath());
        }
        
        Element rootElm = doc.getDocumentElement();
		if ( !rootElm.getLocalName().equals("images") ) {
			throw new OpenCvJException("not image descriptor: file=" + descFile.getAbsolutePath()
										+ " top-level element=" + rootElm.getNodeName());
		}
		
		int count = 0;
		File parentDir = descFile.getParentFile();
		for ( Element imageElm: XmlUtils.getChildElements(rootElm, "image") ) {
			ImageEntry e = new ImageEntry();
			e.id = XmlUtils.getChildElementText(imageElm, "id").get();
			e.imageFile = new File(parentDir, XmlUtils.getChildElementText(imageElm, "file").get());
			if ( !e.imageFile.exists() ) {
				s_logger.warn("fails to load image file: " + e.imageFile);
				continue;
			}
			String cornerStr = XmlUtils.getChildElementText(imageElm, "corners").orElse(null);
			if ( cornerStr != null ) {
				String[] numbers = cornerStr.trim().split(" ");
				e.corners = new Point[numbers.length/2];
				for ( int i =0; i < e.corners.length; ++i ) {
					e.corners[i] = new Point(Float.parseFloat(numbers[2*i]),
												Float.parseFloat(numbers[2*i+1]));
				}
			}
			else {
				e.corners = null;
			}
			
			if ( s_logger.isDebugEnabled() ) {
				s_logger.debug("loading image file: " + e.imageFile);
			}
			
			entryList.add(e);
			++count;
		}
		
		return count;
	}
}
