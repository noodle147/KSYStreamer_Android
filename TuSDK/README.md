# 金山云 - TuSDK涂图美颜集成

## 一. 功能特点

   在 [金山云推流SDK](https://github.com/ksvc/KSYStreamer_Android)集成了涂图美颜的功能。

## 二. 运行环境

   最低支持版本为Android 4.4

## 三. 开发指南

### 3.1 涂图资源获取
  1. 登录[涂图网站](https://tusdk.com)注册账号
  2. 申请权限：打开 [tusdk.com/video](http://tusdk.com/video) 点击「联系商务」，填写表单，等待权限开通
  3. 进入控制台，创建应用
  4. 管理和打包滤镜资源
  5. 下载资源包，更新工程文件

  如有疑问可与具体负责的工作人员沟通。

### 3.2 金山云推流SDK获取
   请直接从github获取：https://github.com/ksvc/KSYStreamer_Android

### 3.3 demo说明
   - 基于金山云推流SDK的结构，demo将涂图美颜封装成[TTVideoComponent](https://github.com/ksvc/KSYDiversityLive_Android/blob/master/TuSDK/demo/src/main/java/tusdk/TTVideoComponent.java)模块
   - demo定义了新的kit类[KSYTTStreamer](https://github.com/ksvc/KSYDiversityLive_Android/blob/master/TuSDK/demo/src/main/java/tusdk/KSYTTStreamer.java)管理各个模块, 完成直播推流、美颜、录制等功能.

### 3.4 简单推流示例

   具体可参考demo工程中的[CameraActivity](https://github.com/ksvc/KSYDiversityLive_Android/blob/master/TuSDK/demo/src/main/java/com/ksyun/media/streamer/demo/CameraActivity.java).

   - 初始化

 ```java
    GLSurfaceView mCameraPreview = (GLSurfaceView)findViewById(R.id.camera_preview);
    ....
    // 创建KSYTTStreamer实例
    mStreamer = new KSYSTTtreamer(this);
    // 设置预览View
    mStreamer.setDisplayPreview(mCameraPreview);
    // 设置推流url（需要向相关人员申请，测试地址并不稳定！）
    mStreamer.setUrl("rtmp://test.uplive.ksyun.com/live/{streamName}");
    // 设置推流分辨率，可以不同于预览分辨率
    mStreamer.setTargetResolution(360, 640);
    // 设置预览帧率
    mStreamer.setPreviewFps(15);
    // 设置推流帧率，当预览帧率大于推流帧率时，编码模块会自动丢帧以适应设定的推流帧率
    mStreamer.setTargetFps(15);
    // 设置视频码率,单位为bps
    mStreamer.setVideoKBitrate(600);
    // 设置音频采样率
    mStreamer.setAudioSampleRate(44100);
    // 设置音频码率，单位为kbps，另有setAudioBitrate接口，单位为bps
    mStreamer.setAudioKBitrate(48);
    // 设置屏幕的旋转角度，支持 0, 90, 180, 270
    mStreamer.setRotateDegrees(0);
 ```

   - 开始推流
 ```java
    mStreamer.startStream();
 ```

   - 推流开始前及推流过程中可动态设置的常用方法
 ```java
    // 切换前后摄像头
    mStreamer.switchCamera();
    // 开关闪光灯
    mStreamer.toggleTorch(true);
 ```

   - 停止推流
 ```java
    mStreamer.stopStream();
 ```

   - Activity生命周期的回调处理
  ```java
    public class CameraActivity extends Activity {

        // ...

        @Override
        public void onResume() {
            super.onResume();
            // 一般可以在onResume中开启摄像头预览
            startCameraPreviewWithPermCheck();
            // 调用KSYStreamer的onResume接口
            mStreamer.onResume();
        }

        @Override
        public void onPause() {
            super.onPause();
            mStreamer.onPause();
            // 一般在这里停止摄像头采集
            mStreamer.stopCameraPreview();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            // 清理相关资源
            mStreamer.release();
        }
    }
  ```

## 四. 反馈与建议
- 主页：[金山云](http://www.ksyun.com/)
- 邮箱：<zengfanping@kingsoft.com>
- QQ讨论群：574179720
- Issues: <https://github.com/ksvc/KSYDiversityLive_Android/issues>