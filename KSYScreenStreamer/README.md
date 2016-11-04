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

工程结构图：

<img src="https://raw.githubusercontent.com/wiki/ksvc/KSYStreamer_Android/images/screen_package.png" width = "799.3" height = "614" alt="图片名称" align=center />

## 2、功能特点
基于KSYStreamer4.x，详细参考：[KSYStramer说明](https://github.com/ksvc/KSYStreamer_Android/wiki)
此外，存在以下不同点
* 不支持[KSYStreamer](https://github.com/ksvc/KSYStreamer_Android)的软编兼容模式
* 不支持设置预览分辨率
* ✓支持添加摄像头窗口，对摄像头的支持同KSYStreamer4.x，但是存在以下差异：
    * ✓支持摄像头窗口根据顶层App横竖屏动态切换摄像头的横竖屏
    * ×不支持触摸对焦、变焦、闪光灯、测光等功能
* ×不支持背景音乐功能
* ×不支持纯音频推流

## 3. 运行环境
* 最低支持版本为Android 5.0 (API level 21)
* 支持的cpu架构：armv7, arm64, x86

## 4. 快速集成
快速集成方式基本同[KSYStreamer](https://github.com/ksvc/KSYStreamer_Android/wiki)的集成方式。
具体可以参考demo工程中README及相应文件。

## 5. 更多
* [悬浮摄像头窗口使用说明](https://github.com/ksvc/KSYScreenStreamer_Android/wiki/Screen_Streamer_CameraFlowView)

## 6. 效果展示
* 竖屏（含Camera悬浮窗口）：
<img src="https://raw.githubusercontent.com/wiki/ksvc/KSYStreamer_Android/images/Screen_game_p.png" width = "180" height = "320" alt="图片名称" align=center />
* 横屏（含Camera悬浮窗口）：
<img src="https://raw.githubusercontent.com/wiki/ksvc/KSYStreamer_Android/images/Screen_play_l.png" width = "320" height = "180" alt="图片名称" align=center />