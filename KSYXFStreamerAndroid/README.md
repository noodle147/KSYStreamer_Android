# 金山云 - 讯飞语言听写示例



 该工程基于[金山云推流SDK](https://github.com/ksvc/KSYLive_Android) 和 [讯飞云SDK](http://www.xfyun.cn/)。

其中，讯飞云SDK实现了将语言听写功能，将语言转化为文字。

该示例将讯飞云SDK和金山云融合SDK进行集成，可在推流的同时，使用语言听写功能，并将听写内容作为字幕叠加到推流画面上。    
![demo视频](https://raw.githubusercontent.com/wiki/ksvc/KSYDiversityLive_Android/images/ksy_xfyun_demo.gif)


## 1. 运行环境

支持的cpu架构：armeabi

## 2. SDK更新

 您可以关注金山云和讯飞云SDK的发版信息，自行更新依赖的SDK，:

- 金山云融合版新版本获取[地址](https://github.com/ksvc/KSYLive_Android/releases)， 讯飞云SDK下载[地址](http://www.xfyun.cn/sdk/dispatcher)


- 替换**app/libs** 目录下对应的so和jar文件

 如果更新出现任何问题，请联系我们

## 3. 代码说明

**app/libs** 工程依赖的libs文件，其中Msc.jar和libmsc.so为讯飞云SDK

[KSYSpeechRecognizer](app/src/main/java/com/ksyun/media/xfyun/demo/kit/KSYSpeechRecognizer.java): 用金山推流SDK的接口，将讯飞云听写功能封装为KSYSpeechRecognizer模块。

[KSYSpeechListener](app/src/main/java/com/ksyun/media/xfyun/demo/kit/KSYSpeechListener.java): 听写模块的回调

[KSYXFStreamer](app/src/main/java/com/ksyun/media/xfyun/demo/kit/KSYXFStreamer.java) : kit类，在推流的基础上，集成了KSYSpeechRecognizer模块，增加听写功能

## 4. 反馈于建议

### 4.1 反馈模板

|  类型   |                 描述                  |
| :---: | :---------------------------------: |
| SDK名称 |         KSYXYStreamAndroid          |
| SDK版本 |               v1.0.0                |
| 设备型号  |              oppo r9s               |
| OS版本  |            Android 6.0.1            |
| 问题描述  |              描述问题出现的现象              |
| 操作描述  |           描述经过如何操作出现上述问题            |
| 额外附件  | 文本形式控制台log、crash报告、其他辅助信息（界面截屏或录像等） |

## 5. 联系方式

- 主页：[金山云](http://v.ksyun.com)
- 邮箱：<zengfanping@kingsoft.com>
- QQ讨论群：574179720
- Issues: <https://github.com/ksvc/KSYDiversityLive_Android/issues>

[KSYLive_Android]:https://github.com/ksvc/KSYLive_Android
