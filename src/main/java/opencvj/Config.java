package opencvj;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import camus.service.DoubleRange;
import camus.service.FloatRange;
import camus.service.IntRange;
import camus.service.SizeRange;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

import com.fasterxml.jackson.databind.JsonNode;

import opencvj.camera.FlipCode;
import utils.UninitializedException;
import utils.UnitUtils;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class Config {
	private final File m_file;
	private final String m_path;
	private JsonNode m_node;
	private final Properties m_variables = new Properties();
	
	public Config(File file, String path) {
		if ( file == null ) {
			throw new UninitializedException("Property 'configFile' was not specified: class="
											+ getClass().getName());
		}
		m_file = file;
		m_path = path;
		
		initialize();
	}
	
	public Config(File file) {
		this(file, "");
	}
	
	public Config(File file, String path, JsonNode node, Properties variables) {
		m_file = file;
		m_path = path;
		m_node = node;
		
		initialize();
		m_variables.putAll(variables);
	}
	
	public Config(String configStr) {
		String[] parts = configStr.split("#");
		
		m_file = new File(parts[0]);
		if ( !m_file.canRead() ) {
			throw new OpenCvJException("Config file not found: path=" + parts[0]
										+ ", config=" + configStr);
		}
		m_path = parts[1];
		
		initialize();
	}
	
	public void addVariable(String name, String value) {
		m_variables.put(name, value);
	}
	
	public static Config fromString(String configStr) {
		return new Config(configStr);
	}
	
	public File getHomeDir() {
		return m_file.getParentFile();
	}
	
	public File getConfigFile() {
		return m_file;
	}
	
	public String getPath() {
		return m_path;
	}
	
	public Config get(String name) {
		String path = m_path.length() > 0 ? m_path + "." + name : name;
		return new Config(m_file, path, m_node.path(name), m_variables);
	}
	
	public Config traverse(String path) {
		if ( path != null ) {
			String prefix = (m_path.length() == 0) ? "" : m_path + ".";
			return new Config(m_file, prefix + path.trim(), traverse(m_node, path), m_variables);
		}
		else {
			return this;
		}
	}
	
	public Config fullPath(String path) {
		JsonNode node = null;
		try {
			node = OpenCvJUtils.readJsonFile(m_file);
		}
		catch ( IOException e ) {
			throw new OpenCvJException(String.format("fails to load config %s",
														toString()) + ", cause=" + e);
		}
		
		if ( path != null ) {
			return new Config(m_file, path, traverse(node, path), m_variables);
		}
		else {
			return this;
		}
	}
	
	public boolean isMissing() {
		return m_node.isMissingNode();
	}
	
	public boolean isContainer() {
		return m_node.isContainerNode();
	}
	
	public boolean hasChild(String name) {
		return m_node.get(name) != null;
	}
	
	public int asInt() {
		assertNotMissingNode();
		return m_node.intValue();
	}
	
	public int asInt(int defValue) {
		return m_node.isMissingNode() ? defValue : m_node.intValue();
	}
	
	public long asLong() {
		assertNotMissingNode();
		return m_node.longValue();
	}
	
	public long asLong(long defValue) {
		return m_node.isMissingNode() ? defValue : m_node.longValue();
	}
	
	public float asFloat() {
		assertNotMissingNode();
		return m_node.floatValue();
	}
	
	public float asFloat(float defValue) {
		return m_node.isMissingNode() ? defValue : m_node.floatValue();
	}
	
	public String asString(String defValue) {
		String str = m_node.isMissingNode() ? defValue : m_node.textValue();
		if ( str != null ) {
			return StrSubstitutor.replace(str, m_variables);
		}
		else {
			return str;
		}
	}
	
	public String asString() {
		assertNotMissingNode();
		return StrSubstitutor.replace(m_node.textValue(), m_variables);
	}
	
	public boolean asBoolean(boolean defValue) {
		return m_node.isMissingNode() ? defValue : m_node.booleanValue();
	}
	
	public boolean asBoolean() {
		assertNotMissingNode();
		return m_node.booleanValue();
	}
	
	public Config asReference() {
		String path = asString();
		return new Config(m_file, path);
	}
	
	public File asFile(File defValue) {
		return m_node.isMissingNode() ? defValue : new File(asString());
	}
	
	public File asFile() {
		return new File(asString());
	}
	
	public long asDuration(String defValue) {
		String durStr = m_node.isMissingNode() ? defValue : asString();
		return UnitUtils.parseDuration(durStr);
	}
	
	public long asDuration() {
		return UnitUtils.parseDuration(asString());
	}
	
	public FlipCode asFlipCode() {
		assertNotMissingNode();
		return FlipCode.from(asString());
	}
	
	public FlipCode asFlipCode(FlipCode defValue) {
		return m_node.isMissingNode() ? defValue : FlipCode.from(asString());
	}
	
	public Size asSize() {
		assertNotMissingNode();
		return asSize(m_node);
	}
	
	public Size asSize(Size defValue) {
		return m_node.isMissingNode() ? defValue : asSize(m_node);
	}
	
	public static Size asSize(JsonNode node) {
		Size size = new Size();
		
		if ( node.isArray() && node.size() == 2 ) {
			size.width = node.get(0).asDouble();
			size.height = node.get(1).asDouble();
		}
		else if ( node.isContainerNode() ) {
			JsonNode width = node.get("width");
			JsonNode height = node.get("height");
			size.width = (width != null) ? width.asDouble() : Double.MAX_VALUE;
			size.height = (height != null) ? height.asDouble() : Double.MIN_VALUE;
		}
		else {
			throw new IllegalArgumentException("invalid Size value");
		}
		
		return size;
	}
	
	public Scalar asScalar() {
		assertNotMissingNode();

		if ( m_node.isArray() ) {
			double[] values = new double[m_node.size()];
			for ( int i =0; i < values.length; ++i ) {
				values[i] = m_node.get(i).doubleValue();
			}
			
			return new Scalar(values);
		}
		else {
			throw new IllegalArgumentException("invalid Scalar value");
		}
	}
	
	public Scalar asScalar(Scalar defValue) {
		return m_node.isMissingNode() ? defValue : asScalar();
	}
	
	public IntRange asIntRange() {
		assertNotMissingNode();
		return asIntRange(m_node);
	}
	
	public static IntRange asIntRange(JsonNode node) {
		IntRange range = new IntRange();
		
		if ( node.isArray() && node.size() == 2 ) {
			range.low = node.get(0).asInt();
			range.high = node.get(1).asInt();
		}
		else if ( node.isContainerNode() ) {
			JsonNode high = node.get("high");
			JsonNode low = node.get("low");
			range.high = (high != null) ? high.asInt() : Integer.MAX_VALUE;
			range.low = (low != null) ? low.asInt() : Integer.MIN_VALUE;
		}
		else {
			throw new IllegalArgumentException("invalid IntRange value");
		}
		
		return range;
	}
	
	public FloatRange asFloatRange(FloatRange defValue) {
		return m_node.isMissingNode() ? defValue : asFloatRange();
	}
	
	public FloatRange asFloatRange() {
		assertNotMissingNode();
		
		FloatRange range = new FloatRange();
		if ( m_node.isArray() && m_node.size() == 2 ) {
			range.low = (float)m_node.get(0).asDouble();
			range.high = (float)m_node.get(1).asDouble();
		}
		else if ( m_node.isContainerNode() ) {
			JsonNode high = m_node.get("high");
			JsonNode low = m_node.get("low");
			range.high = (high != null) ? (float)high.asDouble() : Float.MAX_VALUE;
			range.low = (low != null) ? (float)low.asDouble() : Float.MIN_VALUE;
		}
		else {
			throw new IllegalArgumentException("invalid FloatRange value");
		}
		
		return range;
	}
	
	public DoubleRange asDoubleRange(DoubleRange defValue) {
		return m_node.isMissingNode() ? defValue : asDoubleRange();
	}
	
	public DoubleRange asDoubleRange() {
		assertNotMissingNode();
		return asDoubleRange(m_node);
	}
	
	public static DoubleRange asDoubleRange(JsonNode node) {
		DoubleRange range = new DoubleRange();
		
		if ( node.isArray() && node.size() == 2 ) {
			range.low = node.get(0).asDouble();
			range.high = node.get(1).asDouble();
		}
		else if ( node.isContainerNode() ) {
			JsonNode high = node.get("high");
			JsonNode low = node.get("low");
			range.high = (high != null) ? high.asDouble() : Double.MAX_VALUE;
			range.low = (low != null) ? low.asDouble() : Double.MIN_VALUE;
		}
		else {
			throw new IllegalArgumentException("invalid DoubleRange value");
		}
		
		return range;
	}
	
	public SizeRange asSizeRange() {
		assertNotMissingNode();
		return asSizeRange(m_node);
	}
	
	public SizeRange asSizeRange(SizeRange defValue) {
		return m_node.isMissingNode() ? defValue : asSizeRange();
	}
	
	public static SizeRange asSizeRange(JsonNode node) {
		SizeRange range = new SizeRange();
		
		if ( node.isArray() && node.size() == 2 ) {
			range.low = node.get(0).asInt();
			range.high = node.get(1).asInt();
		}
		else if ( node.isContainerNode() ) {
			JsonNode high = node.get("high");
			JsonNode low = node.get("low");
			range.high = (high != null) ? high.asInt() : Integer.MAX_VALUE;
			range.low = (low != null) ? low.asInt() : Integer.MIN_VALUE;
		}
		else {
			throw new IllegalArgumentException("invalid SizeRange value");
		}
		
		return range;
	}
	
	public Point asPoint(Point defValue) {
		return m_node.isMissingNode() ? defValue : asPoint();
	}
	
	public Point asPoint() {
		assertNotMissingNode();
		return asPoint(m_node);
	}
	
	public static Point asPoint(JsonNode node) {
		Point pt = new Point();
		
		if ( node.isArray() && node.size() == 2 ) {
			pt.x = node.get(0).asDouble();
			pt.y = node.get(1).asDouble();
		}
		else {
			throw new IllegalArgumentException("invalid Point value");
		}
		
		return pt;
	}
	
	public Point[] asPoints() {
		assertNotMissingNode();
		return asPoints(m_node);
	}
	
	public static Point[] asPoints(JsonNode node) {
		if ( node.isArray() && node.size()%2 == 0 ) {
			Point[] pts = new Point[node.size() / 2];
			for ( int i =0; i < pts.length; ++i ) {
				pts[i] = new Point();
				pts[i].x = node.get(i*2).asDouble();
				pts[i].y = node.get(i*2+1).asDouble();
			}
			
			return pts;
		}
		else {
			throw new IllegalArgumentException("invalid Point Array value");
		}
	}
	
	public static double[] toJsonNode(Point pt) {
		return new double[]{pt.x, pt.y};
	}
	
	public static double[] toJsonNode(Point[] pts) {
		double[] vals = new double[pts.length*2];
		for ( int i =0; i < pts.length; ++i ) {
			vals[i*2] = pts[i].x;
			vals[i*2+1] = pts[i].y;
		}
		
		return vals;
	}
	
	public static Map<String,Object> toJsonNode(Mat mat) {
		Map<String,Object> map = new HashMap<String,Object>();

		int rows = mat.rows();
		int cols = mat.cols();
		
		map.put("rows", rows);
		map.put("cols", cols);
		map.put("type", mat.type());
		
		switch ( mat.type() ) {
			case CvType.CV_64F:
				double[] doubleData = new double[rows * cols];
				mat.get(0, 0, doubleData);
				map.put("data", doubleData);
				break;
			case CvType.CV_32F:
				MatOfFloat mof = new MatOfFloat(mat);
				map.put("data", mof.toArray());
				mof.release();
				break;
			default:
				throw new java.lang.UnsupportedOperationException("unsupported mat type=" + mat.type());
		}
		
		return map;
	}
	
	public Mat asMat() {
		assertNotMissingNode();
		return asMat(m_node);
	}
	
	public static Mat asMat(JsonNode node) {
		if ( node.isContainerNode() ) {
			int rows = node.get("rows").asInt();
			int cols = node.get("cols").asInt();
			int type = node.get("type").asInt();

			Mat mat = new Mat(rows, cols, type);
			JsonNode data = node.get("data");
			
			switch ( type ) {
				case CvType.CV_64F:
					double[] doubleData = new double[rows * cols];
					for ( int i =0; i < data.size(); ++i ) {
						doubleData[i] = data.get(i).asDouble();
					}
					mat.put(0, 0, doubleData);
					break;
			}
			
			return mat;
		}
		else {
			throw new IllegalArgumentException("invalid Mat value");
		}
	}

	@Override
	public String toString() {
		return String.format("%s:%s", m_file.getAbsolutePath(), m_path);
	}
	
	private static JsonNode traverse(JsonNode node, String path) {
		if ( (path = path.trim()).length() > 0 ) {
			for ( String name: path.split("\\.") ) {
				node = node.path(name);
			}
		}
		return node;
	}
	
	private void assertNotMissingNode() {
		if ( m_node.isMissingNode() ) {
			throw new OpenCvJException(String.format("invalid path: path=%s file=%s",
													m_path, m_file.getAbsolutePath()));
		}
	}
	
	private final void initialize() {
		JsonNode node = null;
		try {
			node = OpenCvJUtils.readJsonFile(m_file);
		}
		catch ( IOException e ) {
			throw new OpenCvJException(String.format("fails to load config %s",
														toString()) + ", cause=" + e);
		}

		m_variables.put("config_dir", m_file.getParentFile().getAbsolutePath());
		
		Map<String,String> envVars = System.getenv();
		for ( Map.Entry<String,String> e: envVars.entrySet() ) {
			m_variables.put(e.getKey(), StrSubstitutor.replace(e.getValue(), m_variables));
		}
		
		JsonNode configNode = node.path("config_variables");
		if ( !configNode.isMissingNode() ) {
			Iterator<Map.Entry<String,JsonNode>> it = configNode.fields();
			while ( it.hasNext() ) {
				final Map.Entry<String,JsonNode> e = it.next();
				String value = StrSubstitutor.replace(e.getValue().asText(), m_variables);
				m_variables.put(e.getKey(), value);
			}
		}
		
		m_node = traverse(node, m_path);
	}
}
