package opencvj.camera;

import java.util.concurrent.TimeUnit;

import org.opencv.core.Mat;
import org.opencv.core.Size;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface OpenCvJCamera extends AutoCloseable {
	public void open();
	
	public Size getSize();
	
	public void capture(Mat image);
	
	public default void dropFrames(int count) {
		Mat image = new Mat();
		try {
			for ( int i =0; i < count; ++i ) {
				capture(image);
			}
		}
		finally {
			image.release();
		}
	}
	
	public default void dropFrames(long period, TimeUnit tu) {
		Mat image = new Mat();
		try {
			long periodMillis = tu.toMillis(period);
			long started = System.currentTimeMillis();
			while ( true ) {
				capture(image);
				
				if ( (System.currentTimeMillis()-started) > periodMillis ) {
					break;
				}
			}
		}
		finally {
			image.release();
		}
	}
}
