package opencvj;



/**
 * <code>OpenCvJException</code>는 OpenCvJ 관련 예외 객체를 정의한다.
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class OpenCvJException extends RuntimeException {
	private static final long serialVersionUID = 6490611316947789701L;

	public OpenCvJException(String msg) {
		super(msg);
	}
}
