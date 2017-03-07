package test.camera;

import java.io.File;

import camus.service.image.Color;

import org.apache.commons.cli.Option;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import opencvj.OpenCvJSystem;
import opencvj.OpenCvView;
import opencvj.OpenCvViewManager;
import opencvj.camera.OpenCvJCamera;
import utils.CommandLine;
import utils.CommandLineParser;
import utils.FramePerSecondMeasure;
import utils.Initializable;
import utils.Log4jConfigurator;
import utils.config.ConfigNode;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class TestBasicOpenCvJCamera {
	public static final void main(String[] args) throws Exception {
    	Log4jConfigurator.configure("log4j.properties");

    	CommandLineParser parser = new CommandLineParser("test_opencv_camera ");
    	parser.addArgOption("home", "directory", "home directory");
    	parser.addArgOption("camera", "config path", "target camera config path");
    	parser.addOption(new Option("h", "usage help"));
    	CommandLine cl = parser.parseArgs(args);
	    
	    if ( cl.hasOption("h") ) {
	    	cl.exitWithUsage(0);
	    }

	    String homeDirPath = cl.getOptionValue("home", ".");
	    File homeDir = new File(homeDirPath).getCanonicalFile();
        if ( !homeDir.isDirectory() ) {
            System.err.println("Invalid home directory: path=" + homeDirPath);
            cl.exitWithUsage(-1);
        }

		OpenCvJSystem.initialize(homeDir, null);
        
        ConfigNode config = OpenCvJSystem.getConfigNode(cl.getOptionValue("camera", "highgui")); 
        
        // creates target test object and dependent ones
        //
		OpenCvJCamera camera = OpenCvJSystem.createOpenCvJCamera(config);

		Mat image = new Mat();

		camera.open();
		try {
			FramePerSecondMeasure fpsMeasure = new FramePerSecondMeasure(0.01);

			OpenCvView window = OpenCvViewManager.getView("camera", camera.getSize(), true, false);
			while ( window.getVisible() ) {
				fpsMeasure.startFrame();
				camera.capture(image);
				fpsMeasure.stopFrame();
				
				window.draw(image);
				window.draw(String.format("fps=%.0f", fpsMeasure.getFps()),
									new Point(10, 17), 17, Color.GREEN);
				window.updateView();
			}
			System.out.println(String.format("fps=%.1f", fpsMeasure.getFps()));
		}
		finally {
			image.release();

			if ( camera instanceof Initializable ) {
				((Initializable)camera).destroy();
			}
			
			OpenCvJSystem.shutdown();
		}
	}
}