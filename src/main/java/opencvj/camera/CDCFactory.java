package opencvj.camera;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.opencv.core.Mat;
import org.opencv.core.Size;

import config.Config;
import net.jcip.annotations.GuardedBy;
import utils.CheckedSupplier;
import utils.Initializable;
import utils.UninitializedException;
import utils.Utilities;
import utils.io.IOUtils;
import utils.thread.ExecutorAware;
import utils.thread.InterThreadShareSupplier;




/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class CDCFactory implements ColorDepthCompositeFactory, ExecutorAware, Initializable {
	private static final long MAX_CAPTURE_WAIT_MILLIS = 3*1000;
	
	// properties (BEGIN)
	private volatile ColorDepthComposite m_source;
	private volatile boolean m_owner = true;
	private volatile Config m_config;
	private volatile Executor m_executor;			// optional
	// properties (END)
	
	private InterThreadShareSupplier<ImageComposite> m_sharedImageSupplier;
	private OpenCvJCameraFactoryImpl m_colorFact;
	private OpenCvJCameraFactoryImpl m_depthFact;
	
	private final ReentrantLock m_factLock = new ReentrantLock();
	private final Condition m_cond = m_factLock.newCondition();
	@GuardedBy("m_factLock") private final ImageComposite m_images = new ImageComposite();
	@GuardedBy("m_factLock") private final List<SharedCDC> m_shareds = new ArrayList<SharedCDC>();
	
	private final CheckedSupplier<ImageComposite> m_imageProducer = () -> {
		m_source.captureSynched(m_images.m_colorImage, m_images.m_depthImage);
		return m_images;
	};
	
	static class ImageComposite implements AutoCloseable {
		Mat m_colorImage = new Mat();
		Mat m_depthImage = new Mat();
		
		@Override
		public void close() {
			m_colorImage.release();
			m_depthImage.release();
		}
	}
	
	public static final CDCFactory create(ColorDepthComposite source, Config config) throws Exception {
		CDCFactory fact = new CDCFactory();
		fact.setSourceCdc(source);
		fact.setConfig(config);
		fact.initialize();
		
		return fact;
	}
	
	public CDCFactory() {
		m_sharedImageSupplier = new InterThreadShareSupplier<>(m_imageProducer);
	}
	
	public void setSourceCdc(ColorDepthComposite source) {
		m_source = source;
	}
	
	public void setSourceOwnership(boolean flag) {
		m_owner = flag;
	}
	
	public final void setConfig(Config config) {
		m_config = config;
	}
	
//	public final void setConfig(String configStr) {
//		m_config = new OpenCvJConfig(configStr);
//	}

	@Override
	public void setExecutor(Executor executor) {
		m_executor = executor;
	}

	@Override
	public void initialize() throws Exception {
		if ( m_source == null ) {
			throw new UninitializedException("Property 'source' was not set, class="
											+ getClass().getName());
		}
		if ( m_config == null ) {
			throw new UninitializedException("Property 'config' was not set, class="
					+ getClass().getName());
		}
		
		Config maxWaitConfig = m_config.get("max_capture_wait");
		if ( !maxWaitConfig.isMissing() ) {
			m_sharedImageSupplier.setMaxWaitMillis(maxWaitConfig.asDuration().asMillis());
		}
		
		m_colorFact = OpenCvJCameraFactoryImpl.create(m_source.getColorCamera(),
														m_config.get("color"));
		m_depthFact = OpenCvJCameraFactoryImpl.create(m_source.getDepthCamera(),
														m_config.get("depth"));
	}
	
	@Override
	public void destroy() {
		final List<SharedCDC> shareds = new ArrayList<SharedCDC>();
		m_factLock.lock();
		try {
			shareds.addAll(m_shareds);
		}
		finally {
			m_factLock.unlock();
		}
		
		// close all the spawned camera and wait until they are closed
		//
		Utilities.runAsync(()->{
			for ( SharedCDC shared: shareds ) {
				try {
					shared.close();
				}
				catch ( Exception ignored ) { }
			}
		}, m_executor);

		m_factLock.lock();
		try {
			try {
				while ( m_shareds.size() > 0 ) {
					m_cond.await();
				}
			}
			catch ( InterruptedException e ) { }
	
			m_images.close();
			if ( m_owner ) {
				IOUtils.closeQuietly(m_source);
				
				if ( m_source instanceof Initializable ) {
					((Initializable)m_source).destroyQuietly();
				}
			}
		}
		finally {
			m_factLock.unlock();
		}
	}
	
	ImageComposite capture() throws InterruptedException, TimeoutException, ExecutionException {
		return m_sharedImageSupplier.get();
	}

	@Override
	public Executor getExecutor() {
		return m_executor;
	}
	
	public SharedCDC createColorDepthComposite() {
		return new SharedCDC(this);
	}

	@Override
	public Size getColorImageSize() {
		return m_source.getColorImageSize();
	}

	@Override
	public Size getDepthImageSize() {
		return m_source.getDepthImageSize();
	}

	@Override
	public OpenCvJCameraFactory getColorCameraFactory() {
		return m_colorFact;
	}

	@Override
	public OpenCvJCameraFactory getDepthCameraFactory() {
		return m_depthFact;
	}

	@Override
	public DepthToColorMapper getDepthToColorMapper() {
		return m_source.getDepthToColorMapper();
	}

	@Override
	public ColorToDepthMapper getColorToDepthMapper() {
		return m_source.getColorToDepthMapper();
	}
	
	synchronized void onSharedCDCOpened(SharedCDC shared) {
		m_factLock.lock();
		try {
			m_shareds.add(shared);
			if ( m_shareds.size() == 1 ) {
				m_source.open();
				
				m_cond.signalAll();
			}
		}
		finally {
			m_factLock.unlock();
		}
	}
	
	synchronized void onSharedCDCClosed(SharedCDC shared) {
		m_factLock.lock();
		try {
			m_shareds.remove(shared);
			if ( m_shareds.size() == 0 ) {
				IOUtils.closeQuietly(m_source);
				
				m_cond.signalAll();
			}
		}
		finally {
			m_factLock.unlock();
		}
	}
}
