# 金山云`屏幕直播_Android`使用说明
Android 5.0 提供了[MediaProjectionManager](https://developer.android.com/reference/android/media/projection/MediaProjectionManager.html)相关类来支持录屏功能  ,金山云`屏幕直播_Android`就是基于Android 5.0的该功能开发的。

`屏幕直播_Android`是基于[KSYStreamer](https://github.com/ksvc/KSYStreamer_Android)组件化方式开发完成，其用方法基本同KSYStreamer

`屏幕直播_Android`直接open录屏相关及组件化方式的源码，开发者可以直接以lib库的形式在Demo中引入。

## 1. KSYScreenStreamer_Android 工程介绍：
* libscreenstreamer：录屏libs库，依赖KSYStreamer提供录屏功能类及kit类
* demo：录屏demo示例工程
* libscreenstreamer/libs: 集成KSYStreamer的SDK需要的所有库文件
    * libs/[armeabi-v7a|arm64-v8a|x86]: 各平台的so库
    * libs/ksylive4.0.jar: 推流SDK jar包
    * libs/libksystat.jar: 金山云统计模块

## 2、功能特点
* ✓支持软编、硬编模式（不支持[KSYStreamer](https://github.com/ksvc/KSYStreamer_Android)的软编兼容模式）
* ✓网络自适应，可根据实际网络情况动态调整目标码率，保证流畅性
* ✓音频编码：AAC
* ✓视频编码：H.264
* ✓推流协议：RTMP
* ✓支持设置推流分辨率
* ✓支持添加摄像头窗口
    * ✓支持摄像头窗口美颜滤镜（包括[KSYStreamer](https://github.com/ksvc/KSYStreamer_Android)的[自定义美颜](https://github.com/ksvc/KSYStreamer_Android/wiki/Video_Filter)和[内置美颜](https://github.com/ksvc/KSYStreamer_Android/wiki/Video_Filter_Inner)）
    * ✓支持摄像头窗口分辨率及帧率设置
    * ✓支持摄像头窗口前后摄像头切换
    * ✓支持通过GLSurfaceView或TextureView进行摄像头窗口预览
    * ✓支持摄像头窗口根据顶层App横竖屏动态切换摄像头的横竖屏
    * ×不支持触摸对焦、变焦、闪光灯、测光等功能
* ✓支持音视频目标码率设置
* ✓支持固定横屏或固定竖屏推流
* ✓支持图片及时间戳水印
* ✓支持音频美声及滤镜
* ×不支持背景音乐功能
* ×不支持纯音频推流

## 3. 运行环境
* 最低支持版本为Android 5.0 (API level 21)
* 支持的cpu架构：armv7, arm64, x86

## 4. 版本迭代

## 5. 快速集成
快速集成方式基本同[KSYStreamer](https://github.com/ksvc/KSYStreamer_Android)的集成方式。
如果您是KSYStreamer的用户，只需要关注（5.2）部分，其它均与KSYStreamer的集成方式相同
具体可以参考demo工程中的相应文件。

### 5.1 配置项目
#### 5.1.1 工程引入
以Android Studio为例：
1. copy工程libscreenstreamer到目标工程目录下面
2. 在目标apk所在的工程中引入libscreenstreamer，引入方式参考：
```java
dependencies {
    ...
    compile project(':libscreenstreamer')
   ...
}
```
#### 5.1.2 在AndroidManifest.xml文件中申请相应权限
```java
<!-- 使用权限 -->
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.READ_PHONE_SINTERNETWIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.FLASHLIGHT" />
<uses-permission android:name="android.permission.VIBRATE" />
<!-- 硬件特性 -->
<uses-feature android:name="android.hardware.camera" />
<uses-feature android:name="android.hardware.camera.autofocus" />
```
#### 5.1.3 在AndroidManifest.xml文件中添加ScreenCaptureAssistantActivity类声明
```java
        <activity
         android:name="com.ksyun.media.diversity.screenstreamer.capture.ScreenCapture$ScreenCaptureAssistantActivity"
            android:theme="@android:style/Theme.Translucent">
        </activity>
```

### 5.2 简单推流示例
具体可参考demo工程中的 ccom.ksyun.media.diversity.screenstreamer.demo.ScreenActivity 类.

#### 1.创建KSYScreenStreamer实例并配置推流相关参数
```java
// 创建KSYStreamer实例
mScreenStreamer = new KSYScreenStreamer(this);
// 设置推流url（需要向相关人员申请，测试地址并不稳定！）
mScreenStreamer.setUrl("rtmp://test.uplive.ksyun.com/live/{streamName}");
// 设置推流分辨率
mScreenStreamer.setTargetResolution(480, 0);
// 设置推流帧率
mScreenStreamer.setTargetFps(15);
// 设置视频码率，分别为初始平均码率、最高平均码率、最低平均码率，单位为kbps，另有setVideoBitrate接口，单位为bps
mScreenStreamer.setVideoKBitrate(600, 800, 400);
// 设置音频采样率
mScreenStreamer.setAudioSampleRate(44100);
// 设置音频码率，单位为kbps，另有setAudioBitrate接口，单位为bps
mScreenStreamer.setAudioKBitrate(48);
/**
 * 设置编码模式(软编、硬编):
 * StreamerConstants.ENCODE_METHOD_SOFTWARE
 * StreamerConstants.ENCODE_METHOD_HARDWARE
 */
mScreenStreamer.setEncodeMethod(StreamerConstants.ENCODE_METHOD_SOFTWARE);
// 设置横竖屏
mStreamer.setIsLandspace(false);
// 开启推流统计功能
mStreamer.setEnableStreamStatModule(true);
```

#### 2.创建推流事件监听
> 所有回调均运行在KSYStreamer的创建线程，建议在主线程中进行，开发者可以直接在回调中操作 KSYStreamer的相关接口，但不要在这些回调中做任何耗时的操作。

> 所有回调码同KSYStream的[StreamerConstants](https://github.com/ksvc/KSYStreamer_Android/wiki/Info&Error_Listener)的定义

以下回调不会产生：

|        名称    	 |       数值      |       含义      |     msg1      |
|------------------|:----------:|-------------------|-------------------|
|KSY_STREAMER_CAMERA_INIT_DONE|1000|初始化结束|N/A|

> Camera相关的回调只在添加Camera后产生，在KSYScreenStreamer的回调中不会产生

新增以下几种回调：

|        名称    	 |       数值      |       含义      |     msg1      |
|------------------|:----------:|-------------------|-------------------|
|KSY_STREAMER_SCREEN_RECORD_UNSUPPORTED|-2007|不支持录屏推流|N/A|
|KSY_STREAMER_SCREEN_RECORD_PERMISSION_DENIED|-2008|没有获取录屏的权限|N/A|


```java
// 设置Info回调，可以收到相关通知信息
mScreenStreamer.setOnInfoListener(new KSYStreamer.OnInfoListener() {
    @Override
    public void onInfo(int what, int msg1, int msg2) {
        // ...
    }
});
// 设置错误回调，收到该回调后，一般是发生了严重错误，比如网络断开等，
// SDK内部会停止推流，APP可以在这里根据回调类型及需求添加重试逻辑。
mScreenStreamer.setOnErrorListener(new KSYStreamer.OnErrorListener() {
    @Override
    public void onError(int what, int msg1, int msg2) {
        // ...
    }
});
```

#### 3. 开始推流
> 没有KSY_STREAMER_CAMERA_INIT_DONE的回调，因此不需要等待该回调再调用这个接口
```java
mScreenStreamer.startStream();
```

#### 4. 停止推流
```java
mScreenStreamer.stopStream();
```

#### 5. Activity生命周期的回调处理
在onResume和onPause中不需要做特殊处理，只需要在onDestroy中释放资源即可
```java
    @Override
    public void onDestroy() {
        super.onDestroy();
        // 清理相关资源
        mScreenStreamer.release();
    }
```

# 5. 更多
* API接口速查([KSYStreamer](http://ksvc.github.io/KSYStreamer_Android/docs/reference/packages.html))
* [悬浮摄像头窗口使用说明](https://github.com/ksvc/KSYScreenStreamer_Android/wiki/Screen_Streamer_CameraFlowView)
* 其它详细说明，均可直接参考KSYStreamer的[wiki](https://github.com/ksvc/KSYStreamer_Android/wiki)
