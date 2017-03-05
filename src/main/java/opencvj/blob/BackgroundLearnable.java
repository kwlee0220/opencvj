package opencvj.blob;



import org.opencv.core.Mat;



/**
 * 
 * @author Kang-Woo Lee
 */
public interface BackgroundLearnable {
	public void updateBackgroundModel(Mat image);
	public void clearBackground();

	public Mat getBackground();
}