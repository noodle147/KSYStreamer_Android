# 金山云 - Agora声网连麦

金山云 - Agora声网连麦基于[金山云推流SDK](https://github.com/ksvc/KSYStreamer_Android)，集成了Agora声网连麦相关的功能。

## 一. 功能特点

### 连麦功能
* 在 [金山云推流SDK](https://github.com/ksvc/KSYStreamer_Android)增加了连麦功能。

## 二. 运行环境

* 最低支持版本为Android 4.4

## 三. 开发指南

* 运行demo前请到[声网](https://dashboard.agora.io) 申请自己的app id,
  替换src/main/res/values/strings.xml中app_id的值

* 基于金山云推流SDK，demo定义了自己的kit类[KSYAgoraStreamer](https://github.com/ksvc/KSYDiversityLive_Android/blob/master/Agora/demo/src/main/java/com/ksyun/media/agora/kit/KSYAgoraStreamer.java)
实现直播连麦功能。

  - KSYAgoraStreamer直播推流、美颜、录制等功能同金山云推流SDK，详细使用指南请参考[KSYStramer说明](https://github.com/ksvc/KSYStreamer_Android/wiki)
  - KSYAgoraStreamer连麦功能接口

      开始连麦
      ```java
      void startRTC()
      ```

      结束连麦
      ```java
      void stopRTC()
      ```

      连麦小窗口位置和大小设置
      ```java
      void setRTCSubScreenRect(float left, float top, float width, float height, int mode)
      ```

      设置连麦时主屏幕类型
      ```java
      void setRTCMainScreen(int mainScreenType)
      ```

## 四. 反馈与建议
- 主页：[金山云](http://www.ksyun.com/)
- 邮箱：<zengfanping@kingsoft.com>
- QQ讨论群：574179720
- Issues: <https://github.com/ksvc/KSYDiversityLive_Android/issues>