# NewCall_Library

新通话终端套件，为支撑新通话业务而开发，免费贡献给新通话产业，终端厂家、芯片厂家、三方开发者可以基于开源版本进行二次开发，定制添加自定义需求，充分发挥运营商网络强大能力。  
Develop by NewCall Team, Contribute to NewCall Bussiness.

[Download demo app](http://xxx.com)

## 如何引用
使用Gradle构建

```c
allprojects {
    repositories {
        google()
        jcenter()
        // 添加maven jitpack
        maven { url "https://jitpack.io" }
    }
    // 引用NewCalling
    dependencies {
        // Spreadtrum 展锐平台
        implementation 'com.github.NewCallTeam:NewCalling:master-SNAPSHOT:Spreadtrum'
        // Mtk 芯片平台
        //implementation 'com.github.NewCallTeam:NewCalling:master-SNAPSHOT:Mtk'
        // Qcom 高通平台
        //implementation 'com.github.NewCallTeam:NewCalling:master-SNAPSHOT:Qualcomm'
        // Qcom 高通平台 中兴终端
        //implementation 'com.github.NewCallTeam:NewCalling:master-SNAPSHOT:QualcommZTE'
        // Samsung 三星平台
        //implementation 'com.github.NewCallTeam:NewCalling:master-SNAPSHOT:Samsung'
    }
}


dependencies {
    // NewCalling 引用的三方依赖包：
    // gson
    implementation 'com.google.code.gson:gson:2.8.6'
    // bolts
    implementation 'com.parse.bolts:bolts-tasks:1.4.0'
    // okio
    implementation "com.squareup.okio:okio:2.8.0"
    // glide
    implementation "com.github.bumptech.glide:glide:4.12.0"
    // PictureSelector 
    implementation 'io.github.lucksiege:pictureselector:v3.10.7'
    implementation 'io.github.lucksiege:camerax:v3.10.7'
}
```  

## 权限

```c
<!--文件共享：读写sdcard权限-->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<!--Android 13版本适配，细化存储权限-->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<!--文件共享：拍照录制视频-->
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.CAMERA" />
<!--手机号-->
<uses-permission android:name="android.permission.READ_PHONE_NUMBERS" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<!--录制屏幕: 前台应用权限-->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<!--录制屏幕: 悬浮窗权限-->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
<!-- 粗略的位置权限 -->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```  

## 调用方式
[NewCall_Library集成测试文档](http://xxx.com)

## 终端兼容性测试
目前完成小米、vivo、中兴终端集成。

## 终端性能测试
计划2023年5月完成。

## 终端稳定性测试
计划2023年6月完成。