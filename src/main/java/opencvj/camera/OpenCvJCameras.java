package opencvj.camera;

import org.opencv.core.Mat;
import org.opencv.core.Size;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class OpenCvJCameras {
	public static OpenCvJCamera newFlipCamera(final OpenCvJCamera camera, final FlipCode flipCode) {
		return new OpenCvJCamera() {
			@Override
			public void open() {
				camera.open();
			}
	
			@Override
			public void close() throws Exception {
				camera.close();
			}
	
			@Override
			public Size getSize() {
				return camera.getSize();
			}
	
			@Override
			public void capture(Mat image) {
				camera.capture(image);
				flipCode.flip(image, image);
			}
		};
	}
	
	public static OpenCvJDepthCamera newFlipCamera(final OpenCvJDepthCamera camera,
													final FlipCode flipCode) {
		return new OpenCvJDepthCamera() {
			@Override
			public void open() {
				camera.open();
			}

			@Override
			public void close() throws Exception {
				camera.close();
			}

			@Override
			public Size getSize() {
				return camera.getSize();
			}

			@Override
			public void capture(Mat image) {
				camera.capture(image);
				flipCode.flip(image, image);
			}
		};
	}
}
