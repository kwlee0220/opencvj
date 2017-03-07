package opencvj.projector;

import camus.service.geo.Size2d;
import camus.service.vision.Image;

import etri.service.image.SwingBeamProjector;

import org.opencv.core.Mat;
import org.opencv.core.Size;

import opencvj.MatConvas;
import opencvj.Mats;
import opencvj.OpenCvJUtils;
import utils.config.ConfigNode;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class OpenCvBeamProjector extends SwingBeamProjector {
    private static final int DEF_MONITOR_INDEX = -1;
    
	// properties (BEGIN)
	private volatile ConfigNode m_config;
	// properties (END)
	
	private Size m_size;
	
	public static OpenCvBeamProjector create(ConfigNode config) throws Exception {
		OpenCvBeamProjector prj = new OpenCvBeamProjector();
		prj.setConfig(config);
		prj.initialize();
		
		return prj;
	}
	
	public static OpenCvBeamProjector createMockProjector(int monitorIndex, Size2d size)
		throws Exception {
		OpenCvBeamProjector prj = new OpenCvBeamProjector();
		prj.setMonitorIndex(monitorIndex);
		prj.setViewSize(size);
		prj.setFrameDecoration(true);
		prj.initialize();
		
		return prj;
	}
	
	public OpenCvBeamProjector() { }
	
	public final void setConfig(ConfigNode config) {
		m_config = config;
	}
	
//	public final void setConfig(String configStr) {
//		m_config = new Config(configStr);
//	}
	
	@Override
	public void initialize() throws Exception {
		if ( m_config != null ) {
			int monitorIndex = m_config.get("monitor_index").asInt(DEF_MONITOR_INDEX);
			if ( monitorIndex >= 0 ) {
				super.setMonitorIndex(monitorIndex);
			}
		}
		super.initialize();
		
		m_size = OpenCvJUtils.toCvSize(getScreenSize());
	}

	@Override
	public void show(Image image) {
		show(Mats.toBufferedImage(image));
	}

	public void show(Mat image) {
		show(Mats.toBufferedImage(image));
	}

	public void show(MatConvas convas) {
		show(convas.getMat());
	}
	
	public Size getSize() {
		return m_size;
	}
}
