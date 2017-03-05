package opencvj.features2d;

import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.features2d.KeyPoint;

import opencvj.OpenCvJException;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class OpenCvOutputStream implements Closeable {
	private DataOutputStream m_ostream;
	
	public interface ObjectWriter<T> {
		void write(OpenCvOutputStream istream, T obj) throws IOException;
	}
	
	public OpenCvOutputStream(DataOutputStream istream) {
		m_ostream = istream;
	}
	
	public void close() throws IOException {
		m_ostream.close();
	}
	
	public void writeInt(int v) throws IOException {
		m_ostream.writeInt(v);
	}
	
	public void writeFloat(float v) throws IOException {
		m_ostream.writeFloat(v);
	}
	
	public void writeString(String str) throws IOException {
		m_ostream.writeUTF(str);
	}
	
	public void writeKeyPoints(KeyPoint[] keypoints) throws IOException {
		writeArray(keypoints, m_keypoint);
	}
	
	public void writeKeyPoint(KeyPoint keypoint) throws IOException {
		m_keypoint.write(this, keypoint);
	}
	
	public void writePoint2i(Point pt) throws IOException {
		WRITER_POINT2I.write(this, pt);
	}
	
	public void writePoint2iArray(Point[] pts) throws IOException {
		writeArray(pts, WRITER_POINT2I);
	}
	
	public void writePoint2f(Point pt) throws IOException {
		WRITER_POINT2F.write(this, pt);
	}
	
	public void writePoint2fArray(Point[] pts) throws IOException {
		writeArray(pts, WRITER_POINT2F);
	}
	
	public void writePoint2d(Point pt) throws IOException {
		m_ostream.writeDouble(pt.x);
		m_ostream.writeDouble(pt.y);
	}
	
	public void writeSize2i(Size sz) throws IOException {
		m_ostream.writeInt((int)sz.width);
		m_ostream.writeInt((int)sz.height);
	}
	
	public void writeMat(Mat mat) throws IOException {
		writeSize2i(mat.size());
		
		int type = mat.type();
		m_ostream.writeInt(type);
		
		switch ( CvType.depth(type) ) {
			case CvType.CV_32F:
				float[] fdata = new float[(int)mat.size().area() * CvType.channels(type)];
				mat.get(0, 0, fdata);
				for ( int i=0; i < fdata.length; ++i ) {
					m_ostream.writeFloat(fdata[i]);
				}
				break;
			case CvType.CV_8U:
				byte[] bdata = new byte[(int)mat.size().area() * CvType.channels(type)];
				mat.get(0, 0, bdata);
				for ( int i=0; i < bdata.length; ++i ) {
					m_ostream.writeByte(bdata[i]);
				}
				break;
			default:
				throw new OpenCvJException("unsupport mat type: type=" + type);
		}
	}

	public <T> void writeArray(T[] array, ObjectWriter<T> writer) throws IOException {
		if ( array != null ) {
			m_ostream.writeInt(array.length);
			for ( int i =0; i < array.length; ++i ) {
				writer.write(this, array[i]);
			}
		}
		else {
			m_ostream.writeInt(-1);
		}
	}

	public <T> void writeCollection(Collection<T> coll, ObjectWriter<T> writer) throws IOException {
		if ( coll != null ) {
			m_ostream.writeInt(coll.size());
			for ( T data: coll ) {
				writer.write(this, data);
			}
		}
		else {
			m_ostream.writeInt(-1);
		}
	}
	
	private static final ObjectWriter<Point> WRITER_POINT2I = new ObjectWriter<Point>() {
		@Override
		public void write(OpenCvOutputStream ostream, Point pt) throws IOException {
			ostream.writeInt((int)pt.x);
			ostream.writeInt((int)pt.y);
		}
	};
	
	private static final ObjectWriter<Point> WRITER_POINT2F = new ObjectWriter<Point>() {
		@Override
		public void write(OpenCvOutputStream ostream, Point pt) throws IOException {
			ostream.writeFloat((float)pt.x);
			ostream.writeFloat((float)pt.y);
		}
	};
	
	private static final ObjectWriter<KeyPoint> m_keypoint = new ObjectWriter<KeyPoint>() {
		@Override
		public void write(OpenCvOutputStream ostream, KeyPoint keypt) throws IOException {
			ostream.writeFloat((float)keypt.pt.x);
			ostream.writeFloat((float)keypt.pt.y);
			ostream.writeFloat(keypt.size);
			ostream.writeFloat(keypt.angle);
			ostream.writeFloat(keypt.response);
			ostream.writeInt(keypt.octave);
			ostream.writeInt(keypt.class_id);
		}
	};
}
