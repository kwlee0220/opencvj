package opencvj;

import org.opencv.core.Scalar;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public final class OpenCvJ {
	public static final Scalar ALL_0 = Scalar.all(0);
	public static final Scalar ALL_1 = Scalar.all(1);
	public static final Scalar ALL_255 = Scalar.all(255);
	
	public static final Scalar BLACK = new Scalar(0,0,0);
	public static final Scalar WHITE = new Scalar(255,255,255);
	public static final Scalar RED = new Scalar(0, 0, 255);
	public static final Scalar BLUE = new Scalar(255, 0, 0);
	public static final Scalar GREEN = new Scalar(0, 255, 0);
	public static final Scalar YELLOW = new Scalar(0,255,255);
	public static final Scalar ORANGE = new Scalar(0,165,255);
	public static final Scalar OLIVE = new Scalar(0,128,128);
	public static final Scalar GAINSBORO = new Scalar(220,220,220);	// 약한 회색
	
	private OpenCvJ() {
		throw new AssertionError("Should not be called this one: "
								+ OpenCvJ.class);
	}
}