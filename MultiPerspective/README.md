# Android双机位采集使用说明
  使用以下双机位、双视角采集的方法，可根据需求进行扩展实现多机位、多视角采集。

## 一. 功能特点
   
   双机位采集分为主播和第二视角两个机位进行采集，使用[金山云推流SDK](https://github.com/ksvc/KSYStreamer_Android)
   主播和第二视角数据在主播端融合后，后可预览、推流、录制。
   
## 二. 运行环境
   
   最低支持版本为Android 4.4
   
## 三. 开发指南

   ### 3.1 实现原理
   
   双机位采集分为主播端和第二视角两个部分，主播机位和第二视角使用RTSP协议进行连接，
   如下图所以:
        ![](https://github.com/sujia/image_foder/blob/master/rtsp_server.png) 
  
   1. 第二视角
      作为第二视角的手机，运行了一个RTSP Server。运行Server后，该手机实现了类似网络摄像头的功能。  
      Server在收到主播端连接请求后，使用[金山云推流SDK](https://github.com/ksvc/KSYStreamer_Android)进行
      音视频数据采集、处理(添加音视频filter)、编码后，通过RTP协议发送给主播端。  
   
   2. 主播端  
      在连接第二视角的过程中，主播端相当于RTSP Client。
      在与第二视角建立连接后，将本机与第二视角音视频数据进行融合。
   
   ### 3.2 金山云推流SDK获取   
       请直接从github获取：https://github.com/ksvc/KSYStreamer_Android
       
   ### 3.3 使用说明
   
   SDK定义了新的kit类KSYRTSPStreamKit,管理各个模块, 完成主播和第二视角数据融合、推流、美颜、录制等功能.  
   
   demo使用前请按照以下方法修改rtsp server地址
   
   1. 启动第二视角
   
      ```java
         /* 第二视角机器调用此方法启动RTSP Server服务
      
             如启动第二视角的机器IP为 192.168.1.154，设置端口号为12345
             则第二视角的访问地址为 rtsp://192.168.1.154:12345
             主播端需访问改地址连接第二视角
          */
         boolean startRTSPServer(int port)
      ```
      
   2. 主播端连接第二视角
   
      ```java
        /*
          主播调用此接口连接第二视角
          参数url为第二视角的地址
        */
        boolean startRTSPClient(String url)
      ```
   
   3. 主播端主窗口设置
      ```java
         /* 设置为MAIN_SCREEN_CAMERA，代表主播本机为大窗口
            设置为MAIN_SCREEN_REMOTE，代表第二视角为小窗口
          */
         void setMainScreen(int mainScreenType)
      
      ```
     
   4. 小窗口大小及位置设置
      ```java
         /**
              * the sub screen position
              *
              * @param width  0~1 default value 0.35f
              * @param height 0~1 default value 0.3f
              * @param left   0~1 default value 0.65f
              * @param top    0~1 default value 0.f
              * @param mode   scaling mode
              */
         void setSubScreenRect(float left, float top, float width, float height, int mode)
      ```

   5. 大小窗口切换
       ```java
          void switchMainScreen()
       ```
   
   6. 小窗口拖动实现可参考demo工程中的CameraActivity中的实现.  
      添加音视频特效、推流、录制等实现同[金山云推流SDK](https://github.com/ksvc/KSYStreamer_Android)，
      请参考[wiki](https://github.com/ksvc/KSYStreamer_Android/wiki)
       
## 四. 反馈与建议
- 主页：[金山云](http://www.ksyun.com/)
- 邮箱：<zengfanping@kingsoft.com>
- QQ讨论群：574179720
- Issues: <https://github.com/ksvc/KSYDiversityLive_Android/issues>