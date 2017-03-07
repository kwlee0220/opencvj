package opencvj.camera;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.opencv.core.Mat;
import org.opencv.core.Size;

import config.Config;
import net.jcip.annotations.GuardedBy;
import utils.Duration;
import utils.Initializable;
import utils.UninitializedException;
import utils.Utilities;
import utils.io.IOUtils;
import utils.thread.ExecutorAware;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class OpenCvJCameraFactoryImpl implements OpenCvJCameraFactory, Initializable, ExecutorAware {
	private static final Logger s_logger = Logger.getLogger("RESULT_SHARING");
	private static final Duration MAX_CAPTURE_WAIT = Duration.parseDuration("3s");

	// properties (BEGIN)
	private volatile OpenCvJCamera m_source;
	private volatile long m_maxWaitMillis = -1;		// default: infinite wait
	@GuardedBy("m_mutex") private long m_interval = -1;
	private volatile boolean m_isOwner = true;
	private volatile Config m_config;
	private volatile Executor m_executor;			// optional
	// properties (END)
	
	private final ReentrantLock m_factLock = new ReentrantLock();
	private final Condition m_imageReadyCond = m_factLock.newCondition();
	private final Condition m_cameraOpenCond = m_factLock.newCondition();
	private final Condition m_producerCond = m_factLock.newCondition();
	@GuardedBy("m_factLock") private boolean m_isProducing;
	@GuardedBy("m_factLock") private final Mat m_sharedImage = new Mat();
	@GuardedBy("m_factLock") private RuntimeException m_cause;
	@GuardedBy("m_factLock") private final List<SharedOpenCvJCamera> m_shareds
														= new ArrayList<SharedOpenCvJCamera>();
	
	public static final OpenCvJCameraFactoryImpl create(OpenCvJCamera source, Config config) throws Exception {
		OpenCvJCameraFactoryImpl fact = new OpenCvJCameraFactoryImpl();
		fact.setSourceCamera(source);
		fact.setConfig(config);
		fact.initialize();
		
		return fact;
	}
	
	public OpenCvJCameraFactoryImpl() {
	}
	
	public void setSourceCamera(OpenCvJCamera producer) {
		m_source = producer;
	}
	
	public void setSourceCameraOwnership(boolean flag) {
		m_isOwner = flag;
	}
	
	public final void setConfig(Config config) {
		m_config = config;
	}
	
//	public final void setConfig(String configStr) {
//		m_config = new Config(configStr);
//	}

	@Override
	public void setExecutor(Executor executor) {
		m_executor = executor;
	}

	@Override
	public void initialize() throws Exception {
		if ( m_source == null ) {
			throw new UninitializedException("Property 'sourceCamera' was not set, class="
											+ getClass().getName());
		}
		if ( m_config == null ) {
			throw new UninitializedException("Property 'config' was not set, class="
					+ getClass().getName());
		}
		
		m_maxWaitMillis = m_config.get("max_capture_wait").asDuration(MAX_CAPTURE_WAIT).asMillis();
		m_interval = m_config.get("capture_interval").asDuration().asMillis();
	}
	
	public void destroy() {
		// close all the spawned camera and wait until they are closed
		//
		Utilities.executeAsynchronously(m_executor, new Runnable() {
			@Override
			public void run() {
				List<SharedOpenCvJCamera> shareds = new ArrayList<SharedOpenCvJCamera>();
				m_factLock.lock();
				try {
					shareds.addAll(m_shareds);
				}
				finally {
					m_factLock.unlock();
				}
				
				for ( SharedOpenCvJCamera shared: shareds ) {
					try {
						shared.close();
					}
					catch ( Exception ignored ) { }
				}
			}
		});

		m_factLock.lock();
		try {
			try {
				while ( m_shareds.size() > 0 ) {
					m_cameraOpenCond.await();
				}
			}
			catch ( InterruptedException e ) { }

			m_sharedImage.release();
			if ( m_isOwner ) {
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

	@Override
	public Executor getExecutor() {
		return m_executor;
	}
	
	public OpenCvJCamera createCamera() {
		return new SharedOpenCvJCamera(this);
	}

	public Size getSize() {
		return m_source.getSize();
	}

	public void capture(Mat image) throws InterruptedException {
		m_factLock.lock();
		try {
			// 이미 다른 카메라에 의해 이미지 capturing 중이면
			// 해당 영상이 capture될 때까지 대기 capture가 끝나면 그 영상을 반환한다.
			if ( m_isProducing ) {
				try {
					waitUntilProducedInGuard();
				}
				catch ( InterruptedException e ) {
					throw e;
				}
				
				if ( m_cause != null ) {
					throw m_cause;
				}
				
				if ( s_logger.isDebugEnabled() ) {
					s_logger.debug("use pre-produced result");
				}
				
				m_sharedImage.copyTo(image);
				
				return;
			}
			else {
				// 영상이 capturing 중임을 알려 다른 쓰레드가 추가로 capture하지 못하도록 한다.
				m_isProducing = true;
			}
		}
		finally {
			m_factLock.unlock();
		}

		RuntimeException cause = null;
		
		long started = System.currentTimeMillis();
		try {
			m_source.capture(m_sharedImage);
		}
		catch ( RuntimeException e ) {
			cause = e;
		}

		m_factLock.lock();
		try {
			if ( cause == null ) {
				// 실제 capture 소요시간이 지정된 interval 보다 짧은 경우는 남은 시간 동안
				// 대기하여 보다 많은 쓰레드가 영상을 공유할 수 있도록 한다.
				//
				m_producerCond.awaitUntil(new Date(started + m_interval));
				
				m_isProducing = false;
				m_imageReadyCond.signalAll();
				m_sharedImage.copyTo(image);
			}
			else {
				m_isProducing = false;
				m_imageReadyCond.signalAll();
				m_cause = cause;
				
				throw m_cause;
			}
		}
		finally {
			m_factLock.unlock();
		}
	}
	
	void onSharedCameraOpened(SharedOpenCvJCamera shared) {
		m_factLock.lock();
		try {
			m_shareds.add(shared);
			if ( m_shareds.size() == 1 ) {
				m_source.open();
				
				m_cameraOpenCond.signalAll();
			}
		}
		finally {
			m_factLock.unlock();
		}
	}
	
	void onSharedCameraClosed(SharedOpenCvJCamera shared) {
		m_factLock.lock();
		try {
			m_shareds.remove(shared);
			if ( m_shareds.size() == 0 ) {
				IOUtils.closeQuietly(m_source);
				
				m_cameraOpenCond.signalAll();
			}
		}
		finally {
			m_factLock.unlock();
		}
	}

	@GuardedBy("m_factLock")
	private void waitUntilProducedInGuard() throws InterruptedException {
		long started = System.currentTimeMillis();
		
		Date due = new Date(started + Math.max(m_interval, m_maxWaitMillis));
		if ( !m_imageReadyCond.awaitUntil(due) ) {
			s_logger.fatal("FAILS TO GET AN IMAGE FROM CAMERA_PROVIDER");
			System.err.println("FAILS TO GET AN IMAGE FROM CAMERA_PROVIDER");
			throw new RuntimeException("fails to get an image from " + getClass().getName());
		}
	}
}
