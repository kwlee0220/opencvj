package test;

import java.io.File;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import opencvj.OpenCvJSystem;
import utils.Initializable;


/**
 *
 * @author Kang-Woo Lee
 */
public class TestOpenCvJ {
	private TestOpenCvJ() {
		throw new AssertionError("Should not be invoked!!: class=" + TestOpenCvJ.class.getName());
	}
	
	public static void initialize(File homeDir) throws Exception {
		OpenCvJSystem.initialize(homeDir, null);
//		OpenNI2System.loadSystem(OpenCvJSystem.getOpenCvJLoader(), new File(homeDir, "bin"));
//		PcsdkSystem.loadSystem(OpenCvJSystem.getOpenCvJLoader());
		
        System.out.println("HOME=" + OpenCvJSystem.getHomeDir().getAbsolutePath());
	}
	
	public static void destroyAll(Object... comps) {
		for ( Object comp: comps ) {
			if ( comp != null && comp instanceof Initializable ) {
				try {
					((Initializable)comp).destroy();
				}
				catch ( Exception e ) {
					System.err.println(e);
				}
			}
		}
	}
	
	public static void printUsage(Class<?> mainClass, Options options, int exitCode) {
        new HelpFormatter().printHelp("java " + mainClass.getName(), options);
        System.exit(exitCode);
    }
}
