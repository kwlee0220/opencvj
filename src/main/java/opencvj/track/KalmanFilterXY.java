package opencvj.track;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

import opencvj.Mats;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class KalmanFilterXY implements AutoCloseable {
	public static final float DEFAULT_PROCESS_NOISE_COV = 1e-4f;
	public static final float DEFAULT_MEASUREMENT_NOISE_COV = 1e-1f;
	private static final float[] DATA_A = new float[] {
		1, 0, 1, 0,
		0, 1, 0, 1,
		0, 0, 1, 0,
		0, 0, 0, 1
	};

	private static final Mat A = new Mat(new Size(4,4), CvType.CV_32FC1); // transition matrix (4x4)
	static {
		A.put(0, 0, DATA_A);
	}

	private Mat x;		// state estimation (4x1)
	private Mat xPre;	// state prediction (4x1)
	private Mat H;		// measurement (2x4)
	private Mat P;		// error covariance post (4x4)
	private Mat PPre;	// error covariance prediction (4x4)
	private Mat Q;		// process noise(w) covariance (4x4)
	private Mat R;		// measurement noise(v) covariance (2x2)
	
	public KalmanFilterXY() {
		// 추정 상태 행렬 생성
		x = new Mat(4, 1, CvType.CV_32FC1);
		
		// 예측 상태 행렬 생성
		xPre = new Mat(4, 1, CvType.CV_32FC1);
		
		// 측정값 행렬 생성
		H = new Mat(2, 4, CvType.CV_32FC1);
		Core.setIdentity(H);
		
		// 오차 공분산 예측 행렬
		P = new Mat(4, 4, CvType.CV_32FC1);
		Core.setIdentity(P, new Scalar(0.1));
		PPre = new Mat();
		
		// 잡음 공분산 대각 행렬
		Q = new Mat(4, 4, CvType.CV_32FC1);
		Core.setIdentity(Q, Scalar.all(DEFAULT_PROCESS_NOISE_COV));	// 따라오는 속도,클수록 빠름
		
		// 측정 잡음 공분산 대각 
		R = new Mat(2, 2, CvType.CV_32FC1);
		Core.setIdentity(R, Scalar.all(DEFAULT_MEASUREMENT_NOISE_COV));	// 따라오는 속도, 클수록 느림
		
		x.put(0, 0, new float[]{0, 0, 0, 0});
	}

	@Override
	public void close() {
		Mats.releaseAll(x, xPre, H, P, PPre, Q, R);
	}
	
	public void setProcessNoiseCov(float cov) {
		Core.setIdentity(Q, Scalar.all(cov));	// 따라오는 속도,클수록 빠름
	}
	
	public void setMeasurementNoiseCov(float cov) {
		Core.setIdentity(R, Scalar.all(cov));	// 따라오는 속도, 작을수록 빠름
	}
	
	public void setInitial(Point pos) {
		x.put(0, 0, new float[]{(float)pos.x, (float)pos.y, 0, 0});
	}
	
	public Point getPointEstimated() {
		return getPoint(x);
	}
	
	public Point getPointPredicted() {
		return getPoint(xPre);
	}
	
	public Point predict() {
		product(A, x, xPre);
		product3AndAdd(A, P, A, Q, PPre, Core.GEMM_3_T);
		
		return getPoint(xPre);
	}
	
	public Point correct(Point pos) {
		Mat tmp = new Mat();
		Mat K = new Mat();
		Mat z = new Mat(2, 1, CvType.CV_32FC1);
		
		try {
			// 칼만 이득(K) 계산
			product3AndAdd(H, PPre, H, R, tmp, Core.GEMM_3_T);
			Mat tmp2 = tmp.inv();
			tmp.release();
			tmp = tmp2;
			product3(PPre, H, tmp, K, Core.GEMM_2_T);
			
			// 측정 값 행령(z) 설정
			z.put(0, 0, new float[]{(float)pos.x, (float)pos.y});
			
			// 상태 추정 (m_x)
			product(H, xPre, tmp);
			Core.subtract(z, tmp, tmp);
			product(K, tmp, tmp);
			Core.add(xPre, tmp, x);
			
			// 오차 공분산(m_P) 보정
			product3(K, H, PPre, tmp, 0);
			Core.subtract(PPre, tmp, P);

			return getPoint(x);
		}
		finally {
			Mats.releaseAll(tmp, K, z);
		}
	}
	
	private static final Mat NIL = new Mat();
	private void product(Mat a, Mat b, Mat result) {
		Core.gemm(a, b, 1, NIL, 0, result);
	}
	private void product(Mat a, Mat b, Mat result, int flags) {
		Core.gemm(a, b, 1, NIL, 0, result, flags);
	}
	private void product3(Mat a, Mat b, Mat c, Mat result, int flags) {
		Mat tmp = new Mat();
		try {
			int tmpFlags = flags & (Core.GEMM_1_T|Core.GEMM_2_T);
			Core.gemm(a, b, 1, NIL, 0, tmp, tmpFlags);
			tmpFlags = flags << 1;
			Core.gemm(tmp, c, 1, NIL, 0, result, tmpFlags);
		}
		finally {
			tmp.release();
		}
	}
	private void productAndAdd(Mat a, Mat b, Mat c, Mat result, int flags) {
		Core.gemm(a, b, 1, c, 1, result, flags);
	}
	private void product3AndAdd(Mat a, Mat b, Mat c, Mat d, Mat result, int flags) {
		Mat tmp = new Mat();
		try {
			int tmpFlags = flags & (Core.GEMM_1_T|Core.GEMM_2_T);
			Core.gemm(a, b, 1, NIL, 0, tmp, tmpFlags);
			tmpFlags = flags >> 1;
			Core.gemm(tmp, c, 1, d, 1, result, tmpFlags);
		}
		finally {
			tmp.release();
		}
	}
	
	private Point getPoint(Mat stateMat) {
		float[] data = new float[4];
		stateMat.get(0, 0, data);
		return new Point(data[0], data[1]);
	}
}
