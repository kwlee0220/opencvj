package opencvj.blob;


import org.opencv.core.Mat;



/**
 * 
 * @author Kang-Woo Lee
 */
public interface BackgroundModel extends BackgroundLearnable, AutoCloseable {
	public Mat getBackgroundModel();

}