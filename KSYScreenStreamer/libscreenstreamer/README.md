# libscreenstreamer工程介绍
* libs: 集成KSYStreamer的SDK需要的所有库文件
    * libs/[armeabi-v7a|arm64-v8a|x86]: 各平台的so库
    * libs/ksylive4.0.jar: 推流SDK jar包
    * libs/libksystat.jar: 金山云统计模块

* capture:提供录屏视频数据采集相关类
* kit:录屏推流相关接口类
* proguard-project.txt:混淆文件