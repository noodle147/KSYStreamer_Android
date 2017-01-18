/**
 * TuSDKVideo
 * LiveVideoCamera.java
 *
 * @author		Yanlin
 * @Date		10:41:43 AM
 * @Copright	(c) 2015 tusdk.com. All rights reserved.
 *
 */
package tusdk.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.widget.RelativeLayout;

import com.ksyun.media.streamer.framework.ImgBufFrame;
import com.ksyun.media.streamer.framework.SrcPin;

import org.lasque.tusdk.core.TuSDKStreamingConfiguration;
import org.lasque.tusdk.core.seles.video.SelesSurfaceEncoderInterface;
import org.lasque.tusdk.core.struct.TuSdkSize;
import org.lasque.tusdk.core.utils.TLog;
import org.lasque.tusdk.core.utils.hardware.CameraConfigs.CameraFacing;
import org.lasque.tusdk.core.utils.hardware.TuSdkLiveVideoCamera;

import java.nio.ByteBuffer;

/**
 * LiveVideoCamera.java
 *
 * @author Yanlin
 *
 */
@SuppressWarnings("deprecation")
public class LiveVideoCamera extends TuSdkLiveVideoCamera implements PreviewCallback
{
	private LiveVideoEncoder liveVideoEncoder;

	private TuSDKStreamingConfiguration mConfig;

	/** Capture Photo As Bitmap Callback */
	public interface CapturePhotoAsBitmapCallback
	{
		/** Capture Photo As Bitmap */
		void onCapturePhotoAsBitmap(Bitmap bitmap);
	}
	
	// 存放 YUV 帧数据
	private ByteBuffer yuvFrameBuffer;
	
	//------------ ONLY FOR TESTING ---------------
	public CapturePhotoAsBitmapCallback callback;
	
	// 正在更新缩略视图
	private boolean updatingThumbView = false;
	
	//------------ ONLY FOR TESTING ---------------
	
	/**
	 * @param context
	 * @param facing
	 */
	public LiveVideoCamera(Context context, CameraFacing facing, RelativeLayout holderView)
	{	
		super(context, facing, holderView);
	}

	public void initParams(TuSDKStreamingConfiguration config) {
		mConfig = config;
		// 配置视频输出参数
		// SDK 会根据设置自动调整预览尺寸、帧率
		// 设置后请勿再更改
		this.setStreamingConfiguration(config);

		initOutputSettings();

		//
		// 设置为横屏
		// this.setOutputImageOrientation(InterfaceOrientation.PortraitUpsideDown);
		// 水平镜像前置摄像头
		this.setHorizontallyMirrorFrontFacingCamera(true);
		// 禁用前置摄像头自动水平镜像 (默认: false，前置摄像头拍摄结果自动进行水平镜像)
		this.setDisableMirrorFrontFacing(false);

		//-----------------------------------------

		if (this.isOuputEncodedStream() == false)
		{
			// 关闭智能美颜，提升性能
			this.setEnableFaceAutoBeauty(false);

			this.setFrameDelegate(mFrameDelegate);
		}
	}
	
	/**
	 * 是否直接输出编码好的视频流。
	 * 如果为 true，直接输出经过 MediaCodec 编码后的视频流；
	 * 如果为 false，输出每桢的数据，格式为：RGB | NV21，自行去做编码
	 * 
	 * @return
	 */
	public boolean isOuputEncodedStream()
	{
		// 默认为 true，直接输出编码好的视频流
		_isOuputEncodedStream = true;
		
		return _isOuputEncodedStream;
	}

	/**
	 * 编码器
	 * 
	 * @return 编码器对象
	 */
	@Override
	protected SelesSurfaceEncoderInterface getVideoEncoder() 
	{
		if (isOuputEncodedStream() && mSurfaceEncoder == null)
		{
			// 初始化编码器
			liveVideoEncoder = new LiveVideoEncoder();
			mSurfaceEncoder = liveVideoEncoder;
		}
		
		return mSurfaceEncoder;
	}
	
	/**
	 * 配置视频输出参数
	 * 
	 * @return
	 */
	private TuSDKStreamingConfiguration getVideoStreamingConfiguration()
	{
		return mConfig;
	}
	
	@Override
	protected void onCameraStarted()
	{
		super.onCameraStarted();
		
		// 开机启动美颜
		// switchFilter("VideoFair");
	}
	
	/**
	 * 初始化相机
	 */
	@Override
	protected void onInitConfig(Camera camera)
	{
		super.onInitConfig(camera);		
		
		// Parameters mParams = camera.getParameters();
		// 这里配置相机参数
		
		// camera.setParameters(mParams);	
	}
	
	//----------------------------------------------------

	/**
	 * _isOuputEncodedStream = false 时才会调用，输出每桢的数据，RGB ｜ NV21
	 */
	private TuSdkLiveVideoCameraDelegate mFrameDelegate = new TuSdkLiveVideoCameraDelegate()
	{
		
		@Override
		public void onFrameReady()
		{
			TLog.d("YUV frame data ready");

			TuSdkSize out = getVideoStreamingConfiguration().videoSize;
			int capacity = out.width * out.height * ImageFormat.getBitsPerPixel(ImageFormat.NV21)/8;
			if (yuvFrameBuffer == null) {
//				yuvFrameBuffer = ByteBuffer.allocate(capacity);
				yuvFrameBuffer = ByteBuffer.allocateDirect(capacity);
			}
			
			yuvFrameBuffer.position(0);
			
			if (updatingThumbView)
			{
				TLog.d("drop ---- ");
				return;
			}
			
			// 原始桢数据为RGB格式，这里转换为 YUV (NV21) 格式
			// 调用 convertFrameData 时，如果帧转换成功，则会将数据写入 yuvFrameBuffer 中，否则不会修改。
			convertFrameData(yuvFrameBuffer.array(), true);

			// 将转换后的 yuvFrameBuffer 用于视频编码
			// 软编：编码 -> 推流
			
			// --------------------------------------------------
			// 以下为测试代码
			//---------------------------------------------------
			// 显示输出画面，仅用于测试
			
			// updatingThumbView = true;
			// showAsBitmap(yuvFrameBuffer.array());
		}
	};

	/*
	private void showOrignalData(final int[] data)
	{
		ThreadHelper.runThread(new Runnable()
		{
			@Override
			public void run()
			{
				TuSdkSize out = getVideoStreamingConfiguration().videoSize;
				
				int pixelCount = out.width*out.height;
				
				int[] colors = new int[pixelCount];

		        for (int i = 0; i < pixelCount; i++) {
		            int c = data[i];
		            colors[i] = (c & 0xff00ff00) | ((c & 0x00ff0000) >> 16) | ((c & 0x000000ff) << 16);
		        }
				
				TLog.d("out.size: %s", out.toString());
				
				final Bitmap bitmap = Bitmap.createBitmap(colors, out.width, out.height, Bitmap.Config.ARGB_8888);
				
				ThreadHelper.post(new Runnable()
				{
					@Override
					public void run()
					{
						if (callback != null)
						{
							callback.onCapturePhotoAsBitmap(bitmap);
						}
						
						updatingThumbView = false;
					}
				});
			}
		});
	}

	private void showAsBitmap(final byte[] data)
	{
		ThreadHelper.runThread(new Runnable()
		{
			@Override
			public void run()
			{
			
				TuSdkSize out = getVideoStreamingConfiguration().videoSize;
				
				YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, out.width, out.height, null);
				
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				
				yuvImage.compressToJpeg(new Rect(0, 0, out.width, out.height), 100, baos);
				
				final Bitmap result = BitmapHelper.imageDecode(baos.toByteArray(), true);
				
				try {
					baos.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		        
				ThreadHelper.post(new Runnable()
				{
					@Override
					public void run()
					{
						if (callback != null)
						{
							callback.onCapturePhotoAsBitmap(result);
						}
						
						updatingThumbView = false;
					}
				});
			}
		});
	}
	*/

	public SrcPin<ImgBufFrame> getSrcPin() {
		if (liveVideoEncoder == null)
			return null;

		return liveVideoEncoder.getSrcPin();
	}

	public long getEncodedFrames() {
		if (liveVideoEncoder == null)
			return 0;

		return liveVideoEncoder.getEncodedFrames();
	}

	public void sendExtraData() {
		if (liveVideoEncoder != null) {
			liveVideoEncoder.sendExtraData();
		}
	}
}
