/**
 * TuSDKVideoDemo
 * LiveVideoEncoder.java
 *
 * @author		Yanlin
 * @Date		Jul 15, 2016 2:50:46 PM
 * @Copright	(c) 2015 tusdk.com. All rights reserved.
 *
 */
package tusdk.camera;

import android.graphics.SurfaceTexture;

import com.ksyun.media.streamer.framework.ImgBufFrame;
import com.ksyun.media.streamer.framework.SrcPin;

import org.lasque.tusdk.core.TuSDKStreamingConfiguration;
import org.lasque.tusdk.core.seles.video.SelesSurfaceTextureEncoder;

/**
 * LiveVideoEncoder.java
 *
 * Surface to Surface 硬编
 *
 * @author Yanlin
 *
 */
public class LiveVideoEncoder extends SelesSurfaceTextureEncoder
{
	private H264VideoCodec h264VideoCodec;

	public LiveVideoEncoder() {
		h264VideoCodec = new H264VideoCodec();
	}

	@Override
	protected void prepareEncoder(TuSDKStreamingConfiguration config)
	{
		mVideoEncoder = h264VideoCodec;
		mVideoEncoder.initCodec(config);
	}

	/**
	 * 获取时间戳
	 *
	 * @param surfaceTexture
	 *            用于获取相机纹理的SurfaceTexture
	 * @return 时间戳
	 */
	protected long getTimestamp(SurfaceTexture surfaceTexture)
	{
		long timestamp = surfaceTexture.getTimestamp();

		// 在某些机型上，该数字始终为 0
		// 这里可以根据需要选择系统时间戳来同步
		if (timestamp <= 0)
		{
			timestamp = System.nanoTime();
		}

		return timestamp;
	}

	public SrcPin<ImgBufFrame> getSrcPin() {
		return h264VideoCodec.getSrcPin();
	}

	public long getEncodedFrames() {
		if (h264VideoCodec == null)
			return 0;

		return h264VideoCodec.getEncodedFrames();
	}

	public void sendExtraData() {
		if (h264VideoCodec != null) {
			h264VideoCodec.sendExtraData();
		}
	}
}