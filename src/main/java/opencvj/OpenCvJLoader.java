package opencvj;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.Initializable;
import utils.UninitializedException;



/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class OpenCvJLoader implements Initializable {
	private static final Logger s_logger = LoggerFactory.getLogger(OpenCvJLoader.class);
	
	// properties (BEGIN)
	private volatile File m_dllDir;
	private volatile String m_dllName = "opencv_java2411";
	// properties (END)
	
	public OpenCvJLoader() { }

	@Inject
	public void setDllDir(File dllDir) {
		m_dllDir = dllDir;
	}
	
	public void setDllName(String dllName) {
		m_dllName = dllName;
	}

	@Override
	public void initialize() throws Exception {
		if ( m_dllDir == null ) {
			throw new UninitializedException("Property 'dllDir' was not specified: class="
											+ getClass().getName());
		}

		try {
			loadLibraries(new String[]{m_dllName}, m_dllDir);
		}
		catch ( Exception e ) {
			throw new RuntimeException("fails to load relevant DLL file: details=" + e);
		}
	}
	
	@Override public void destroy() { }
	
	public File getDllDir() {
		return m_dllDir;
	}
	
    private static void loadLibraries(String[] libNames, File dllDir) throws IOException {
//    	String prefix = OSValidator.getOSBits();
    	String prefix = "x86";
    	
    	if ( dllDir != null ) {
            for ( int i =0; i < libNames.length; ++i ) {
            	File dllFile = new File(new File(dllDir, prefix), libNames[i] + ".dll");
    			if ( dllFile.isFile() ) {
    				String path = dllFile.getAbsolutePath();
    				System.load(path);

        			s_logger.debug("loaded file={}", path);
    			}
    			else {
    				throw new IOException("invalid file path=" + dllFile.getAbsolutePath());
    			}
            }
    	}
    	else {
			s_logger.warn("fails to find JNI library dir");
    		
	        for ( int i =0; i < libNames.length; ++i ) {
	    		System.loadLibrary(libNames[i]);
        		
        		s_logger.debug("loaded from PATH: {}", libNames[i]);
	        }
    	}
	}
}
