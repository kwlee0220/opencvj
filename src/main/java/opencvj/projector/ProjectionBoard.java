package opencvj.projector;

import java.io.Closeable;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import opencvj.OpenCvJException;
import opencvj.OpenCvJLoader;
import opencvj.OpenCvJUtils;
import opencvj.marker.BoardDetector;
import opencvj.marker.BoardTracker;
import opencvj.misc.PerspectiveTransform;
import opencvj.track.MAPointTrackerFactory;
import opencvj.track.MultiPointTracker;
import opencvj.track.PointTrackerFactory;
import utils.config.ConfigNode;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class ProjectionBoard implements Closeable {
	private static final float BOARD_TRACK_ALPHA = 0.5f;
	private static final int BOARD_DETECT_IGNORE_COUNT = 0;
	private static final int BOARD_LOST_IGNORE_COUNT = 2;
	private static final long BOARD_LOST_IGNORE_MILLIS = Long.MAX_VALUE;
	
	// properties (BEGIN)
	private volatile CameraProjectorComposite m_cpc;
	private volatile BoardTracker m_tracker;
	// properties (END)

	private Size m_screenSize;
	private Point[] m_boardCorners;
	
	private Point[] m_boardCornersInImage;	// 가장 마지막으로 검출된 보드의 코너점 (카메라 좌표 기준)
	private PerspectiveTransform m_screenToBoard =null;
	private PerspectiveTransform m_boardToScreen =null;
	
	public static ProjectionBoard create(OpenCvJLoader loader, CameraProjectorComposite cpc,
										ConfigNode config) throws Exception {
		return new ProjectionBoard(cpc, BoardTracker.create(loader, config));
	}
	
	public ProjectionBoard(CameraProjectorComposite cpc, BoardTracker tracker) {
		m_cpc = cpc;
		m_tracker = tracker;
		
		m_screenSize = cpc.getProjector().getSize();
	}
	
	public ProjectionBoard(CameraProjectorComposite cpc, BoardDetector board) {
		this(cpc, createBoardTracker(board));
	}

	@Override
	public void close() {
		if ( m_boardToScreen != null ) {
			m_boardToScreen.close();
			m_boardToScreen = null;
		}
		if ( m_screenToBoard != null ) {
			m_screenToBoard.close();
			m_screenToBoard = null;
		}
	}
	
	/**
	 * 대상 보드 이미지의 크기를 설정한다.
	 * 
	 * @param size	보드 이미지의 크기
	 */
	public void setBoardSize(Size size) {
		m_boardCorners = OpenCvJUtils.getCorners(size);
		
		if ( m_boardToScreen != null ) {
			m_boardToScreen.close();
			m_boardToScreen = null;
		}
		if ( m_screenToBoard != null ) {
			m_screenToBoard.close();
			m_screenToBoard = null;
		}
	}
	
	/**
	 * 가장 마지막으로 검출된 영상 내 보드 코너점 좌표를 반환한다.
	 * 
	 * @return	영상 내 보드 코너점 좌표
	 */
	public Point[] getBoardCornersInImage() {
		return m_boardCornersInImage;
	}
	
	/**
	 * 영상 내 보드 코너점을 설정한다.
	 * 
	 * @param cornersInImage	영상 내 보드 코너점
	 */
	public Point[] updateBoardCorners(Point[] cornersInImage) {
		cornersInImage = m_tracker.track(cornersInImage);
		
		// tracker를 사용하면 처음 몇번의 코너점이 무시될 수 있으므로
		// track 결과의 코너점 좌표가 획득된 경우 갱신한다.
		if ( cornersInImage != null ) {
			boardCornersUpdated(cornersInImage);
		}
		
		return cornersInImage;
	}

	/**
	 * 영상에서 보드 코너점을 검출하여 설정한다.
	 * 
	 * @param image	보드를 검출한 대상 이미지
	 */
	public boolean updateBoardCorners(Mat image) {
		Point[] corners = m_tracker.track(image);
		
		// tracker를 사용하면 처음 몇번의 코너점이 무시될 수 있으므로
		// track 결과의 코너점 좌표가 획득된 경우 갱신한다.
		if ( corners != null ) {
			boardCornersUpdated(corners);
			return true;
		}
		else {
			return false;
		}
	}
	
	/**
	 * 영상 내에서 보드 영역에 해당하는 부분을 사각형 영상으로 추출한다.
	 * 
	 * @param image	보드 영역을 뽑을 영상.
	 * @param destImage	추출된 영상을 저장할 mat 객체.
	 * @param destSize	추출된 영상의 크기.
	 */
	public void warpBoardImage(Mat image, Mat destImage, Size destSize) {
		OpenCvJUtils.warpRectangleImage(image, m_boardCornersInImage, destImage, destSize);
	}
	
	/**
	 * 보드 좌표계에서 스크린 좌표계로의 변환기를 반환한다.
	 * 
	 * @return 좌표계 변환기
	 */
	public PerspectiveTransform getBoardToScreen() {
		if ( m_boardToScreen == null ) {
			if ( m_boardCorners == null ) {
				throw new OpenCvJException("source board corners has not been specified");
			}
			if ( m_boardCornersInImage == null ) {
				throw new OpenCvJException("board corners are unknown");
			}

			// 보드 좌표계에서 카메라의 보드 좌표계로의 변환 계산 (T1)
			PerspectiveTransform trans = PerspectiveTransform.createPerspectiveTransform(
																		m_boardCorners,
																		m_boardCornersInImage);
			try {
				// 보드 좌표계 -> 카메라 좌표계 -> 스크린 좌표계
				m_boardToScreen = PerspectiveTransform.product(m_cpc.getCameraToProjectorTransform(),
																trans);
			}
			finally {
				trans.close();
			}
		}
		
		return m_boardToScreen;
	}
	
	/**
	 * 스크린 좌표에서 보드 좌표로의 변환기를 반환한다.
	 * 
	 * @return 좌표계 변환기
	 */
	public PerspectiveTransform getScreenToBoard() {
		if ( m_screenToBoard == null ) {
			if ( m_boardCornersInImage == null ) {
				return null;
//				throw new OpenCvJException("board corners are unknown");
			}
			
			Point[] screenCorners = OpenCvJUtils.getCorners(m_screenSize);
			Point[] boardCornersInImage = m_cpc.getParams().flipCode.reorder(m_boardCornersInImage);
			Point[] cornersInScreenCoords = m_cpc.toProjectorCoordinates(boardCornersInImage);

			m_screenToBoard = PerspectiveTransform.createPerspectiveTransform(
															screenCorners, cornersInScreenCoords);
		}
		
		return m_screenToBoard;
	}
	
	/**
	 * 주어진 이미지를 검출된 보드에 출력시킨다.
	 * 
	 * @param image	출력시킬 이미지.
	 */
	public void projectOntoBoard(Mat image) {
		Mat convas = new Mat();
		Mat src = image;

		try {
			// 입력 영상 전체가 board 상에 출력되어야 하기 때문에, 먼저 입력 영상을 screen 크기로
			// warpping 한 뒤, screen 전체 영상을 board 내에 삽입되도록 다시 warpping 시킨다.
	
			// 1. 입력 영상을 screen 크기로 warpping 시킨다.
			if ( image.size() != m_screenSize ) {
				Imgproc.resize(src, convas, m_screenSize);
				src = convas;
			}
	
			// 2. screen 전체 영상이 board 상에 맞게 출력되도록 보정한다.
			PerspectiveTransform transScreenToBoard = getScreenToBoard();
			if ( transScreenToBoard != null ) {
				transScreenToBoard.perform(src, convas, m_screenSize);
		
				// 3. 프로젝터에 출력시킨다.
				m_cpc.getProjector().show(convas);
			}
			else {
				m_cpc.getProjector().show(src);
			}
		}
		finally {
			convas.release();
		}
	}
	
	private static BoardTracker createBoardTracker(BoardDetector board) {
		PointTrackerFactory fact = new MAPointTrackerFactory(BOARD_TRACK_ALPHA,
												BOARD_DETECT_IGNORE_COUNT, BOARD_LOST_IGNORE_COUNT,
												BOARD_LOST_IGNORE_MILLIS);
		MultiPointTracker ptTracker = new MultiPointTracker(fact);
		return new BoardTracker(board, ptTracker);
	}
	
	private void boardCornersUpdated(Point[] corners) {
		m_boardCornersInImage = corners;
		
		if ( m_boardToScreen != null ) {
			m_boardToScreen.close();
			m_boardToScreen = null;
		}
		if ( m_screenToBoard != null ) {
			m_screenToBoard.close();
			m_screenToBoard = null;
		}
	}
}
