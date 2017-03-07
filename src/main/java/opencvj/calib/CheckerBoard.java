package opencvj.calib;

import java.io.File;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import opencvj.OpenCvJException;
import opencvj.OpenCvJUtils;
import utils.config.ConfigNode;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class CheckerBoard {
	private static final TermCriteria CRIT
								= new TermCriteria(TermCriteria.EPS+TermCriteria.COUNT, 30, 0.1);
	private static final Size SEARCH_WIN_SIZE = new Size(11, 11);
	private static final Size ZERO_ZONE = new Size(-1, -1);
	
	private Size m_patternSize;
	private int m_cellLength;
	private Point3[] m_corners;
	private File m_imageFile;
	
	public static CheckerBoard create(ConfigNode config) {
		Size patternSize = OpenCvJUtils.asSize(config.get("pattern_size"));
		int cellSize = config.get("cell_length").asInt();
		
		CheckerBoard board = new CheckerBoard(patternSize, cellSize);
		board.setImageFile(config.get("image_filepath").asFile());
		
		return board;
	}
	
	public CheckerBoard(Size patternSize, int cellSize) {
		m_patternSize = patternSize;
		m_cellLength = cellSize;
		m_corners = new Point3[(int)patternSize.area()];
		int idx = -1;
		for ( int i=0; i < m_patternSize.height; ++i ) {
			for ( int j=0; j < m_patternSize.width; ++j ) {
				m_corners[++idx] = new Point3(j*cellSize, j*cellSize, 0);
			}
		}
	}
	
	public Size getPatternSize() {
		return m_patternSize;
	}
	
	public int getCellLength() {
		return m_cellLength;
	}
	
	public boolean fastCheck(Mat image, Point[] corners) {
		MatOfPoint2f cornersMat = new MatOfPoint2f(corners);
		try {
			return Calib3d.findChessboardCorners(image, m_patternSize, cornersMat,
												Calib3d.CALIB_CB_FAST_CHECK);
		}
		finally {
			cornersMat.release();
		}
	}
	
	public Point[] findCorners(Mat image) {
		return findCorners(image, Calib3d.CALIB_CB_ADAPTIVE_THRESH/*+Calib3d.CALIB_CB_NORMALIZE_IMAGE*/);
	}
	
	public Point[] findCorners(Mat image, int flags) {
		Mat grayImage = new Mat();
		MatOfPoint2f cornersMat = new MatOfPoint2f();

		try {
			boolean done = Calib3d.findChessboardCorners(image, m_patternSize, cornersMat, flags);
			if ( done ) {
				Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY); 
				Imgproc.cornerSubPix(grayImage, cornersMat, SEARCH_WIN_SIZE, ZERO_ZONE, CRIT);
				
				return cornersMat.toArray();
			}
			else {
				return null;
			}
		}
		finally {
			grayImage.release();
			cornersMat.release();
		}
	}
	
	public Mat readCheckerImage() {
		Mat image = Highgui.imread(m_imageFile.getAbsolutePath());
		if ( image.dataAddr() == 0 ) {
			throw new OpenCvJException(String.format("cannot read the checker board image: file='%s'",
													m_imageFile.getAbsolutePath()));
		}
		
		return image;
	}
	
	public void setImageFile(File imageFile) {
		m_imageFile = imageFile;
	}
}
