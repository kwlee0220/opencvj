package opencvj.features2d;

import java.io.File;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.highgui.Highgui;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class ImageEntry {
	/** DB 등록 물체 영상 경로명. 등록된 물체의 영상이 없는 경우는 null이 된다. */
	public String id;
	/** 해당 이미지의 파일 */
	public File imageFile;
	/** DB 등록 영상에서의 물체의 모서리 좌표 값들 */
	public Point[] corners;
	
	public Mat loadImage() {
		return Highgui.imread(imageFile.getAbsolutePath());
	}
}