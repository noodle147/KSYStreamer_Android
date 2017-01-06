# 金山云-Faceunity 人脸跟踪及虚拟道具

## 1.概述

金山视频云拥有功能全面的推流和拉流解决方案，Faceunity在图像识别和图像处理有多年的技术积累。

基于金山云开发平台，可以很方便地将Faceunity人脸跟踪、虚拟道具、美颜功能植入到金山云直播SDK中。

Faceunity虚拟道具预览、直播效果视频请点击以下链接：

[![ScreenShot]()

### 1.1 Faceunity鉴权
Demo中不包含Faceunity鉴权需要的证书，需要用户联系Faceunity

Faceunity的系统通过标准TLS
证书进行鉴权。客户在使用时先从发证机构申请证书，之后将证书数据写在客户端代码中，客户端运行时发回我司服务器进行验证。在证书有效期内，可以正常使用库函数所提供的各种功能。没有证书或者证书失效等鉴权失败的情况会限制库函数的功能，在开始运行一段时间后自动终止。

证书类型分为两种，分别为发证机构证书和终端用户证书。

- 发证机构证书
适用对象：此类证书适合需批量生成终端证书的机构或公司，比如软件代理商，大客户等。

发证机构的二级CA证书必须由我司颁发，具体流程如下。

1. 机构生成私钥 机构调用以下命令在本地生成私钥 CERT_NAME.key ，其中 CERT_NAME 为机构名称。
```java
openssl ecparam -name prime256v1 -genkey -out CERT_NAME.key
```

2. 机构根据私钥生成证书签发请求 机构根据本地生成的私钥，调用以下命令生成证书签发请求 CERT_NAME.csr 。在生成证书签发请求的过程中注意在 Common Name
字段中填写机构的正式名称。
```java
openssl req -new -sha256 -key CERT_NAME.key -out CERT_NAME.csr
```

3. 将证书签发请求发回我司颁发机构证书
如果需要在终端用户证书有效期内终止证书，可以由机构自行用OpenSSL吊销，然后生成pem格式的吊销列表文件发给我们。例如如果要吊销先前误发的 "bad_client.crt"，可以如下操作：
```java
openssl ca -config ca.conf -revoke bad_client.crt -keyfile CERT_NAME.key -cert CERT_NAME.crt
openssl ca -config ca.conf -gencrl -keyfile CERT_NAME.key -cert CERT_NAME.crt -out CERT_NAME.crl.pem
```
然后将生成的 CERT_NAME.crl.pem 发回给我司。

- 终端用户证书

适用对象：直接的终端证书使用者。比如，直接客户或个人等。

终端用户由我司或者其他发证机构颁发证书，对于Android平台，出于Android平台易于逆向工程的考虑，需要通过我司的证书工具生成一个authpack.java文件交给用户。该类含有一个静态方法，返回内容是加密之后的证书数据，类型为byte数组，形式如下：
```java
public class authpack {
    ...
    public static byte[] A() {
        ...
    }
}
```

用户在库环境初始化时，需要提供该数组进行鉴权，具体参考 fuSetup 接口。没有证书、证书失效、网络连接失败等情况下，会造成鉴权失败，在控制台或者Android平台的log里面打出 "not authenticated" 信息，并在运行一段时间后停止渲染道具。

任何其他关于授权问题，请email：support@faceunity.com


## 2.反馈与建议
### 2.1 金山云
* 主页：[金山云](http://www.ksyun.com/)
* 邮箱：<zengfanping@kingsoft.com>
* QQ讨论群：574179720
* Issues:https://github.com/ksvc/KSYDiversityLive_Android/issues


### 2.3 Faceunity科技
* 主页：http://www.faceunity.com

