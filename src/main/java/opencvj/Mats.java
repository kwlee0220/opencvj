package opencvj;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.util.List;

import camus.service.IntRange;
import camus.service.camera.ImageProxy;
import camus.service.camera.JniImageProxy;
import camus.service.geo.Size2d;
import camus.service.vision.Image;
import camus.service.vision.ImageEncoding;
import camus.service.vision.ImageFormat;
import camus.service.vision.Resolution;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import utils.swing.ImageUtils;



/**
 * 
 * @author Kang-Woo Lee
 */
public final class Mats {
	public static final Mat EMPTY = new Mat();
	
	private Mats() {
		throw new AssertionError("Should not be called this one: " + Mats.class);
	}
	
	public static void releaseAll(List<? extends Mat> mats) {
		for ( Mat mat: mats ) {
			if ( mat != null ) {
				mat.release();
			}
		}
	}
	
	public static void releaseAll(Mat... mats) {
		for ( Mat mat: mats ) {
			if ( mat != null ) {
				mat.release();
			}
		}
	}
	
	public static Mat toMat(File file) {
		return Highgui.imread(file.getAbsolutePath(),
							Highgui.CV_LOAD_IMAGE_ANYCOLOR + Highgui.CV_LOAD_IMAGE_ANYDEPTH);
	}

	public static void toMat(BufferedImage bi, Mat image) {
		Mats.createIfNotValid(image, new Size(bi.getWidth(), bi.getHeight()), CvType.CV_8UC3);
		
		byte[] pixels;
		if ( bi.getType() == BufferedImage.TYPE_INT_RGB ) {
			int nbytes = (int)image.total() * (int)image.elemSize();
			pixels = new byte[nbytes];
			int[] rgb = ((DataBufferInt)bi.getRaster().getDataBuffer()).getData();
			for ( int i =0; i < rgb.length; ++i ) {
				pixels[i*3] = (byte)((rgb[i]) & 0xFF);			// B
				pixels[i*3+1] = (byte)((rgb[i] >> 8) & 0xFF);	// G
				pixels[i*3+2] = (byte)((rgb[i] >> 16) & 0xFF);		// R
			}
		}
		else {
			pixels = ((DataBufferByte)bi.getRaster().getDataBuffer()).getData();
		}
		image.put(0, 0, pixels);
	}
	
	public static Mat toMat(Image image) {
		if ( image.format.getImageEncoding() != ImageEncoding.JPEG ) {
			throw new IllegalArgumentException("invalid image format: JPEG expected, but="
												+ image.format);
		}
		
		MatOfByte mob = new MatOfByte(image.bytes);
		try {
			return Highgui.imdecode(mob, Highgui.CV_LOAD_IMAGE_ANYCOLOR + Highgui.CV_LOAD_IMAGE_ANYDEPTH);
		}
		finally {
			mob.release();
		}
	}
	
	public static final Mat toMat(ImageProxy proxy) {
		if ( proxy instanceof MatProxy ) {
			return ((MatProxy)proxy).getMat();
		}
		else if ( proxy instanceof JniImageProxy ) {
			return new Mat((long)((JniImageProxy)proxy).getJniPointer());
		}
		
		throw new IllegalArgumentException("invalid proxy: " + proxy);
	}
	
	public static BufferedImage toBufferedImage(Image image) {
		Mat mat = toMat(image);
		try {
			return toBufferedImage(mat);
		}
		finally {
			mat.release();
		}
	}
	
	public static BufferedImage toBufferedImage(Mat mat) {
		Mat colored = new Mat();
		Mat target = mat;
		
		try {
			int mtype = mat.type();
			if ( mtype != CvType.CV_8UC1 && mtype != CvType.CV_8UC3 ) {
				if ( mtype == CvType.CV_16SC1 ) {
					Mats.scaleDepthToColoredGray(mat, colored);
				}
				else if ( mat.channels() == 1 ) {
					Mats.scaleToColoredGray(mat, colored, null);
				}
				else {
					throw new OpenCvJException("unsupported image type: type=" + mtype);
				}
				target = colored;
			}
	
			Size sz = mat.size();
			Size2d size = new Size2d((int)sz.width, (int)sz.height);
			
			int type;
			switch ( target.channels() ) {
				case 3:
					type = BufferedImage.TYPE_3BYTE_BGR;
					break;
				case 1:
					type = BufferedImage.TYPE_BYTE_GRAY;
					break;
				default:
					throw new IllegalArgumentException("invalid mat: invalid channel: " + mat.channels());
			}
			
			byte[] pixels = new byte[size.getArea() * (int)target.elemSize()];
			target.get(0, 0, pixels);
			
			BufferedImage bi = new BufferedImage(size.width, size.height, type);
			byte[] tarPixels = ((DataBufferByte)bi.getRaster().getDataBuffer()).getData();
			System.arraycopy(pixels, 0, tarPixels, 0, pixels.length);
			
			return bi;
		}
		finally {
			colored.release();
		}
	}

	public static void toMat(Image image, Mat mat) {
		if ( image.format.encoding != ImageEncoding.JPEG ) {
			throw new OpenCvJException("unsupported image encoding: encoding="
												+ image.format.encoding);
		}
		
		createIfNotValid(mat, OpenCvJUtils.toCvSize(image.format.getSize()), CvType.CV_8UC3);
		
		mat.put(0, 0, ImageUtils.getRasterBytes(image.toBufferedImage()));
	}

	public static final boolean isValid(Mat mat) {
		return mat != null && !mat.empty();
	}

	public static final boolean isValid(Mat mat, Size size) {
		return mat != null && mat.size().equals(size);
	}

	public static final void createIfNotValid(Mat mat, Size size, int type) {
		createIfNotValid(mat, size, type, null);
	}

	public static final void createGrayIfNotValid(Mat mat, Size size) {
		createIfNotValid(mat, size, CvType.CV_8UC1, null);
	}

	public static final void createGrayIfNotValid(Mat mat, Size size, Scalar init) {
		createIfNotValid(mat, size, CvType.CV_8UC1, init);
	}

	public static final void createIfNotValid(Mat mat, Size size, int type, Scalar init) {
		if ( mat.empty() || !mat.size().equals(size) || mat.type() != type ) {
			mat.release();
			
			mat.create(size, type);
			if ( init != null ) {
				mat.setTo(init);
			}
		}
	}
	
	public static void calcRangeMask(Mat mat, IntRange range, Mat rangeMask) {
		createIfNotValid(rangeMask, mat.size(), CvType.CV_8UC1);
		Core.inRange(mat, new Scalar(range.low), new Scalar(range.high), rangeMask);
	}
	
	public static final void toGrayImage(Mat image, Mat gray) {
		Imgproc.cvtColor(image, gray, Imgproc.COLOR_RGB2GRAY);
	}
	
	public static final void toColored(Mat gray, Mat colored) {
		Imgproc.cvtColor(gray, colored, Imgproc.COLOR_GRAY2BGR);
	}

	public static void scaleToGray(Mat mat, Mat gray, Mat mask) {
		Mat tmp = new Mat();
		
		try {
			Core.MinMaxLocResult minMax = Core.minMaxLoc(mat, mask);
			Core.add(mat, new Scalar(-minMax.minVal), tmp);
			double alpha = 255 / (minMax.maxVal - minMax.minVal);
			tmp.convertTo(gray, CvType.CV_8UC1, alpha);
		}
		finally {
			Mats.releaseAll(tmp);
		}
	}

	public static void scaleToColoredGray(Mat mat, Mat colored, Mat mask) {
		scaleToGray(mat, colored, mask);
		Imgproc.cvtColor(colored, colored, Imgproc.COLOR_GRAY2BGR);
	}

	public static void scaleDepthToColoredGray(Mat mat, Scalar unknownColor, Mat colored) {
		Mat mask = new Mat();
		try {
			calcRangeMask(mat, new IntRange(1,Short.MAX_VALUE), mask);
			scaleToColoredGray(mat, colored, mask);
			
			Core.bitwise_not(mask, mask);
			colored.setTo(unknownColor, mask);
		}
		finally {
			Mats.releaseAll(mask);
		}
	}

	public static void scaleDepthToColoredGray(Mat mat, Mat colored) {
		scaleDepthToColoredGray(mat, OpenCvJ.RED, colored);
	}

	public static final Mat NIL = new Mat();
	public static void product(Mat a, Mat b, Mat result) {
		Core.gemm(a, b, 1, NIL, 0, result);
	}
	public static void product(Mat a, Mat b, Mat result, int flags) {
		Core.gemm(a, b, 1, NIL, 0, result, flags);
	}
	
	public static byte[] toJpegBytes(BufferedImage bi, int jpegQuality) {
		Mat tmpImage = new Mat();
		try {
			toMat(bi, tmpImage);
			return toJpegBytes(tmpImage, jpegQuality);
		}
		finally {
			tmpImage.release();
		}
	}

	public static byte[] toJpegBytes(Mat image, int jpegQuality) {
		MatOfByte jpegBuf = new MatOfByte();
		MatOfInt jpegParams = new MatOfInt(Highgui.IMWRITE_JPEG_QUALITY, jpegQuality);
		try {
			Highgui.imencode(".jpg", image, jpegBuf, jpegParams);
			return jpegBuf.toArray();
		}
		finally {
			releaseAll(jpegBuf, jpegParams);
		}
	}
	
	public static final Image toImage(Mat mat, int jpegQuality) {
		ImageFormat format = new ImageFormat(ImageEncoding.JPEG,
											OpenCvJUtils.toResolution(mat.size()));
		return new Image(Mats.toJpegBytes(mat, jpegQuality), format);
	}
	
	public static final Image toImage(BufferedImage bi, int jpegQuality) {
		ImageFormat format = new ImageFormat(ImageEncoding.JPEG, new Resolution(bi.getWidth(), bi.getHeight()));
		return new Image(Mats.toJpegBytes(bi, jpegQuality), format);
	}
}
