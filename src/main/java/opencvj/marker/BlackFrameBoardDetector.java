package opencvj.marker;

import java.util.ArrayList;
import java.util.List;

import camus.service.DoubleRange;
import camus.service.SizeRange;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;

import opencvj.OpenCvJException;
import opencvj.OpenCvJLoader;
import opencvj.OpenCvJSystem;
import opencvj.OpenCvJUtils;
import opencvj.blob.Blob;
import opencvj.blob.BlobForest;
import opencvj.blob.BlobForest.Node;
import opencvj.blob.ImageThreshold;
import utils.Initializable;
import utils.UninitializedException;
import utils.config.ConfigNode;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class BlackFrameBoardDetector implements BoardDetector, Initializable {
	private static final DoubleRange DEFAULT_SIDE_RANGE = new DoubleRange(0.1f, 0.7f);
	private static final float BLOCK_SIZE_RATIO = (131 / 640.0f);
	
    public static class Params {
    	private DoubleRange sideLengthRatio =DEFAULT_SIDE_RANGE;
    	
    	public Params(ConfigNode config) {
    		sideLengthRatio = OpenCvJUtils.asDoubleRange(config.get("side_ratio_range"),
    														DEFAULT_SIDE_RANGE);
    	}
    }
	
	// properties (BEGIN)
	private volatile OpenCvJLoader m_loader;
	private volatile ConfigNode m_config;
	// properties (END)

	private Params m_params;
	private ImageThreshold m_threshold;
	private Size m_imageSize;
	private SizeRange m_sideLength;
	private SizeRange m_markerArea;
	
	public static final BlackFrameBoardDetector create(OpenCvJLoader loader, ConfigNode config) throws Exception {
		BlackFrameBoardDetector board = new BlackFrameBoardDetector();
		board.setOpenCvJLoader(loader);
		board.setConfig(config);
		board.initialize();
		
		return board;
	}
	
	public BlackFrameBoardDetector() { }

	public final void setOpenCvJLoader(OpenCvJLoader loader) {
		m_loader = loader;
	}
	
	public final void setConfig(ConfigNode config) {
		m_config = config;
	}
	
//	public final void setConfig(String configStr) {
//		m_config = new Config(configStr);
//	}

	@Override
	public void initialize() throws Exception {
		if ( m_loader == null ) {
			throw new UninitializedException("Property 'openCvJLoader' was not specified: class="
											+ getClass().getName());
		}
		if ( m_params == null ) {
			if ( m_config == null ) {
				throw new UninitializedException("Property 'config' was not specified: class="
												+ getClass().getName());
			}
			m_params = new Params(m_config);
		}
		
		m_threshold = OpenCvJSystem.createImageThreshold(m_config.get("threshold"));
	}

	@Override
	public void destroy() throws Exception { }

	@Override
	public Point[] detect(Mat image) {
		Mat mask = new Mat();
		try {
			Point[] corners = findLargest(detect(image, mask));
			if ( corners != null ) {
				corners = OpenCvJUtils.orderCorners(corners);
			}
			return corners;
		}
		finally {
			mask.release();
		}
	}
	
	public List<Point[]> detect(Mat image, Mat mask) {
		if ( image.empty() ) {
			throw new OpenCvJException("image is empty");
		}

		// 이미지 크기를 고려해서, 최소 blob 크기를 설정한다.
		if ( m_imageSize == null || m_imageSize.equals(image.size()) ) {
			Size size = image.size();
			
			DoubleRange ratio = m_params.sideLengthRatio;

			m_imageSize = size;
			m_sideLength = new SizeRange();
			m_sideLength.low = (int)Math.round(Math.min(size.width, size.height) * ratio.low);
			m_sideLength.high = (int)Math.round(Math.max(size.width, size.height) * ratio.high);

			m_markerArea = new SizeRange();
			m_markerArea.low = (int)Math.round(size.area() * ratio.low*ratio.low);
			m_markerArea.high = (int)Math.round(size.area() * ratio.high*ratio.high);
		}

//		Mat gray;
//		Imgproc.GaussianBlur(gray, gray, new Size(3,3), 0, 0);
		m_threshold.detect(image, mask);

//		MatConvas convas = new MatConvas(mask.clone());
//		Imgproc.cvtColor(convas.getMat(), convas.getMat(), Imgproc.COLOR_GRAY2BGR);
//		WindowManager.show("board", convas);
//		WindowManager.show("org", image);
		
		List<Point[]> markers = new ArrayList<Point[]>();
		List<BlobForest.Node> toDoList = new ArrayList<BlobForest.Node>();

		Mat cloned = mask.clone();
		BlobForest forest = new BlobForest(cloned);
		cloned.release();

		appendToList(toDoList, forest, forest.getTopLevels(), m_markerArea.low);
		while ( !toDoList.isEmpty() ) {
			BlobForest.Node node = toDoList.remove(0);
			
			if ( node.getLevel() % 2 == 0 ) {
				Point[] corners = checkBoardShape(mask, node.getBlob());
				if ( corners != null ) {
					markers.add(corners);
					
//					convas.drawContour(corners, OpenCvJ.RED, 2);
//					WindowManager.show("board", convas);
				}
				else {
					appendToList(toDoList, forest, node.getChildren(), m_markerArea.low);
				}
			}
			else {
				// 홀수 level의 노드는 marker가 아니므로, 자신의 자식 노드를 toDoList에 추가한다.
				appendToList(toDoList, forest, node.getChildren(), m_markerArea.low);
			}
		}

		return markers; 
	}

	private static final float APPROX_EPSILON = 0.05f;
	private static final Size WIN_SIZE = new Size(2,2);
	private static final Size ZERO_ZONE = new Size(-1,-1);
	private static final TermCriteria TERM_CRIT
								= new TermCriteria(TermCriteria.EPS+TermCriteria.COUNT, 10, 0.01);
	
	private Point[] checkBoardShape(Mat image, Blob blob) {
		// 바운딩 박스를 찾는 이유는 컨투어의 대략적인 크기를 알기 위해서다.
		// 크기에 따라 컨투어를 approximation 하는 정밀도를 조정한다.
		// 여기서는 대략 10%정도의 정밀도로 조정한다. (d*approx_param 부분)
		Size blobSize = blob.minAreaBox().size;

		if ( Math.min(blobSize.width, blobSize.height) >= m_sideLength.low
			&& Math.max(blobSize.width, blobSize.height) <= m_sideLength.high ) {
			double d = Math.sqrt(blob.area());
			Blob approx = blob.approximate(d*APPROX_EPSILON);
			if ( approx.npoints() == 4 ) {
				Point[] corners = approx.contour();
				
				Point center = OpenCvJUtils.calcCrossPoint(corners[0], corners[2], corners[1], corners[3]);
				if ( center != null ) {
					MatOfPoint2f mop = new MatOfPoint2f(corners);
					try {
						Imgproc.cornerSubPix(image, mop, WIN_SIZE, ZERO_ZONE, TERM_CRIT);
						return mop.toArray();
					}
					finally {
						mop.release();
					}
				}
			}
		}
		
		return null;
		
	}
	
	public void appendToList(List<Node> list, BlobForest forest, List<Node> nodes, int minSize) {
		for ( Node node: nodes ) {
			if ( node.getBlob().area() >= minSize ) {
				list.add(node);
			}
		}
	}
	
	private Point[] findLargest(List<Point[]> boards) {
		Point[] largest = null;
		double largestArea = -1;
		for ( Point[] board: boards ) {
			double area = new Blob(board).area();
			if ( area > largestArea ) {
				largest = board;
				largestArea = area;
			}
		}
		
		return largest;
	}
}
