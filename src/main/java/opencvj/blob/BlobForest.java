package opencvj.blob;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class BlobForest {
	private Node[] m_nodes;
	private final int[] m_hierarchy;
	private List<Node> m_topLevelNodes = new ArrayList<Node>();
	private final Node NIL_NODE = new Node(null);
	
	public class Node {
		private int m_index = -1;
		private int m_level = -1;
		private List<Node> m_children = new ArrayList<Node>();
		private Blob m_blob;
		
		Node(Blob blob) {
			m_blob = blob;
		}
		
		public final int getLevel() {
			return m_level;
		}
		
		public final Blob getBlob() {
			return m_blob;
		}
		
		public final List<Node> getChildren() {
			return m_children;
		}
		
		public Node getParent() {
			int parentIdx = getParentIndex(m_index);
			return ( parentIdx >= 0 ) ? m_nodes[parentIdx] : NIL_NODE;
		}
	}
	
	public BlobForest(Mat bwImage) {
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Mat hierarchy = new Mat();
		
		Imgproc.findContours(bwImage, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
		m_nodes = new Node[contours.size()];
		for ( int i =0; i < m_nodes.length; ++i ) {
			MatOfPoint mop = contours.get(i);
			m_nodes[i] = new Node(new Blob(mop.toArray()));
			mop.release();
		}
		
		m_hierarchy = new int[(int)hierarchy.total() * hierarchy.channels()];
		hierarchy.get(0, 0, m_hierarchy);
		hierarchy.release();

		for ( int i =0; i < m_nodes.length; ++i ) {
			scanUp(i);
		}
	}
	
	public List<Node> getTopLevels() {
		return m_topLevelNodes;
	}
	
	public int countBlobs() {
		return m_nodes.length;
	}
	
	public Node getNode(int idx) {
		return m_nodes[idx];
	}
	
	public Blob getBlob(int idx) {
		return m_nodes[idx].getBlob();
	}
	
	private int getNextSiblingIndex(int idx) {
		return m_hierarchy[idx*4];
	}
	
	private int getPreviousSiblingIndex(int idx) {
		return m_hierarchy[idx*4 + 1];
	}
	
	private int getFirstChildIndex(int idx) {
		return m_hierarchy[idx*4 + 2];
	}
	
	private int getParentIndex(int idx) {
		return m_hierarchy[idx*4 + 3];
	}
	
	private void scanUp(int nodeIdx) {
		final Node node = m_nodes[nodeIdx];

		// 이미 거쳐간 노드인 경우 scan하지 않는다.
		if ( node.m_index >= 0 ) {
			return;
		}
		node.m_index = nodeIdx;
		
		int parent_idx = getParentIndex(nodeIdx);
		if ( parent_idx < 0 ) {
			node.m_level = 0;
			m_topLevelNodes.add(node);
		}
		else {
			final Node parent = m_nodes[parent_idx];
			if ( parent.m_level < 0 ) {
				scanUp(parent_idx);
			}
			parent.m_children.add(m_nodes[node.m_index]);

			node.m_level = parent.m_level + 1;
		} 
	}
}
