# 金山云-Faceunity 人脸跟踪及虚拟道具

## 1.概述

金山视频云拥有功能全面的推流和拉流解决方案，Faceunity在图像识别和图像处理有多年的技术积累。

基于金山云开发平台，可以很方便地将Faceunity人脸跟踪、虚拟道具、美颜功能植入到金山云直播SDK中。

* Faceunity虚拟道具预览、直播效果视频请点击以下链接：

[![ScreenShot](https://raw.githubusercontent.com/wiki/ksvc/KSYDiversityLive_Android/images/faceunity.png)](http://www.bilibili.com/video/av7879423/)

* Faceunity大眼、瘦脸、手势识别效果，请点击视频链接：
[![ScreenShot](https://raw.githubusercontent.com/wiki/ksvc/KSYDiversityLive_Android/images/fu/fudroid-bilibili.jpg)](http://www.bilibili.com/video/av8168249/)

### 1.1 Faceunity鉴权
Demo中鉴权只是临时鉴权，需要用户联系Faceunity获取正式鉴权

Faceunity的系统通过标准TLS证书进行鉴权。客户在使用时先从发证机构申请证书，之后将证书数据写在客户端代码中，客户端运行时发回Faceunity公司服务器进行验证。在证书有效期内，可以正常使用库函数所提供的各种功能。没有证书或者证书失效等鉴权失败的情况会限制库函数的功能，在开始运行一段时间后自动终止。

关于鉴权授权问题，请email：support@faceunity.com


## 2.反馈与建议
### 2.1 金山云
* 主页：[金山云](http://www.ksyun.com/)
* 邮箱：<zengfanping@kingsoft.com>
* QQ讨论群：574179720
* Issues:https://github.com/ksvc/KSYDiversityLive_Android/issues


### 2.3 Faceunity科技
* 主页：http://www.faceunity.com

