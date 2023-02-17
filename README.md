# NewCall_Library 1.0.0

新通话终端套件，为支撑新通话业务而开发，免费贡献给新通话产业，终端厂家、芯片厂家、三方开发者可以基于开源版本进行二次开发，定制添加自定义需求，充分发挥运营商网络强大能力。
Develop by NewCall Team, Contribute to NewCall Bussiness.

[Download demo app](http://xxx.com)

## 如何引用
使用Gradle构建

```c
repositories {
  google()
  mavenCentral()
}

dependencies {
  // 新通话SDK逻辑功能
  implementation ''
  
  // 内容共享图片选择器 (必须)
  implementation 'io.github.lucksiege:pictureselector:v3.10.7'

  // 内容共享图片选择器 (必须)
  implementation 'io.github.lucksiege:camerax:v3.10.7'
  
  implementation 'androidx.constraintlayout:constraintlayout:1.1.3'
  
  // gson
  implementation 'com.google.code.gson:gson:2.8.6'
  
  // bolts
  implementation 'com.parse.bolts:bolts-tasks:1.4.0'
  
}
```

## 权限

```c
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<!--录制屏幕：前台应用权限-->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<!--录制屏幕：悬浮窗权限-->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
<!-- 粗略的位置权限 -->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<!-- 精确的位置权限 -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

<!-- MTK 平台 Service权限 -->
<queries>
        <package android:name="com.cmcc.dcservice" />
</queries>
<uses-permission android:name="com.cmcc.dcservice.bind" />
```

## 调用方式
[NewCall_Library集成测试文档](http://xxx.com)

## 终端兼容性测试
目前完成小米、vivo、中兴终端集成。

## 终端性能测试
计划2023年5月完成。

## 终端稳定性测试
计划2023年6月完成。
