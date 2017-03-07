package opencvj;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import opencvj.blob.AdaptiveImageThreshold;
import opencvj.blob.BackgroundModel;
import opencvj.blob.DeltaAwareForegroundDetector;
import opencvj.blob.ForegroundDetector;
import opencvj.blob.HueThreshold;
import opencvj.blob.ImageThreshold;
import opencvj.blob.MAColorBackgroundModel;
import opencvj.blob.MAColorForegroundDetector;
import opencvj.blob.MADepthBackgroundModel;
import opencvj.blob.MADepthForegroundDetector;
import opencvj.blob.SimpleImageThreshold;
import opencvj.camera.CDCFactory;
import opencvj.camera.ColorDepthComposite;
import opencvj.camera.ColorDepthCompositeFactory;
import opencvj.camera.ColorDepthCompositeLoader;
import opencvj.camera.HighGuiCamera;
import opencvj.camera.OpenCvJCamera;
import opencvj.camera.OpenCvJCameraFactoryImpl;
import opencvj.camera.OpenCvJCameraLoader;
import opencvj.camera.OpenCvJDepthCamera;
import opencvj.features2d.ImageStore;
import opencvj.features2d.ObjectTemplateStore;
import opencvj.track.Backprojector;
import opencvj.track.DeltaDepthBackprojector;
import opencvj.track.HueBackprojector;
import utils.config.ConfigNode;
import utils.config.json.JsonConfiguration;




/**
 * 
 * @author Kang-Woo Lee
 */
public final class OpenCvJSystem {
	private static File HOME_DIR =null;
	private static File BIN_DIR =null;
	private static File DATA_DIR =null;
	
	private static File CONFIG_DIR =null;
	private static JsonConfiguration ROOT_CONFIG =null;
	
	private static OpenCvJLoader LOADER;
	private static final Map<String,OpenCvJCameraLoader> CAMERA_LOADERS
												= new HashMap<String,OpenCvJCameraLoader>();
	private static final Map<String,ColorDepthCompositeLoader> CDC_LOADERS
													= new HashMap<String,ColorDepthCompositeLoader>();
	private static final Map<String,BackgroundModel> BG_MODELS = new HashMap<String,BackgroundModel>();
	
	private OpenCvJSystem() {
		throw new AssertionError("Should not be called this one: " + OpenCvJSystem.class);
	}
	
	public static void initialize(File homeDir, String configFileName)
														throws FileNotFoundException, IOException {
		if ( homeDir == null ) {
			String homePath = System.getenv("OPENCVJ_HOME");
			if ( homePath == null ) {
				try {
					HOME_DIR = new File(".").getCanonicalFile();
				}
				catch ( IOException e ) {
					throw new OpenCvJException("fails to locate HOME dir, cause=" + e);
				}
			}
			else {
				HOME_DIR = new File(homePath);
			}
		}
		else {
			HOME_DIR = homeDir;
		}
		BIN_DIR = new File(getHomeDir(), "bin");
		DATA_DIR = new File(getHomeDir(), "data");
		
		CONFIG_DIR = new File(getHomeDir(), "configs");
		String fname = configFileName != null ? configFileName : "opencvj.conf";
		ROOT_CONFIG = JsonConfiguration.load(new File(getConfigDir(), fname));
		ROOT_CONFIG.addVariable("home.dir", HOME_DIR.getAbsolutePath());
		
		try {
			LOADER = new OpenCvJLoader();
			LOADER.setDllDir(getBinDir());
			LOADER.initialize();
		}
		catch ( Exception e ) {
			LOADER = null;
			throw new OpenCvJException("fails to create OpenCvJLoader, cause=" + e);
		}
		
		registerOpenCvJCameraLoader("highgui", HIGHGUI_LOADER);
	}
	
	public static void shutdown() {
		OpenCvViewManager.shutdown();
	}
	
	public static File getHomeDir() {
		return HOME_DIR;
	}
	
	public static File getBinDir() {
		return BIN_DIR;
	}
	
	public static File getDataDir() {
		return DATA_DIR;
	}
	
	public static File getDataFile(String path) {
		return new File(DATA_DIR, path);
	}
	
	public static File getConfigDir() {
		return CONFIG_DIR;
	}
	
	public static JsonConfiguration getConfiguration() {
		return ROOT_CONFIG;
	}
	
	public static ConfigNode getConfigNode(String path) {
		return getConfiguration().traverse(path);
	}
	
	public static OpenCvJLoader getOpenCvJLoader() {
		return LOADER;
	}
	
	public static final void registerOpenCvJCameraLoader(String type, OpenCvJCameraLoader fact) {
		CAMERA_LOADERS.put(type, fact);
	}
	
	public static final void unregisterOpenCvJCameraLoader(String type) {
		CAMERA_LOADERS.remove(type);
	}
	
	public static final void registerCDCLoader(String type, ColorDepthCompositeLoader fact) {
		CDC_LOADERS.put(type, fact);
	}
	
	public static final void unregisterCDCLoader(String type) {
		CDC_LOADERS.remove(type);
	}
	
	private static final OpenCvJCameraLoader HIGHGUI_LOADER = new OpenCvJCameraLoader() {
		@Override
		public OpenCvJCamera load(ConfigNode config) throws Exception {
			return HighGuiCamera.create(getOpenCvJLoader(), config);
		}
	};
	
	public static final OpenCvJCameraFactoryImpl createOpenCvJCameraFactory(ConfigNode config)
		throws Exception {
		String type = config.get("type").asString(null);
		OpenCvJCameraLoader loader = CAMERA_LOADERS.get(type);
		if ( loader != null ) {
			OpenCvJCameraFactoryImpl cameraFact = new OpenCvJCameraFactoryImpl();
			cameraFact.setSourceCamera(loader.load(config));
			cameraFact.setConfig(config);
			cameraFact.initialize();
			
			return cameraFact;
		}
		else {
			throw new OpenCvJException("unknown camera type=" + type);
		}
	}
	
	public static final OpenCvJCamera createOpenCvJCamera(ConfigNode config) throws Exception {
		String type = config.get("type").asString(null);
		OpenCvJCameraLoader loader = CAMERA_LOADERS.get(type);
		if ( loader != null ) {
			return loader.load(config);
		}
		else {
			throw new OpenCvJException("unknown camera type=" + type);
		}
	}
	
	public static final OpenCvJDepthCamera createDepthCamera(ConfigNode config) throws Exception {
		String type = config.get("type").asString(null);
		OpenCvJCameraLoader loader = CAMERA_LOADERS.get(type);
		if ( loader != null ) {
			return (OpenCvJDepthCamera)loader.load(config);
		}
		else {
			throw new OpenCvJException("unknown camera type=" + type);
		}
	}
	
	public static final ColorDepthCompositeFactory createCDCFactory(ConfigNode config)
		throws Exception {
		ColorDepthComposite cdc = createCDC(config);
		return CDCFactory.create(cdc, config);
	}
	
	private static final ColorDepthComposite createCDC(ConfigNode config) throws Exception {
		String type = config.get("type").asString(null);
		ColorDepthCompositeLoader loader = CDC_LOADERS.get(type);
		if ( loader != null ) {
			return loader.load(config);
		}
		else {
			throw new OpenCvJException("unknown ColorDepthComposite type=" + type);
		}
	}
	
//	public static final OpenCvJDepthCamera createDepthCamera(Config config) throws Exception {
//		String type = config.get("type").asString(null);
//		if ( "openni2_depth".equals(type) ) {
//			return OpenNI2DepthCamera.create(getOpenNI2Loader(), config);
//		}
//		else {
//			throw new OpenCvJException("unknown depth camera type=" + type);
//		}
//	}
//
//	public static final OpenNI2ColorDepthComposite createColorDepthComposite(Config config)
//		throws Exception {
//		String type = config.get("type").asString(null);
//		if ( "openni2".equals(type) ) {
//			return OpenNI2ColorDepthComposite.create(getOpenNI2Loader(), config);
//		}
//		else {
//			throw new OpenCvJException("unknown ColorDepthComposite type=" + type);
//		}
//	}

	public static BackgroundModel createBackgroundModel(ConfigNode config) {
		BackgroundModel bgModel;
		
		String type = config.get("type").asString(null);
		if ( type.equals("mvavg_depth") ) {
			bgModel = MADepthBackgroundModel.create(config);
		}
		else if ( type.equals("mvavg_color") ) {
			bgModel = MAColorBackgroundModel.create(config);
		}
		else {
			throw new OpenCvJException("unknown BackgroundModel: type=" + type);
		}
		BG_MODELS.put(type, bgModel);
		
		return bgModel;
	}
	
	public static BackgroundModel getBackgroundModel(ConfigNode config) {
		ConfigNode bgModelConfig = config.get("bgmodel");
		bgModelConfig = (bgModelConfig.isMissing()) ? config : bgModelConfig.asReference();
		BackgroundModel bgModel = BG_MODELS.get(bgModelConfig.getPath());
		if ( bgModel == null ) {
			bgModel = createBackgroundModel(bgModelConfig);
		}
		
		return bgModel;
	}

	public static ForegroundDetector createForegroundDetector(ConfigNode config) {
		BackgroundModel bgModel = getBackgroundModel(config);
		if ( bgModel instanceof MADepthBackgroundModel ) {
			return MADepthForegroundDetector.create(config);
		}
		else if ( bgModel instanceof MAColorBackgroundModel ) {
			return MAColorForegroundDetector.create(config);
		}
		else {
			throw new OpenCvJException("unknown ForegroundDetector: class=" + bgModel.getClass());
		}
	}
	
	public static Backprojector createBackprojector(ConfigNode config) {
		String type = config.get("type").asString();
		if ( "depth_delta".equals(type) ) {
			DeltaAwareForegroundDetector fgDetector = (DeltaAwareForegroundDetector)createForegroundDetector(config);
			return DeltaDepthBackprojector.create(fgDetector, config);
		}
		else if ( "hue".equals(type) ) {
			return HueBackprojector.create(config);
		}
		else {
			throw new OpenCvJException("unknown Backprojector: type=" + type);
		}
	}

	public static ImageThreshold createImageThreshold(ConfigNode config) {
		String type = config.get("type").asString(null);
		if ( type == null || type.equals("simple") ) {
			return SimpleImageThreshold.create(config);
		}
		else if ( "adaptive".equals(type) ) {
			return AdaptiveImageThreshold.create(config);
		}
		else if ( "hue".equals(type) ) {
			return HueThreshold.create(config);
		}
		
		throw new OpenCvJException("unknown ImageThreshold: type=" + type);
	}
	
	public static ImageStore createImageStore(File storeDir) throws Exception {
		ImageStore imageStore = new ImageStore();
		imageStore.setStoreDir(storeDir);
		imageStore.initialize();
		
		return imageStore;
	}
	
	public static final ObjectTemplateStore createTemplateStore(ImageStore imageStore, String type)
		throws Exception {
		String dbName = String.format("%s_features.db", type);
		File tmpltDb = new File(getDataDir(), dbName);
	
		String feature = type;
		String desc = type;
		if ( type.equalsIgnoreCase("sift") || type.equalsIgnoreCase("surf")
			|| type.equalsIgnoreCase("orb") ) {
			feature = type;
			desc = type;
		}
		else {
			throw new OpenCvJException("invalid feature type: " + type);
		}
		
		ObjectTemplateStore tmpltStore = new ObjectTemplateStore();
		tmpltStore.setImageStore(imageStore);
		tmpltStore.setTemplateStore(tmpltDb);
		tmpltStore.setFeatureDetector(feature);
		tmpltStore.setDescriptorExtractor(desc);
		tmpltStore.initialize();
		if ( tmpltStore.isUpdated() ) {
			tmpltStore.save();
		}
		
		return tmpltStore;
	}
}
