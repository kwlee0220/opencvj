package opencvj.features2d;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Array;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.features2d.KeyPoint;

import opencvj.Mats;
import opencvj.OpenCvJException;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class OpenCvInputStream implements Closeable {
	private DataInputStream m_istream;
	
	interface ObjectReader<T> {
		T read(OpenCvInputStream istream) throws IOException;
	}
	
	public OpenCvInputStream(DataInputStream istream) {
		m_istream = istream;
	}
	
	public void close() throws IOException {
		m_istream.close();
	}
	
	public int readInt() throws IOException {
		return m_istream.readInt();
	}
	
	public float readFloat() throws IOException {
		return m_istream.readFloat();
	}
	
	public String readString() throws IOException {
		return m_istream.readUTF();
	}
	
	public KeyPoint[] readKeyPoints() throws IOException {
		return readArray(KeyPoint.class, READER_KEYPOINT);
	}
	
	public KeyPoint readKeyPoint() throws IOException {
		return READER_KEYPOINT.read(this);
	}
	
	public Point readPoint2i() throws IOException {
		Point pt = new Point();
		pt.x = m_istream.readInt();
		pt.y = m_istream.readInt();
		
		return pt;
	}
	
	public Point readPoint2f() throws IOException {
		return READER_POINT2F.read(this);
	}
	
	public Point[] readPoint2fArray() throws IOException {
		return readArray(Point.class, READER_POINT2F);
	}
	
	public Point readPoint2d() throws IOException {
		Point pt = new Point();
		pt.x = m_istream.readDouble();
		pt.y = m_istream.readDouble();
		
		return pt;
	}
	
	public Size readSize2i() throws IOException {
		Size sz = new Size();
		sz.width = m_istream.readInt();
		sz.height = m_istream.readInt();
		
		return sz;
	}
	
	public void readMat(Mat mat) throws IOException {
		Size size = readSize2i();
		int type = readInt();
		
		Mats.createIfNotValid(mat, size, type);
		int depth = CvType.depth(type);
		int channel = CvType.channels(type);
		
		switch ( depth ) {
			case CvType.CV_32F:
				float[] fdata = new float[(int)size.area() * channel];
				mat.get(0, 0, fdata);
				for ( int i=0; i < fdata.length; ++i ) {
					fdata[i] = m_istream.readFloat();
				}
				mat.put(0, 0, fdata);
				break;
			case CvType.CV_8U:
				byte[] bdata = new byte[(int)size.area() * channel];
				mat.get(0, 0, bdata);
				for ( int i=0; i < bdata.length; ++i ) {
					bdata[i] = m_istream.readByte();
				}
				mat.put(0, 0, bdata);
				break;
			default:
				throw new OpenCvJException("unsupport mat type: type=" + type);
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T[] readArray(Class<T> cls, ObjectReader<T> reader) throws IOException {
		int count = m_istream.readInt();
		if ( count >= 0 ) {
			Object array = Array.newInstance(cls, count);
			for ( int i =0; i < count; ++i ) {
				Array.set(array, i, reader.read(this));
			}
	
			return (T[])array;
		}
		else {
			return null;
		}
	}
	
	private static final ObjectReader<Point> READER_POINT2F = new ObjectReader<Point>() {
		@Override
		public Point read(OpenCvInputStream istream) throws IOException {
			Point pt = new Point();
			pt.x = istream.readFloat();
			pt.y = istream.readFloat();
			
			return pt;
		}
	};
	
	private static final ObjectReader<KeyPoint> READER_KEYPOINT = new ObjectReader<KeyPoint>() {
		@Override
		public KeyPoint read(OpenCvInputStream istream) throws IOException {
			float x = istream.readFloat();
			float y = istream.readFloat();
			float size = istream.readFloat();
			float angle = istream.readFloat();
			float response = istream.readFloat();
			int octave = istream.readInt();
			int classId = istream.readInt();
			
			return new KeyPoint(x, y, size, angle, response, octave, classId);
		}
	};
}
