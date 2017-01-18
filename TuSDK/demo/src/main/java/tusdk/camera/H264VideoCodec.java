/**
 * TuSDKVideoDemo
 * H264VideoCodec.java
 *
 * @author		Yanlin
 * @Date		Jul 15, 2016 2:51:04 PM
 * @Copright	(c) 2015 tusdk.com. All rights reserved.
 *
 */
package tusdk.camera;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;

import com.ksyun.media.streamer.framework.AVBufFrame;
import com.ksyun.media.streamer.framework.AVFrameBase;
import com.ksyun.media.streamer.framework.ImgBufFormat;
import com.ksyun.media.streamer.framework.ImgBufFrame;
import com.ksyun.media.streamer.framework.SrcPin;

import org.lasque.tusdk.core.seles.output.SelesVideoDataEncoder;
import org.lasque.tusdk.core.utils.TLog;
import org.lasque.tusdk.core.utils.TuSdkDate;

import java.nio.ByteBuffer;

/**
 * H264VideoCodec.java
 *
 * @author Yanlin
 *
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class H264VideoCodec extends SelesVideoDataEncoder
{
    private final static String TAG = "H264VideoCodec";
	private TuSdkDate mStartingEncodeTime;
	private int mFrameCount = 0;

	private SrcPin<ImgBufFrame> mSrcPin;
	private ImgBufFormat mOutFormat;

	private ImgBufFrame mExtraData;
	private volatile boolean mSendExtraData;

	public H264VideoCodec() {
		super();

		mSrcPin = new SrcPin<>();
	}

	public SrcPin<ImgBufFrame> getSrcPin() {
		return mSrcPin;
	}

	/**
	 * 开始接收编码后的视频流
	 * @param format
	 */
	protected void onEncoderStarted(MediaFormat format)
	{
		int width = format.getInteger("width");
		int height = format.getInteger("height");
		mOutFormat = new ImgBufFormat(ImgBufFormat.FMT_AVC, width, height, 0);

		if (mSrcPin.isConnected()) {
			mSrcPin.onFormatChanged(mOutFormat);
		}
	}

	/**
	 * 处理编码数据
	 * @param encodedData
	 */
	protected void onEncodedFrameDataAvailable(ByteBuffer encodedData)
	{
		if (mStartingEncodeTime == null)
		{
			mStartingEncodeTime = TuSdkDate.create();
		}

		mFrameCount ++;

		long currentFrameTime = mStartingEncodeTime.diffOfMillis();

		TLog.d("%d frames taken: %s ms", mFrameCount, currentFrameTime);

		// 这里进行 RTMP 包封装
		TLog.d("sent " + mBufferInfo.size + " bytes, ts=" +  mBufferInfo.presentationTimeUs);

		ImgBufFrame outFrame = getOutFrame(encodedData, mBufferInfo);

		if (mSendExtraData) {
			if (mExtraData != null && mSrcPin.isConnected()) {
				mSrcPin.onFrameAvailable( mExtraData);
			}
			mSendExtraData = false;
		}
		cacheExtra(outFrame);

		if (mSrcPin.isConnected()) {
			mSrcPin.onFrameAvailable(outFrame);
		}
	}

	private void cacheExtra(ImgBufFrame frame) {
		//cache the frame with FLAG_CODEC_CONFIG flag
		if ((frame.flags & AVFrameBase.FLAG_CODEC_CONFIG) != 0) {
			mExtraData = frame.clone();

			//copy the buffer of the frame to a new buffer, otherwise it will be overwrite
			ByteBuffer buffer = ByteBuffer.allocateDirect(frame.buf.limit());
			buffer.clear();
			buffer.put(frame.buf);
			buffer.flip();
			mExtraData.buf.flip();
			mExtraData.buf = buffer;
		}
	}
	private ImgBufFrame getOutFrame(ByteBuffer buffer, MediaCodec.BufferInfo bufferInfo) {
		ImgBufFrame frame = new ImgBufFrame((ImgBufFormat)mOutFormat, buffer,
				bufferInfo.presentationTimeUs / 1000);
		if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0)
			frame.flags |= AVBufFrame.FLAG_END_OF_STREAM;
		if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0)
			frame.flags |= AVBufFrame.FLAG_KEY_FRAME;
		if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0)
			frame.flags |= AVBufFrame.FLAG_CODEC_CONFIG;
		return frame;
	}

	public long getEncodedFrames() {
		return mFrameCount;
	}

	public void sendExtraData() {
		mSendExtraData = true;
	}
}
