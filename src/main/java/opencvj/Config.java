package opencvj;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import opencvj.camera.FlipCode;
import utils.Tuple2;
import utils.UninitializedException;
import utils.UnitUtils;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class Config {
	private final String m_path;
	private final JsonElement m_node;
	private final Properties m_variables;
	
	public static Config from(File configFile, String startPath)
														throws FileNotFoundException, IOException {
		Preconditions.checkNotNull(configFile, "configuration file is null");
		
		Tuple2<JsonObject,Properties> tuple = loadConfigFile(configFile);
		if ( startPath == null || startPath.trim().length() == 0 ) {
			return new Config(tuple._1, "", tuple._2);
		}
		else {
			return null;
		}
	}
	
	public Config(JsonElement node, String path, Properties variables) {
		m_node = node;
		m_path = path;
		m_variables = variables;
	}
	
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
	
	public Config(File file, String path, JsonElement node, Properties variables) {
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
	
	public Optional<Config> get(String name) {
		Preconditions.checkNotNull(name, "member name is null");
		Preconditions.checkState(m_node instanceof JsonObject, "not composite, path=" + m_path);
		
		JsonElement member = ((JsonObject)m_node).get(name);
		if ( member == null ) {
			return Optional.empty();
		}
		
		String path = m_path.length() > 0 ? m_path + "/" + name : name;
		return Optional.of(new Config(member, path, m_variables));
	}
	
	public Optional<Config> traverse(String path) {
		Preconditions.checkNotNull(path, "path is null");
		
		Config config = this;
		String fullPath = (m_path.length() == 0) ? path : m_path + "." + path;
		for ( String seg: fullPath.trim().split("/") ) {
			Optional<Config> member = config.get(seg);
			if ( member.isPresent() ) {
				config = member.get();
			}
			else {
				return Optional.empty();
			}
		}
		
		return Optional.of(config);
	}
	
	public boolean isContainer() {
		return m_node instanceof JsonObject;
	}
	
	public boolean hasMember(String name) {
		if ( !(m_node instanceof JsonObject) ) {
			return false;
		}
		else {
			return ((JsonObject)m_node).get(name) != null;
		}
	}
	
	public int asInt() {
		return m_node.getAsInt();
	}
	
	public long asLong() {
		return m_node.getAsLong();
	}
	
	public float asFloat() {
		return m_node.getAsFloat();
	}
	
	public String asString() {
		return StrSubstitutor.replace(m_node.getAsString(), m_variables);
	}
	
	public boolean asBoolean() {
		return m_node.getAsBoolean();
	}
	
	public File asFile() {
		return new File(asString());
	}
	
	public long asDuration() {
		return UnitUtils.parseDuration(asString());
	}
	
	public FlipCode asFlipCode() {
		return FlipCode.from(asString());
	}
	
	public Size asSize() {
		return asSize(m_node);
	}
	
	public static Size asSize(JsonElement node) {
		Size size = new Size();
		
		if ( node.isJsonArray() && node.getAsJsonArray().size() == 2 ) {
			JsonArray jarr = node.getAsJsonArray();
			if ( jarr.size() == 2 ) {
				return new Size(jarr.get(0).getAsDouble(), jarr.get(1).getAsDouble());
			}
		}
		else if ( node.isJsonObject() ) {
			JsonObject jobj = (JsonObject)node;
			
			JsonElement width = jobj.get("width");
			JsonElement height = jobj.get("height");
			size.width = (width != null) ? width.getAsDouble() : Double.MAX_VALUE;
			size.height = (height != null) ? height.getAsDouble() : Double.MIN_VALUE;
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
	
	public static IntRange asIntRange(JsonElement node) {
		IntRange range = new IntRange();
		
		if ( node.isArray() && node.size() == 2 ) {
			range.low = node.get(0).asInt();
			range.high = node.get(1).asInt();
		}
		else if ( node.isContainerNode() ) {
			JsonElement high = node.get("high");
			JsonElement low = node.get("low");
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
			JsonElement high = m_node.get("high");
			JsonElement low = m_node.get("low");
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
	
	public static DoubleRange asDoubleRange(JsonElement node) {
		DoubleRange range = new DoubleRange();
		
		if ( node.isArray() && node.size() == 2 ) {
			range.low = node.get(0).asDouble();
			range.high = node.get(1).asDouble();
		}
		else if ( node.isContainerNode() ) {
			JsonElement high = node.get("high");
			JsonElement low = node.get("low");
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
	
	public static SizeRange asSizeRange(JsonElement node) {
		SizeRange range = new SizeRange();
		
		if ( node.isArray() && node.size() == 2 ) {
			range.low = node.get(0).asInt();
			range.high = node.get(1).asInt();
		}
		else if ( node.isContainerNode() ) {
			JsonElement high = node.get("high");
			JsonElement low = node.get("low");
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
	
	public static Point asPoint(JsonElement node) {
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
	
	public static Point[] asPoints(JsonElement node) {
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
	
	public static double[] toJsonElement(Point pt) {
		return new double[]{pt.x, pt.y};
	}
	
	public static double[] toJsonElement(Point[] pts) {
		double[] vals = new double[pts.length*2];
		for ( int i =0; i < pts.length; ++i ) {
			vals[i*2] = pts[i].x;
			vals[i*2+1] = pts[i].y;
		}
		
		return vals;
	}
	
	public static Map<String,Object> toJsonElement(Mat mat) {
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
	
	public static Mat asMat(JsonElement node) {
		if ( node.isContainerNode() ) {
			int rows = node.get("rows").asInt();
			int cols = node.get("cols").asInt();
			int type = node.get("type").asInt();

			Mat mat = new Mat(rows, cols, type);
			JsonElement data = node.get("data");
			
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
	
	private static JsonElement traverse(JsonElement node, String path) {
		if ( (path = path.trim()).length() > 0 ) {
			for ( String name: path.split("\\.") ) {
				node = node.path(name);
			}
		}
		return node;
	}
	
	private static Tuple2<JsonObject,Properties> loadConfigFile(File configFile)
														throws FileNotFoundException, IOException {
		try ( FileReader reader = new FileReader(configFile) ) {
			JsonElement root = new JsonParser().parse(reader);
			if ( !(root instanceof JsonObject) ) {
				throw new IllegalArgumentException("invalid configuration file: path=" + configFile);
			}
			
			Properties variables = new Properties();
			variables.put("config_dir", configFile.getParentFile().getAbsolutePath());
			
			Map<String,String> envVars = System.getenv();
			for ( Map.Entry<String,String> e: envVars.entrySet() ) {
				variables.put(e.getKey(), StrSubstitutor.replace(e.getValue(), variables));
			}
			
			JsonElement configElm = ((JsonObject)root).get("config_variables");
			if ( configElm != null && configElm instanceof JsonObject ) {
				JsonObject config = (JsonObject)configElm;
				
				config.entrySet().stream()
						.forEach(ent -> {
							String value = ent.getValue().getAsString();
							value = StrSubstitutor.replace(value, variables);
							variables.put(ent.getKey(), value);
						});
			}
			
			return new Tuple2<>((JsonObject)root, variables);
		}
	}
}
