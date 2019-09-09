### 直播带有YY美颜

#### 准备工作
1. 在三体云官网SDK下载页 [http://3ttech.cn/index.php?menu=53](http://3ttech.cn/index.php?menu=53) 下载对应平台的 连麦直播SDK。(Pod正式使用版是2.3.0及以上)
2. 登录三体云官网 [http://dashboard.3ttech.cn/index/login](http://dashboard.3ttech.cn/index/login) 注册体验账号，进入控制台新建自己的应用并获取APPID

### 三体SDK

1. 在**MainApplication**里填写申请到的三体AppID。
2. 要去官网上下载三体SDK，Demo默认不带。
3. 使用YY美颜，需要YY来提供视频采集，所以需要启用三体外部视频源接口功能 **setExternalVideoSource**。
4. Demo中暂只有主播角色可以使用和调整美颜，副播暂不支持美颜，视频采集还是用三体SDK的，是因为demo界面展示原因，改动工作量太大。


### 美颜SDK

1. Demo自带有YY美颜SDK **orangefilter.aar** 。
2. YY美颜SDK需要配置包名和相对应的AppID，AppID在**string.xml**里配置

### 注意事项
1.YY美颜SDK需要的最低版本是21，而三体SDK最低要求的版本是16.



