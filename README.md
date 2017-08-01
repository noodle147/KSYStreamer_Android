# 金山云多样化SDK接入(Android)
## 一. 目标
[KSYDiversityLive](https://github.com/ksvc/KSYDiversityLive_Android)基于组件化思路，通过和多种数据处理服务商的联动，共同构建和繁荣移动直播生态链。

金山云SDK通过共享全链路处理能力，开放数据接入处理，实现以下几个方面的多样化：  

1. 功能模式的多样化；
1. 应用场景的多样化；
1. 合作方式的多样化；

## 二. 基础
[KSYDiversityLive](https://github.com/ksvc/KSYDiversityLive_Android) 基于[KSYLive_Android][KSYLive_Android]，依托[KSYLive_Android][KSYLive_Android]的组件化功能，将金山云长期积累的音视频处理、编码和传输能力分享出来，和第三方组建结合提供更强大的功能。

其中由金山云组合的功能，完成组合工作的所有代码都采用open source形式在此开放。

同时也欢迎从事音视频处理的企业、个人共同参与，一起构建更繁荣的移动直播市场。

## 三. 场景
[KSYDiversityLive](https://github.com/ksvc/KSYDiversityLive_Android)会覆盖越来越多的场景，不仅限于以下。也欢迎大家踊跃提新的适配场景：
* 混响
* 声音增强
* 美颜处理
* 人脸识别
* 视频增强
* 实时音视频通信
* 第三方视频输入

## 四. 功能
[KSYDiversityLive](https://github.com/ksvc/KSYDiversityLive_Android)提供以下功能。同时也欢迎大家提新的功能需求。
* [游戏录屏直播](KSYScreenStreamer)
* [应用内录屏](KSYScreenStreamer)
* [第三方相机接入](https://github.com/ksvc/KSYDiversityLive_Android/tree/master/eyemore)
* [短视频录制编辑](https://github.com/ksvc/KSYMediaEditorKit_Android)
* 边录制边推流
* [商汤动态贴纸、手势识别](https://github.com/ksvc/KSYDiversityLive_Android/tree/master/KSYStickerAndroid)
* [Faceunity动态贴纸](KSYFaceunityAndroid)
* [涂图美颜](TuSDK)  
* [画中画](KSYPipStreamerAndroid)
* [声网连麦](Agora)
* 无人机直播
* [多视角采集](MultiPerspective)

## 五. 开放合作
任何企业、个人都可以参与，提供的第三方SDK付费、免费都可以。不限于以下的合作场景：

1. 企业商用SDK，金山云提供品牌露出和鉴权、付费接口露出；  
1. 个人完成的SDK；  
1. 基于[KSYLive_Android][KSYLive_Android]的任意工具、SDK、plugin；  

### 5.1 原则
第三方音视频处理SDK与[KSYLive_Android][KSYLive_Android]组合，完成数据流通路上的协作。串联第三方SDK和[KSYLive_Android][KSYLive_Android]的代码，全部**开源提供**。方便大家修改和适配其他SDK。

## 六. 大事记

1. 2016.11.04，[录屏稳定版本](KSYScreenStreamer)上线
1. 2016.11.11，[专业相机推流](eyemore)提供接入
1. 2016.11.24，[画中画](KSYPipStreamerAndroid)上线
1. 2016.12.6, [商汤动态贴纸](KSYStickerAndroid)上线  
1. 2016.12.28, [声网连麦](Agora)上线  
1. 2017.1.6, [Faceunity动态贴纸](KSYFaceunityAndroid)上线  
1. 2017.1.18, [涂图美颜](TuSDK)上线   
1. 2017.1.22, [Faceunity手势识别](KSYFaceunityAndroid)上线   
1. 2017.1.24, [商汤手势识别](KSYStickerAndroid)上线
1. 2017.4.1, [多视角采集](MultiPerspective)上线
1. 2017.4.24 [短视频编辑SDK](https://github.com/ksvc/KSYMediaEditorKit_Android)上线

## 七. 反馈与建议
### 7.1 反馈模板  

| 类型    | 描述|
| :---: | :---:| 
|SDK名称|KSYDiversityLive_android|
|SDK版本| v2.5.0|
|设备型号| iphone7  |
|OS版本| iOS 10 |
|问题描述| 描述问题出现的现象  |
|操作描述| 描述经过如何操作出现上述问题                     |
|额外附件| 文本形式控制台log、crash报告、其他辅助信息（界面截屏或录像等） |

### 7.2 联系方式
- 主页：[金山云](http://v.ksyun.com)
- 邮箱：<zengfanping@kingsoft.com>
- QQ讨论群：574179720 [视频云技术交流群] 
- Issues: <https://github.com/ksvc/KSYDiversityLive_Android/issues>

[KSYLive_Android]:https://github.com/ksvc/KSYLive_Android
