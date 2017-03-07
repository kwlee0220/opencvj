package test.blob;

import java.io.File;

import camus.service.image.Color;

import org.apache.commons.cli.Option;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import opencvj.OpenCvJSystem;
import opencvj.OpenCvView;
import opencvj.OpenCvViewManager;
import opencvj.blob.BackgroundModel;
import opencvj.camera.OpenCvJCamera;
import test.TestOpenCvJ;
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
public class TestBackgroundModel {
	public static final void main(String[] args) throws Exception {
    	Log4jConfigurator.configure("log4j.properties");
    	
    	CommandLineParser parser = new CommandLineParser("test_background_model");
    	parser.addArgOption("home", "directory", "home directory");
    	parser.addArgOption("camera", "config path", "target camera config path");
    	parser.addArgOption("bgmodel", "config path", "background model config path");
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

		TestOpenCvJ.initialize(homeDir);
        
//		ConfigNode cameraConfig = OpenCvJSystem.getConfigNode(cl.getOptionValue("camera", "xtion.depth"));
//		ConfigNode bgModelConfig = OpenCvJSystem.getConfigNode(cl.getOptionValue("bgmodel", "depth_bgmodel"));
		ConfigNode cameraConfig = OpenCvJSystem.getConfigNode(cl.getOptionValue("camera", "highgui"));  
		ConfigNode bgModelConfig = OpenCvJSystem.getConfigNode(cl.getOptionValue("bgmodel", "color_bgmodel")); 
        
        // creates target test object and dependent ones
        //
        OpenCvJCamera camera = OpenCvJSystem.createOpenCvJCamera(cameraConfig);
        BackgroundModel bgModel = OpenCvJSystem.getBackgroundModel(bgModelConfig);
    	
		Mat image = new Mat();

		camera.open();
    	try {
    		camera.dropFrames(10);
        	
			FramePerSecondMeasure captureFps = new FramePerSecondMeasure(0.01);
			FramePerSecondMeasure updateFps = new FramePerSecondMeasure(0.01);
			
			OpenCvView window = OpenCvViewManager.getView("bgmodel", camera.getSize(), true, false);
	    	while ( window.getVisible() ) {
				captureFps.startFrame();
	    		camera.capture(image);
				captureFps.stopFrame();

	    		updateFps.startFrame();
	    		bgModel.updateBackgroundModel(image);
	    		updateFps.stopFrame();
	    		
				window.draw(bgModel.getBackground());
	    		window.draw(String.format("fps: capture=%.0f update=%.0f",
										captureFps.getFps(), updateFps.getFps()),
										new Point(10, 17), 17, Color.GREEN);
	    		window.updateView();
	    	}
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
