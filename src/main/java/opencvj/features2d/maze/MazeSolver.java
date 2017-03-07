package opencvj.features2d.maze;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import opencvj.MatConvas;
import opencvj.OpenCvJ;
import opencvj.blob.Blob;
import opencvj.features2d.Template;
import opencvj.marker.BoardDetector;
import opencvj.projector.CameraProjectorComposite;
import opencvj.projector.ProjectionBoard;
import opencvj.track.BlobTrackable;
import opencvj.track.RegionalObjectTracker;
import opencvj.track.TrackState;
import utils.config.ConfigNode;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class MazeSolver implements AutoCloseable {
    private static final Logger s_logger = Logger.getLogger("MAZE");
    
	public static class Params {
		public long tempLostTimeout = 5000;
		
		public Params(ConfigNode config) {
			tempLostTimeout = config.get("temp_lost_timeout").asDuration("5s");
		}
	}
	// properties (BEGIN)
	private volatile CameraProjectorComposite m_cpc;
	// properties (END)
	
	private Params m_params;
	private MazeMatcher m_matcher;
	private RegionalObjectTracker.Params m_trackParams;
	private RegionalObjectTracker m_tracker;
	private ProjectionBoard m_prjBoard;
	
	private TrackState m_state;
	private MazeInfo m_maze;
	private long m_lostStarted;
	private MatConvas m_convas;
	
	public static MazeSolver create(CameraProjectorComposite cpc, ConfigNode config) throws Exception {
		MazeMatcher matcher = MazeMatcher.create(config);
		RegionalObjectTracker.Params trackerParams = new RegionalObjectTracker.Params(config);
		Params params = new Params(config.get("tracker"));
		return new MazeSolver(cpc, matcher, params, trackerParams);
	}
	
	public MazeSolver(CameraProjectorComposite cpc, MazeMatcher matcher, Params params,
						RegionalObjectTracker.Params trackerParams) {
		m_params = params;
		m_cpc = cpc;
		m_matcher = matcher;
		m_trackParams = trackerParams;
		m_maze = null;
		m_state = TrackState.LOST;
		m_lostStarted = -1;
		
		m_prjBoard = new ProjectionBoard(m_cpc, m_board);
		m_convas = new MatConvas(m_cpc.getProjector().getSize());
	}

	@Override
	public void close() {
		m_convas.close();
		m_prjBoard.close();
	}
	
	public Point[] getMazeCornersInImage() {
		return m_prjBoard.getBoardCornersInImage();
	}
	
	public MazeInfo getMazeInfo() {
		return m_maze;
	}
	
	public void process(Mat image) throws IOException {
		m_prjBoard.updateBoardCorners(image);

		m_convas.clear();
		if ( m_state == TrackState.TRACKED || m_state == TrackState.TEMP_LOST ) {
			Point[] solutionPath = m_prjBoard.getBoardToScreen().perform(m_maze.m_solutionPath);
			m_convas.drawOpenContour(solutionPath, OpenCvJ.RED, 7);
		}
		m_cpc.getProjector().show(m_convas.getMat());
	}
	
	private final BoardDetector m_board = new BoardDetector() {
		@Override
		public Point[] detect(Mat image) {
			try {
				detectMaze(image);
				if ( m_state == TrackState.TRACKED ) {
					return m_maze.m_corners;
				}
			}
			catch ( IOException e ) {
				s_logger.error(e);
			}
			
			return null;
		}
	};
	
	private void detectMaze(Mat image) throws IOException {
		switch ( m_state ) {
			case TRACKED:
			case TEMP_LOST:
				m_tracker.track(image);
				break;
			case LOST:
				m_maze = m_matcher.match(image);
				if ( m_maze != null ) {
					BlobTrackable target = new BlobTrackable(m_maze.m_id, new Blob(m_maze.m_corners));
					m_tracker = new RegionalObjectTracker(m_matcher.getObjectTemplateStore(),
																		target, m_trackParams);
					
					Template tmplt = m_matcher.getObjectTemplateStore().getTemplate(m_maze.m_id);
					m_prjBoard.setBoardSize(tmplt.imageSize);
				}
				break;
		}
		if ( m_tracker == null ) {
			return;
		}
		
		switch ( m_tracker.getTrackState() ) {
			case TRACKED:
				m_lostStarted = -1;
				m_maze.m_corners = m_tracker.getTarget().m_location.contour();
				m_state = TrackState.TRACKED;
				break;
			case TEMP_LOST:
			case LOST:
				if ( m_lostStarted < 0 ) {
					m_lostStarted = System.currentTimeMillis();
				}
				long elapsed = System.currentTimeMillis() - m_lostStarted;
				if ( elapsed > m_params.tempLostTimeout ) {
					m_state = TrackState.LOST;
				}
				else {
					m_state = TrackState.TEMP_LOST;
				}
				break;
		}
	}
}
