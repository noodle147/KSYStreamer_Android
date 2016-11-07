# 金山云`录屏直播_Android`使用说明
* `录屏直播_Android`是基于Android 5.0的录屏功能开发
  官方资料参考：[MediaProjectionManager](https://developer.android.com/reference/android/media/projection/MediaProjectionManager.html)相关类。

* `录屏直播_Android`是基于KSYStreamer的组件化功能开发完成，其使用方法基本同[KSYStreamer](https://github.com/ksvc/KSYStreamer_Android)

* `录屏直播_Android`直接open录屏相关及组件化方式的源码，开发者可以直接以lib库的形式在目标工程中引入。

## 1. 录屏组件化集成介绍
KSYStreamer 是金山云推流SDK提供的kit类，集成了摄像头推流所需的积木类，其组件化集成结构图如下：
<img src="https://raw.githubusercontent.com/wiki/ksvc/KSYStreamer_Android/images/ksystreamer_connect.png" width = "1105" height = "710" alt="图片名称" align=center />

KSYScreenStreamer是libscreenstreamer基于金山云推流SDK提供的kit类，集成了录屏推流所需的积木类，其组件化集成结构图如下：
<img src="https://raw.githubusercontent.com/wiki/ksvc/KSYStreamer_Android/images/screen_connect.png" width = "1105" height = "710" alt="图片名称" align=center />

## 2. KSYScreenStreamer_Android 工程介绍
* libscreenstreamer：录屏libs库，依赖KSYStreamer提供录屏功能类及kit类
* demo：录屏demo示例工程

工程结构图：

<img src="https://raw.githubusercontent.com/wiki/ksvc/KSYStreamer_Android/images/screen_package.png" width = "799.3" height = "614" alt="图片名称" align=center />

## 3. 关键接口介绍
* ScreenCaptureAssistantActivity:接收录屏权限申请回调，触发录屏开始，需要在AndroidManifest中声明该类
* KSYScreenStreamer.OnInfoListener:推流状态回调，无KSY_STREAMER_CAMERA_INIT_DONE回调，无Camera相关回调
* KSYScreenStreamer.OnErrorListener:推流错误回调，增加KSY_STREAMER_SCREEN_RECORD_XXX相关错误回调
* KSYCameraPreview.OnInfoListener:悬浮窗口打开时，摄像头状态回调
* KSYCameraPreview.OnErrorListener:悬浮窗口打开时，摄像头错误回调
* kit 类关键接口介绍：
<img src="https://raw.githubusercontent.com/wiki/ksvc/KSYStreamer_Android/images/screen_interface.png" width = "636" height = "426" alt="图片名称" align=center />

## 4、功能特点
基于KSYStreamer4.x，详细参考：[KSYStramer说明](https://github.com/ksvc/KSYStreamer_Android/wiki)
此外，存在以下不同点
* kit类中未集成[KSYStreamer](https://github.com/ksvc/KSYStreamer_Android)的软编兼容模式
* ✓支持添加摄像头窗口，对摄像头的支持同KSYStreamer4.x，但是存在以下差异：
    * ✓支持摄像头窗口根据顶层App横竖屏动态切换摄像头的横竖屏
    * ×kit类中未集成触摸对焦、变焦、闪光灯、测光等功能
* ×kit类中未集成背景音乐功能
* ×kit类中未集成纯音频推流

## 3. 运行环境
* 最低支持版本为Android 5.0 (API level 21)
* 支持的cpu架构：armv7, arm64, x86

## 4. 快速集成
快速集成方式基本同[KSYStreamer](https://github.com/ksvc/KSYStreamer_Android/wiki)的集成方式。
具体可以参考demo工程中README及相应文件。

## 5. 更多
* [悬浮摄像头窗口使用说明](https://github.com/ksvc/KSYDiversityLive_Android/wiki/Screen_Streamer_CameraFlowView)

## 6. 效果展示
* 竖屏（含Camera悬浮窗口）：
<img src="https://raw.githubusercontent.com/wiki/ksvc/KSYStreamer_Android/images/Screen_game_p.png" width = "180" height = "320" alt="图片名称" align=center />
* 横屏（含Camera悬浮窗口）：
<img src="https://raw.githubusercontent.com/wiki/ksvc/KSYStreamer_Android/images/Screen_play_l.png" width = "320" height = "180" alt="图片名称" align=center />
