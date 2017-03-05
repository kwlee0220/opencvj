package opencvj.blob;

import camus.service.camera.ImageProxy;


/**
 * 
 * @author Kang-Woo Lee
 */
public final class DeltaAwareBackground {
	public ImageProxy m_fgMask;
	public ImageProxy m_delta;
	
	public DeltaAwareBackground(ImageProxy mask, ImageProxy delta) {
		m_fgMask = mask;
		m_delta = delta;
	}
}