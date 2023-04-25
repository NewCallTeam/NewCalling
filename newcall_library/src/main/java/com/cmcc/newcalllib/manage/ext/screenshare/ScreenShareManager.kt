/*
 * Copyright (c) 2022 China Mobile Communications Group Co.,Ltd. All rights reserved.
 *
 * Licensed under the XXXX License, Version X.X (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://xxxxxxx/licenses/LICENSE-X.X
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cmcc.newcalllib.manage.ext.screenshare

import android.app.Activity
import android.content.Intent
import android.telecom.Call
import android.telecom.VideoProfile
import android.text.TextUtils
import android.widget.Toast
import bolts.Task
import com.cmcc.newcalllib.adapter.network.NetworkAdapter
import com.cmcc.newcalllib.adapter.screenshare.SketchWindowHolder
import com.cmcc.newcalllib.adapter.screenshare.SketchWindowHolder.CallType
import com.cmcc.newcalllib.adapter.screenshare.bean.SketchCmdBean
import com.cmcc.newcalllib.expose.LifeCycleState
import com.cmcc.newcalllib.expose.ScreenShareHandler
import com.cmcc.newcalllib.expose.ScreenShareStatus
import com.cmcc.newcalllib.expose.ScreenShareStatusListener
import com.cmcc.newcalllib.manage.entity.NewCallException
import com.cmcc.newcalllib.manage.entity.Results
import com.cmcc.newcalllib.manage.entity.handler.req.ReqControlScreenShare
import com.cmcc.newcalllib.manage.ext.ExtensionManager
import com.cmcc.newcalllib.manage.support.Callback
import com.cmcc.newcalllib.tool.*
import com.cmcc.widget.SketchView
import com.cmcc.widget.bean.SketchInfoBean
import com.google.gson.Gson
import org.json.JSONException
import org.json.JSONObject
import java.nio.ByteBuffer
import java.util.*

/**
 * ScreenShare manager class
 * @author jihongfei
 * @createTime 2022/9/27 11:14
 */
class ScreenShareManager(private val extensionManager: ExtensionManager) :
    ScreenShareController {
    companion object {
    }

    // DC label for SDK screenShare use
    var mScreenShareDcLabel: String? = null

    // 主叫/被叫
    @CallType
    private var mCallType = CallType.MO

    /**
     *
     */
    var mActivity: Activity? = null

    // implement by dialer
    var mScreenShareHandler: ScreenShareHandler? = null

    // 涂鸦View
    private var mSketchWindowHolder: SketchWindowHolder? = null


    private fun getActivityOrThrow(): Activity {
        if (mActivity != null) {
            return mActivity!!
        }
        if (extensionManager.cxt is Activity) {
            return extensionManager.cxt
        }
        throw NewCallException("ScreenShare no activity")
    }

    /**
     * 注册label数据的拦截器
     */
    private fun registerDataInterceptor(dcLabel: String) {
        LogUtil.d("ScreenShareManager: registerDataInterceptor. dcLabel=$dcLabel")
        extensionManager.networkAdapter.registerDataInterceptor(object :
            NetworkAdapter.DataInterceptor {
            override fun provideDataChannelLabel(): String {
                return dcLabel
            }

            override fun onDataArrive(data: ByteBuffer): Boolean {
                // 收到涂鸦数据
                onSketchDataReceived(data.toStr())
                return true
            }
        })
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * Dialer API: 启动屏幕共享
     * @param role 当前终端处于屏幕共享业务哪个角色 0 为发起方，1 为接收方
     * @param dcLabel 屏幕共享即将占用的 dclable 名称
     * @param callback
     */
    override fun enableScreenShare(
        role: Int,
        dcLabel: String,
        callback: Callback<Results<Boolean>>
    ) {
        LogUtil.d("ScreenShareManager: enableScreenShare. role=$role dcLabel=$dcLabel handler=$mScreenShareHandler")

        // 主被叫数据赋值
        if (role == ReqControlScreenShare.ROLE_POSITIVE) {
            mCallType = CallType.MO
        } else {
            mCallType = CallType.MT
        }
        // 注册数据拦截器
        if (mScreenShareDcLabel == null) {
            mScreenShareDcLabel = dcLabel
            registerDataInterceptor(dcLabel)
            LogUtil.d("ScreenShareManager: enableScreen.set dcLabel first: $dcLabel")
        } else if (mScreenShareDcLabel != dcLabel) {
            throw NewCallException("dcLabel different with previous in screenShare")
        }
        // 主叫: 
        if (mCallType == CallType.MO) {
            // Dialer API: 启动dialer的屏幕录制功能
            mScreenShareHandler?.startNativeScreenShare(object : ScreenShareStatusListener {
                override fun onScreenShareStatus(status: ScreenShareStatus) {
                    // 回调: 屏幕录制启动成功
                    callback.onResult(Results(status == ScreenShareStatus.SUCCESS))
                    // 开启涂鸦悬浮窗
                    if (status == ScreenShareStatus.SUCCESS) {
                        // 1、初始化涂鸦浮层的View
                        // 2、展示悬浮窗（并检测悬浮窗权限）
                        showSketchControlWindow()
                        // 3、向被叫发消息，启动"被叫悬浮窗"
                        sendWindowShowedFromMO()
                    }
                }
            })
        }
        // 被叫: 
        else if (mCallType == CallType.MT) {
            // 回调: 拦截数据成功
            callback.onResult(Results(true))
            // show sketch control window directly
            // 1、初始化涂鸦浮层的View
            // 2、展示悬浮窗（并检测悬浮窗权限）
            // showSketchControlWindow()
        }
    }


    /**
     * Dialer API: 关闭屏幕共享
     */
    override fun disableScreenShare() {
        LogUtil.d("ScreenShareManager: disableScreenShare. handler=$mScreenShareHandler")
        // Dialer API: 退出Dialoer录屏（调用dialer接口方法，关闭屏幕录制）
        if (mCallType == CallType.MO) {
            mScreenShareHandler?.stopNativeScreenShare()
        }
        // 退出: 悬浮窗
        exitSketchControlWindow()
        mSketchWindowHolder = null
        // 退出: 重置label
        mScreenShareDcLabel = null
    }

    /**
     * Dialer API: 查询屏幕共享功能是否可用
     */
    override fun isScreenShareAvailable(): Boolean {
        LogUtil.d("ScreenShareManager: isScreenShareAvailable. handler=$mScreenShareHandler")
        // 调用dialer接口方法，判断dialer是否具备屏幕共享功能的能力
        val result = mScreenShareHandler?.requestNativeScreenShareAbility()
        return if (result != null) {
            LogUtil.d("ScreenShareManager: isScreenShareAvailable result=$result")
            result
        } else {
            LogUtil.d("ScreenShareManager: isScreenShareAvailable result null")
            false
        }
    }

    /**
     *  SDK API: onActivityResult
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        LogUtil.d("ScreenShareManager: onActivityResult. req=$requestCode, res=$resultCode")
        // 屏幕共享如果不是开启状态，不进行回调处理
        if (mSketchWindowHolder == null) {
            return;
        }
        onSketchWindowResult(requestCode, resultCode, data)
    }

    /**
     * SDK API: 电话状态变化
     */
    override fun onCallStateChanged(state: Int) {
        LogUtil.d("ScreenShareManager: onCallStateChanged. state=$state")
        // 屏幕共享如果不是开启状态，不进行回调处理
        if (mSketchWindowHolder == null) {
            return;
        }
        if (state == Call.STATE_DISCONNECTED) {
            disableScreenShare()
        }
    }

    override fun onCallTypeChanged(state: Int) {
        LogUtil.d("ScreenShareManager: onCallTypeChanged. state: $state")
        // 屏幕共享如果不是开启状态，不进行回调处理
        if (mSketchWindowHolder == null) {
            return;
        }
        // 转为音频通话（退出了屏幕共享）
        if (state == 1) {
            disableScreenShare();
        }
    }

    /**
     * SDK API: Activity 生命周期变化
     */
    override fun onActivityLifeCycleChanged(state: Int) {
        LogUtil.d("ScreenShareManager: onActivityLifeCycleChanged. state: $state")
        // 屏幕共享如果不是开启状态，不进行回调处理
        if (mSketchWindowHolder == null) {
            return;
        }
        when (state) {
            LifeCycleState.ON_START.value -> {
                // 被叫端: 回到前台
                if (mCallType == CallType.MT) {
                    showSketchControlWindow();
                }
            }
            LifeCycleState.ON_STOP.value -> {
                // 被叫端: 退到后台
                if (mCallType == CallType.MT) {
                    exitSketchControlWindow()
                }
            }
            LifeCycleState.ON_DESTROY.value -> {
                disableScreenShare()
            }
        }
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~屏幕涂鸦Begin~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /**
     * 展示屏幕涂鸦悬浮窗
     */
    private fun showSketchControlWindow() {
        LogUtil.d("ScreenShareManager: showSketchControlWindow")
        // 创建涂鸦Window
        if (mSketchWindowHolder == null) {
            LogUtil.d("ScreenShareManager: initSketchControlWindow: new mSketchWindowHolder")
            mSketchWindowHolder = SketchWindowHolder(getActivityOrThrow())
            mSketchWindowHolder?.setSketchWindowListener(object :
                SketchWindowHolder.SketchWindowListener() {
                override fun onExitScreenShareBtnClick(callType: Int) {
                    LogUtil.d("ScreenShareManager: onExitScreenShareBtnClick")
                    if (mActivity == null) {
                        return
                    }
                    // 发送退出消息
                    sendWindowExitData();
                    // Dialog 按钮确认: 退出屏幕共享
                    disableScreenShare()
                }

                override fun onSketchEvent(sketchView: SketchView, event: Int, callType: Int) {
                    if (sketchView == null) {
                        return
                    }
                    // 被叫: 涂鸦完成，回调涂鸦数据
                    if (event == SketchView.Event.SKETCH_UP) {
                        sendDrawingDataFromMT(sketchView)
                    }
                }
            })
        }
        // 1、检测悬浮窗权限 & 2、展示悬浮窗；
        mSketchWindowHolder?.showSketchControlWindow(mCallType)
    }

    /**
     * 退出屏幕涂鸦悬浮窗
     */
    private fun exitSketchControlWindow() {
        LogUtil.d("ScreenShareManager: exitSketchControlWindow.")
        // 退出: 退出屏幕悬浮
        mSketchWindowHolder?.exitSketchControlWindow()
    }

    /**
     * 涂鸦Window弹窗权限申请
     */
    private fun onSketchWindowResult(requestCode: Int, resultCode: Int, data: Intent?) {
        LogUtil.d("ScreenShareManager: onSketchWindowResult.")
        // 涂鸦中有权限申请弹窗
        mSketchWindowHolder?.onActivityResult(requestCode, resultCode, data)
    }

    /**
     * 添加外部涂鸦数据
     *
     * @param sketchInfo
     */
    private fun addSketchInfo(sketchInfo: SketchInfoBean?) {
        LogUtil.d("ScreenShareManager: addSketchInfo")
        mSketchWindowHolder?.addSketchInfo(sketchInfo)
    }

//    /**
//     * 移除列表中的sketch
//     *
//     * @param sketchIds
//     */
//    private fun rollBackSketchs(sketchIds: List<String?>?) {
//        LogUtil.d("ScreenShareManager: rollBackSketchs")
//        mSketchWindowHolder?.rollBackSketchs(sketchIds)
//    }

    // ~~~~~~~~~~~~~~~~~~~~~~~涂鸦消息接收Begin~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * 接收到涂鸦数据
     */
    private fun onSketchDataReceived(data: String) {
        LogUtil.d("ScreenShareManager: onSketchDataReceived. data: $data")
        // 收到屏幕共享DC消息
        Task.call(
            {
                // 收到屏幕共享DC消息
                val base64Data = JSONObject(data).getString("data")
                val sketchCmdJsonData = Base64Util.base64ToStr(base64Data)
                LogUtil.d("ScreenShareManager: base64Data: $base64Data")
                LogUtil.d("ScreenShareManager: sketchCmdJsonData: $sketchCmdJsonData")
                //
                Gson().fromJson(sketchCmdJsonData, SketchCmdBean::class.java)
            }, Task.BACKGROUND_EXECUTOR
        ).continueWith<Boolean>(
            { task ->
                // 2、主线程更新UI
                val error = task.error
                val sketchCmdBean = task.result
                LogUtil.d("ScreenShareManager: error=$error")
                LogUtil.d("ScreenShareManager: sketchCmdBean=$sketchCmdBean")
                when (sketchCmdBean!!.cmdType) {
                    // 控制命令：展示悬浮窗
                    SketchCmdBean.CmdType.WINDOW_SHOW -> {
                        if (mCallType == CallType.MT) {
                            // 1、初始化涂鸦浮层的View
                            // 2、展示悬浮窗（并检测悬浮窗权限）
                            showSketchControlWindow()
                        }
                    }
                    // 控制命令：退出
                    SketchCmdBean.CmdType.WINDOW_EXIT -> {
                        // 退出
                        disableScreenShare()
                    }
                    // 绘制命令：涂鸦数据
                    SketchCmdBean.CmdType.SKETCH_DATA -> {
                        if (sketchCmdBean?.sketchData == null) {
                            true
                        }
                        if (mCallType == CallType.MO) {
                            // 如果涂鸦蒙版不存在，则展示涂鸦蒙版
                            mSketchWindowHolder?.showSketchView()
                            // 解析与绘制涂鸦数据
                            val sketchData = sketchCmdBean.sketchData
                            val sketchInfoBean = SketchInfoBean(sketchData.sketchId)
                            sketchInfoBean.sketchColor = sketchData.sketchColor
                            sketchInfoBean.sketchDipWidth = sketchData.sketchDipWidth
                            sketchInfoBean.quadMoveTo = sketchData.quadMoveTo
                            sketchInfoBean.quadControlPoints = sketchData.quadControlPoints
                            sketchInfoBean.quadEndPoints = sketchData.quadEndPoints
                            // "主叫"收到涂鸦动作:添加一笔涂鸦数据
                            addSketchInfo(sketchInfoBean);
                        }
                    }
                }
                true
            }, Task.UI_THREAD_EXECUTOR
        )
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~涂鸦消息发送Begin~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * 主叫: 向被叫发送消息"可以展示涂鸦悬浮窗了"
     */
    private fun sendWindowShowedFromMO() {
        if (mCallType == CallType.MO) {
            LogUtil.d("ScreenShareManager: sendWindowShowedFromMO. mCallType: $mCallType")
            val sketchCmd = SketchCmdBean.Builder()
                .setCmdType(SketchCmdBean.CmdType.WINDOW_SHOW)
                .build()
            // 构造base64数据
            val sketchCmdJsonData = Gson().toJson(sketchCmd)
            val base64Data = Base64Util.strToBase64(Gson().toJson(sketchCmd))
            LogUtil.d("ScreenShareManager: sketchCmdJsonData: $sketchCmdJsonData")
            LogUtil.d("ScreenShareManager: base64Data: $base64Data")
            // 发送数据
            sendSketchDataByDc(mScreenShareDcLabel!!, base64Data,
                object : NetworkAdapter.RequestCallback {
                    override fun onSendDataCallback(statusCode: Int, errorCode: Int) {
                        // UI_Thread
                        Task.call({
                            if (statusCode < 1) {
                                Toast.makeText(
                                    mActivity,
                                    "DC唤起消息发送失败: $errorCode",
                                    Toast.LENGTH_SHORT
                                ).show()
                                true
                            }
                            // 被叫: 清空这一笔涂鸦数据
                            mSketchWindowHolder?.clearSketch()
                            true
                        }, Task.UI_THREAD_EXECUTOR)
                    }
                })
        }
    }

    private fun sendWindowExitData() {
        LogUtil.d("ScreenShareManager: sendWindowExit.")
        val sketchCmd = SketchCmdBean.Builder()
            .setCmdType(SketchCmdBean.CmdType.WINDOW_EXIT)
            .build()
        // 构造base64数据
        val sketchCmdJsonData = Gson().toJson(sketchCmd)
        val base64Data = Base64Util.strToBase64(Gson().toJson(sketchCmd))
        LogUtil.d("ScreenShareManager: sketchCmdJsonData: $sketchCmdJsonData")
        LogUtil.d("ScreenShareManager: base64Data: $base64Data")
        // 发送数据
        sendSketchDataByDc(mScreenShareDcLabel!!, base64Data, null)
    }

    /**
     * 被叫: 发送一笔涂鸦数据
     */
    private fun sendDrawingDataFromMT(sketchView: SketchView) {
        if (mCallType == CallType.MT) {
            LogUtil.d("ScreenShareManager: sendDrawingDataFromMT.")
            //  涂鸦完成时，数据回调
            val sketchInfoBean = sketchView.currSketchInfo
            // 手指抬起
            // 生成一个drawding的数据
            val sketchAction = SketchCmdBean.Builder()
                .setCmdType(SketchCmdBean.CmdType.SKETCH_DATA)
                .setSketchInfo(sketchInfoBean)
                .build()
            // 构造base64数据
            val sketchCmdJsonData = Gson().toJson(sketchAction)
            val base64Data = Base64Util.strToBase64(Gson().toJson(sketchAction))
            LogUtil.d("ScreenShareManager: sketchCmdJsonData: $sketchCmdJsonData")
            LogUtil.d("ScreenShareManager: base64Data: $base64Data")
            // 被叫: 发送drawing数据
            sendSketchDataByDc(mScreenShareDcLabel!!, base64Data,
                object : NetworkAdapter.RequestCallback {
                    override fun onSendDataCallback(statusCode: Int, errorCode: Int) {
                        // UI_Thread
                        Task.call({
                            if (statusCode < 1) {
                                Toast.makeText(
                                    mActivity,
                                    "DC涂鸦数据发送失败: $errorCode",
                                    Toast.LENGTH_SHORT
                                ).show()
                                true
                            }
                            // 被叫: 清空这一笔涂鸦数据
                            mSketchWindowHolder?.clearSketch()
                            true
                        }, Task.UI_THREAD_EXECUTOR)
                    }
                })

        }
    }

    /**
     * 发送涂鸦数据
     */
    private fun sendSketchDataByDc(
        label: String,
        data: String,
        callback: NetworkAdapter.RequestCallback?
    ) {
        LogUtil.d("ScreenShareManager: sendSketchDataByDc. label: $label data: $data")
        if (TextUtils.isEmpty(data) || TextUtils.isEmpty(label)) {
            return;
        }
        try {
            val jsonObj = JSONObject()
            jsonObj.putOpt("msgid", UUID.randomUUID().toString())
            jsonObj.putOpt("msgtype", "request")
            jsonObj.putOpt("busstype", "screenshare")
            jsonObj.putOpt("data", data)
            jsonObj.putOpt("datalength", data?.toByteArray()?.size)
            val jsonStr = jsonObj.toString()
            // 发送消息数据
            LogUtil.d("ScreenShareManager: sendDataOverAppDc. label: $label SketchJsonData: $jsonStr")
            extensionManager.networkAdapter.sendDataOnAppDC(label, jsonStr,
                object : NetworkAdapter.RequestCallback {
                    override fun onSendDataCallback(statusCode: Int, errorCode: Int) {
                        LogUtil.d("ScreenShareManager: sendDataOverAppDc statusCode=$statusCode, errCode=$errorCode")
                        callback?.onSendDataCallback(statusCode, errorCode)
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}