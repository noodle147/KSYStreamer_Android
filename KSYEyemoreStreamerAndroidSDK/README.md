# 金山云`Eyemore_Android`高清摄像头集成使用说明

* `Eyemore_Android`是基于KSYStreamer的组件化功能开发完成，其使用方法基本同[KSYStreamer](https://github.com/ksvc/KSYStreamer_Android)

* `Eyemore_Android`直接open录屏相关及组件化方式的源码，开发者可以直接以lib库的形式在目标工程中引入。

## 1. eyemore是什么？在直播中的优势？
* eyemore是一款高清摄像头，体积小重量轻方便携带，1600万像素，较强的变焦和对焦功能使拍画面具有质感、立体、细节等特点，同时在光线不好的时候，仍然可以还原画面的细节和层次，拍出高质量的画面。给普通用户带来全新的拍摄体验。
* eyemore的特点赋予了其在直播中将有很大应用空间。如在户外做直播时，通过调试焦点可以将清晰的远景通过直播分享，带来不一样的视觉体验。在直播销售中可以使用eyemore对物品进行细节特写，拍摄的画面清晰度更高，为顾客带来更好直观感受。所以在直播中使用eyemore优势明显。

## 2、使用eyemore工作流程
* eyemore摄像头开启，将手机和usb数据线连接，运行demo，获取usb权限，接收usb传输过来的数据，将数据送到sdk中进行预览处理，当点击开始推流时候，sdk进行编码、推流。

## 3、功能特点
基于KSYStreamer4.x，详细参考：[KSYStramer说明](https://github.com/ksvc/KSYStreamer_Android/wiki)
此外，存在以下不同点
* kit类中未集成[KSYStreamer](https://github.com/ksvc/KSYStreamer_Android)的软编兼容模式
* ×kit类中未集成触摸对焦、变焦、闪光灯、测光等功能

## 4. 运行环境
* 最低支持版本为Android 4.x
* 支持的cpu架构：armv7, arm64, x86

## 6. 快速集成
快速集成方式基本同[KSYStreamer](https://github.com/ksvc/KSYStreamer_Android/wiki)的集成方式。
具体可以参考demo工程中README及相应文件。

## 7. 效果展示
* 推流（eyemore作为视频数据输入源）：
* 播放：
