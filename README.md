# NewCalling_Library

新通话终端套件，为支撑新通话业务而开发，免费贡献给新通话产业，`终端厂家`、`芯片厂家`、三方开发者可以`基于开源版本进行二次开发`，定制添加自定义需求，充分发挥运营商网络强大能力。  

Develop by NewCall Team, Contribute to NewCall Bussiness.

## 一、研发背景

随着5G网络的广泛应用和通信服务的不断升级，用户对通信服务的需求不断提高。传统通话服务虽基本满足用户的需求，但在某些方面仍存在不足之处。例如，传统通话在通话前、通话中缺乏“内容”、“场景”和“交互”，传统通话服务因技术限制，难以提供高效、便捷、高质量的解决方案。

中国移动作为通信服务领域的领先企业，启动了“新通话”研发工作，引入IMS Data Channel技术，通过技术升级和业务创新来提升通话服务的质量和体验。在建立音视频流媒体传输通道的同时，再次建立一条或多条低延迟、高速率、高可靠性的数据传输通道（用于传输文本、图片、文件、位置等多媒体实时数据），并推出包括智能翻译、趣味通话、智能客服、远程协助、内容分享等特色增强通话服务，实现从传统通话到`多媒体`、`可视化`、`全交互`的通话升级。

终端研发是“新通话”业务发展的关键，中国移动一直致力于提供更好的通信体验，提出的终端解决方案，已得到`芯片厂商（高通、三星半导体、MTK、展锐）`和`终端设备厂商（中兴、小米、OPPO、vivo、展锐）`的支持。目前，正在联合相关厂商进行芯片、终端设备和终端软件方面的调整和验证，相信通过持续的终端研发，能够不断提升用户的通信体验，为用户带来更好的用户体验。

## 二、技术架构

中国移动设计并实现的“新通话应用”基于运营商网络，用户终端与IMS网络平台之间建立音视频承载的同时，建立一条或多条IMS Data Channel数据专用承载，用于传输文本、文件、位置、图片等多媒体交互数据。

![新通话 数据通道](https://raw.githubusercontent.com/NewCallTeam/NewCalling/master/img/新通话_数据通道.png)

基于以上技术，中国移动提出“新通话应用”解决方案，芯片厂商和终端设备厂商分别提供Modem、Android Framework终端侧IMS Data Channel能力的API接口。中国移动开发并提供“新通话应用小程序运行环境”，并将其内置在终端设备厂商通话拨号盘应用中。第三方开发者或小程序提供商则负责开发运行在“新通话应用”上的新通话小程序。

![新通话应用运行环境架构](https://raw.githubusercontent.com/NewCallTeam/NewCalling/master/img/新通话应用运行环境架构.png)

在架构设计方面，“新通话应用”为用户提供了一个全新的通话体验。其中，“新通话小程序运行环境”负责动态下载、加载和渲染“新通话小程序”，并通过IMS Data Channel专用承载实现“新通话小程序”与“新通话平台”之间的数据传输与交互。
当用户在通话过程中需要使用通话小程序（例如智能翻译、屏幕共享等）时，这些小程序将以“新通话小程序”的形式通过数据通道动态加载到用户终端，从而实现用户与IMS新通话平台的交互。

![新通话应用运行环境详细架构](https://raw.githubusercontent.com/NewCallTeam/NewCalling/master/img/新通话应用运行环境详细架构.png)


## 三、如何引用

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
```

## 四、权限说明

```c
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

## 五、终端兼容
目前完成小米、vivo、中兴、展锐终端集成。

## 六、终端性能测试
计划2023年12月完成。

## 七、终端稳定性测试
计划2023年12月完成。
