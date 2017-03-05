package opencvj.camera;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.opencv.core.Mat;
import org.opencv.core.Size;

import opencvj.OpenCvJException;
import utils.ExceptionUtils;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
class SharedCDC implements ColorDepthComposite {
	private final CDCFactory m_cdcFactory;
	
	SharedCDC(CDCFactory cdcFactory) {
		m_cdcFactory = cdcFactory;
	}

	@Override
	public void open() {
		m_cdcFactory.onSharedCDCOpened(this);
	}

	@Override
	public void close() {
		m_cdcFactory.onSharedCDCClosed(this);
	}

	@Override
	public Size getColorImageSize() {
		return m_cdcFactory.getColorImageSize();
	}

	@Override
	public Size getDepthImageSize() {
		return m_cdcFactory.getDepthImageSize();
	}

	@Override
	public OpenCvJCamera getColorCamera() {
		return m_cdcFactory.getColorCameraFactory().createCamera();
	}

	@Override
	public OpenCvJCamera getDepthCamera() {
		return m_cdcFactory.getDepthCameraFactory().createCamera();
	}

	@Override
	public void captureSynched(Mat colorImage, Mat depthImage) {
		try {
			CDCFactory.ImageComposite comp = m_cdcFactory.capture();
			comp.m_colorImage.copyTo(colorImage);
			comp.m_depthImage.copyTo(depthImage);
		}
		catch ( ExecutionException e ) {
			Throwable cause = ExceptionUtils.unwrapThrowable(e);
			throw new OpenCvJException("sync-capture failed: cause=" + cause);
		}
		catch ( InterruptedException | TimeoutException e ) {
			throw new OpenCvJException("sync-capturing interrupted");
		}
	}

	@Override
	public ColorToDepthMapper getColorToDepthMapper() {
		return m_cdcFactory.getColorToDepthMapper();
	}

	@Override
	public DepthToColorMapper getDepthToColorMapper() {
		return m_cdcFactory.getDepthToColorMapper();
	}
}
