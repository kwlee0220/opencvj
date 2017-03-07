package opencvj.features2d.maze;

import org.opencv.core.Point;

import opencvj.features2d.ImageEntry;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public final class MazeInfo {
	public String m_id;
	public Point[] m_corners;
	public Point[] m_solutionPath;
	public ImageEntry m_imageEntry;
}
