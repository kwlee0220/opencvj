package opencvj;

import java.awt.image.BufferedImage;
import java.io.IOException;

import camus.service.camera.ImageProxy;
import camus.service.camera.ImageType;
import camus.service.vision.Image;
import camus.service.vision.ImageEncoding;
import camus.service.vision.ImageFormat;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class MatProxy implements ImageProxy, AutoCloseable {
	private volatile Mat m_mat;
	private final ImageFormat m_format;

	public MatProxy(Mat mat, ImageFormat format) {
		m_mat = mat;
		m_format = format;
	}

	public MatProxy(Mat mat) {
		m_mat = mat;
		m_format = new ImageFormat(ImageEncoding.JPEG, OpenCvJUtils.toResolution(mat.size()));
	}
	
	public final Mat getMat() {
		return m_mat;
	}

	@Override
	public void close() throws IOException {
		if ( m_mat != null ) {
			m_mat.release();
			m_mat = null;
		}
	}

	@Override
	public final ImageFormat getImageFormat() {
		return m_format;
	}

	@Override
	public final ImageType getImageType() {
		return (m_mat.type() == CvType.CV_8UC3) ? ImageType.RGB : ImageType.DEPTH;
	}

	@Override
	public byte[] getDataBytes() {
		Size sz = m_mat.size();
		byte[] pixels = new byte[(int)(sz.width * sz.height * m_mat.elemSize())];
		m_mat.get(0, 0, pixels);
		
		return pixels;
	}

	@Override
	public short[] getDataShorts() {
		Size sz = m_mat.size();
		short[] pixels = new short[(int)(sz.width * sz.height)];
		m_mat.get(0, 0, pixels);
		
		return pixels;
	}

	@Override
	public final Image toJpegImage(int jpegQuality) {
		if ( m_mat == null ) {
			throw new IllegalStateException("MatProxy has been released already");
		}
		
		return new Image(Mats.toJpegBytes(m_mat, jpegQuality), m_format);
	}
	
	public MatConvas getImageConvas() {
		return new MatConvas(m_mat);
	}
	
	public BufferedImage getBufferedImage() {
		return Mats.toBufferedImage(m_mat);
	}
}
