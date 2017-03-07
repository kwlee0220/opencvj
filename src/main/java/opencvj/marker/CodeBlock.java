package opencvj.marker;

import org.opencv.core.Mat;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
class CodeBlock {
	static final int ROWS = 6;
	static final int COLS = 6;
	static final int MARKER_IMAGE_WIDTH = 60;
	static final int MARKER_IMAGE_HEIGHT = 60;
	
	int[][] m_blocks;
	
	CodeBlock(int[][] blocks) {
		m_blocks = blocks;
	}
	
	static CodeBlock extract(Mat markerImage) {
		byte[] data = new byte[MARKER_IMAGE_WIDTH * MARKER_IMAGE_HEIGHT];
		markerImage.get(0, 0, data);
		
		double[] d = markerImage.get(0, 0);
		
		// 마커 이미지에서 각각의 코드 블록으로 픽셀들을 모두 더한다.
		double[][] gauges = new double[COLS][ROWS];
		for (int y = 0; y < MARKER_IMAGE_HEIGHT; y++) {
			for (int x = 0; x < MARKER_IMAGE_WIDTH; x++) {
				int yi = y/10;
				int xi = x/10;
				
				int v = 0x00ff & data[y*MARKER_IMAGE_WIDTH + x];
				gauges[yi][xi] += v;
			}
		}

		double min_v = 255.; 
		double max_v = 0.;
		
		// 코드 블록의 값을 0 ~ 1 사이의 값으로 정규화 하면서 최대값과 최소값을 찾는다.
		// 하나의 코드 블록에는 100개의 픽셀이 더해지고 한 픽셀의 최대 값은 255이기 때문에
		// 코드 블록을 100*255로 나누어주면 된다.
		for (int y = 0; y < ROWS; y++) {
			for (int x = 0; x < COLS; x++) {
				gauges[y][x] /= 100.*255;

				if (min_v > gauges[y][x]) min_v = gauges[y][x];
				if (max_v < gauges[y][x]) max_v = gauges[y][x];
			}
		}

		// 최대값과 최소값의 중간값을 찾는다.
		double mid_v = (min_v + max_v)/2.;

		// 중간값을 기준으로 검정색에 가까우면 1.을 흰색에 가까우면 0.을 대입한다.
		int[][] codeBlock = new int[COLS][ROWS];
		for (int y = 0; y < ROWS; y++) {
			for (int x = 0; x < COLS; x++) {
				codeBlock[y][x] = (gauges[y][x] < mid_v) ? 1 : 0;
			}
		}
		
		return new CodeBlock(codeBlock);
	}
	
	boolean checkParity() {
		int sum = 0;

		// 테두리가 모두 제대로 있는지 검사한다.
		// 즉, 한 방향의 블럭 수는 6개이고 모서리가 4개이니까 
		// 합이 24개가 되어야 한다.
		for (int i = 0; i < 6; i++) {
			sum += m_blocks[0][i];
			sum += m_blocks[5][i];
			sum += m_blocks[i][0];
			sum += m_blocks[i][5];
		}
		if (sum != 24) return false;

		sum = 0;

		// 체크섬을 검사한다.
		// 테두리를 제외한 내부 블럭의 수는 짝수가 되어야 한다.
		for (int y = 1; y < 5; y++) {
			for (int x = 1; x < 5; x++) {
				sum += (int)m_blocks[y][x];
			}
		}
		return (sum%2 == 0);
	}
	
	int getRotation() {
		if ( m_blocks[1][1] != 0 && m_blocks[1][4]==0
			&& m_blocks[4][4]==0 && m_blocks[4][1]==0 ) {
			return 0;	// 정상
		}
		else if ( m_blocks[1][1] == 0 && m_blocks[1][4] != 0
				&& m_blocks[4][4] == 0 && m_blocks[4][1] == 0 ) {
			return 1;	// 시계방향으로 90도 회전됨
		}
		else if ( m_blocks[1][1] == 0 && m_blocks[1][4] == 0
				&& m_blocks[4][4] != 0 && m_blocks[4][1] == 0 ) {
			return 2; // 시계방향으로 180도 회전됨
		}
		else if ( m_blocks[1][1] == 0 && m_blocks[1][4] == 0
				&& m_blocks[4][4] == 0 && m_blocks[4][1] != 0 ) {
			return 3; // 시계방향으로 270도 회전됨
		}
		else {
			return -1; // 있을수 없는 조합이다. 실패
		}
	}
	
	CodeBlock rotate(int angleIdx) {
		if (angleIdx == 0) {
			return this;
		}

		int rotated[][] = new int[COLS][ROWS];
		for (int y = 0; y < COLS; y++) {
			for (int x = 0; x < ROWS; x++) {
				switch (angleIdx) {
					case 1: rotated[y][x] = m_blocks[x][5-y];	// 반시계 방향으로 90도 회전
						break;
					case 2: rotated[y][x] = m_blocks[5-y][5-x];	// 반시계 방향으로 180도 회전
						break; 
					case 3: rotated[y][x] = m_blocks[5-x][y];	// 반시계 방향으로 270도 회전
						break; 
				}
			}
		}
		
		return new CodeBlock(rotated);
	}
	
	int calcId() {
		int id = 0;
		if (m_blocks[1][2] != 0) id += 1;
		if (m_blocks[1][3] != 0) id += 2;
		if (m_blocks[2][1] != 0) id += 4;
		if (m_blocks[2][2] != 0) id += 8;
		if (m_blocks[2][3] != 0) id += 16;
		if (m_blocks[2][4] != 0) id += 32;
		if (m_blocks[3][1] != 0) id += 64;
		if (m_blocks[3][2] != 0) id += 128;
		if (m_blocks[3][3] != 0) id += 256;
		if (m_blocks[4][2] != 0) id += 512;
		if (m_blocks[4][3] != 0) id += 1024;

		return id;
	}
}