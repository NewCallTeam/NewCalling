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
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.telecom.Call
import android.text.TextUtils
import com.cmcc.newcalllib.adapter.network.ImsDCNetworkAdapter
import com.cmcc.newcalllib.adapter.network.NetworkAdapter
import com.cmcc.newcalllib.adapter.screenshare.SketchWindowHolder
import com.cmcc.newcalllib.adapter.screenshare.SketchWindowHolder.CallType
import com.cmcc.newcalllib.adapter.screenshare.transferbean.SketchAction
import com.cmcc.newcalllib.bridge.CallBackFunction
import com.cmcc.newcalllib.expose.LifeCycleState
import com.cmcc.newcalllib.expose.ScreenShareHandler
import com.cmcc.newcalllib.expose.ScreenShareStatus
import com.cmcc.newcalllib.expose.ScreenShareStatusListener
import com.cmcc.newcalllib.manage.bussiness.interact.JsCommunicator
import com.cmcc.newcalllib.manage.entity.CallInfo
import com.cmcc.newcalllib.manage.entity.NewCallException
import com.cmcc.newcalllib.manage.entity.Results
import com.cmcc.newcalllib.manage.entity.caller.req.VisibilityNotify
import com.cmcc.newcalllib.manage.entity.handler.req.ReqControlScreenShare
import com.cmcc.newcalllib.manage.ext.ExtensionManager
import com.cmcc.newcalllib.manage.support.Callback
import com.cmcc.newcalllib.tool.*
import com.cmcc.newcalllib.tool.DialogTools.DialogBtnClickCallBack
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

    var activity: Activity? = null

    // implement by dialer
    var screenShareHandler: ScreenShareHandler? = null

    // DC label for SDK screenShare use
    var screenShareDataChannelLabel: String? = null

    // 主叫/被叫
    @CallType
    private var mCallType = CallType.MO

    // 屏幕共享功能是否启动
    private var mScreenShareOn = false

    // 涂鸦View
    private var mSketchWindowHolder: SketchWindowHolder? = null


    private fun getActivityOrThrow(): Activity {
        if (activity != null) {
            return activity!!
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
        extensionManager.networkAdapter.registerDataInterceptor(object :
            NetworkAdapter.DataInterceptor {
            override fun provideDataChannelLabel(): String {
                return dcLabel
            }

            override fun onDataArrive(data: ByteBuffer): Boolean {
                // 收到涂鸦数据
                val dataStr = data.toStr()
                LogUtil.d("screen share onDataArrive:$dataStr")
                // 主叫：收到涂鸦数据
                if (mCallType == SketchWindowHolder.CallType.MO) {
                    onMOSketchDataReceived(dataStr)
                }
                return true
            }
        })
    }

    /**
     * 启动屏幕共享
     * @param role 当前终端处于屏幕共享业务哪个角色 0 为发起方，1 为接收方
     * @param dcLabel 屏幕共享即将占用的 dclable 名称
     * @param callback
     */
    override fun enableScreenShare(
        role: Int,
        dcLabel: String,
        callback: Callback<Results<Boolean>>
    ) {
        LogUtil.d("enableScreenShare, handler=$screenShareHandler")

        // 屏幕共享启动
        mScreenShareOn = true

        // 注册数据拦截器
        if (screenShareDataChannelLabel == null) {
            screenShareDataChannelLabel = dcLabel
            registerDataInterceptor(dcLabel)
            LogUtil.i("enableScreen, set dcLabel first: $dcLabel")
        } else if (screenShareDataChannelLabel != dcLabel) {
            throw NewCallException("dcLabel different with previous in screenShare")
        }
        // 主叫：
        if (role == ReqControlScreenShare.ROLE_POSITIVE) {
            // 启动dialer的屏幕录制功能
            screenShareHandler?.startNativeScreenShare(object : ScreenShareStatusListener {
                override fun onScreenShareStatus(status: ScreenShareStatus) {
                    // 回调：屏幕录制启动成功
                    callback.onResult(Results(status == ScreenShareStatus.SUCCESS))
                    // 开启涂鸦悬浮窗
                    if (status == ScreenShareStatus.SUCCESS) {
                        // 1、初始化涂鸦浮层的View
                        initSketchControlWindow()
                        // 2、展示悬浮窗（并检测悬浮窗权限）
                        showSketchControlWindow(role)
                    }
                }
            })
        }
        // 被叫：
        else if (role == ReqControlScreenShare.ROLE_NEGATIVE) {
            // show sketch control window directly
            // 1、初始化涂鸦浮层的View
            initSketchControlWindow()
            // 2、展示悬浮窗（并检测悬浮窗权限）
            showSketchControlWindow(role)
        }
    }

    /**
     * 关闭屏幕共享
     */
    override fun disableScreenShare() {
        LogUtil.d("disableScreenShare, handler=$screenShareHandler")

        // 屏幕共享退出
        mScreenShareOn = false

        // 调用dialer接口方法，关闭屏幕录制
        if (mCallType == SketchWindowHolder.CallType.MO) {
            screenShareHandler?.stopNativeScreenShare()
        }
        // 退出屏幕共享控制按钮
        exitSketchControlWindow()
    }

    /**
     * 查询屏幕共享功能是否可用
     */
    override fun isScreenShareAvailable(): Boolean {
        LogUtil.d("isScreenShareAvailable, handler=$screenShareHandler")
        // 调用dialer接口方法，判断dialer是否具备屏幕共享功能的能力
        val result = screenShareHandler?.requestNativeScreenShareAbility()
        return if (result != null) {
            LogUtil.i("isScreenShareAvailable result=$result")
            result
        } else {
            LogUtil.w("isScreenShareAvailable result null")
            false
        }
    }

    /**
     *  onActivityResult
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        LogUtil.d("ScreenShareManager, onActivityResult, req=$requestCode, res=$resultCode")
        if (!mScreenShareOn) {
            return;
        }
        onSketchWindowResult(requestCode, resultCode, data)
    }

    /**
     * 电话状态变化
     */
    override fun onCallStateChanged(state: Int) {
        LogUtil.d("ScreenShareManager, onCallStateChanged state=$state")
        if (!mScreenShareOn) {
            return;
        }
        if (state == Call.STATE_DISCONNECTED) {
            exitSketchControlWindow()
        }
    }

    /**
     * Activity 生命周期变化
     */
    override fun onActivityVisibilityNotify(state: Int) {
        LogUtil.d("visibilityNotify: $state")
        // 屏幕共享如果不是开启状态，不进行回调处理
        LogUtil.d("mScreenShareOn: $mScreenShareOn")
        if (!mScreenShareOn) {
            return;
        }
        when (state) {
            LifeCycleState.ON_FOREGROUND_IN_VOICE_CALL.value -> {
                // 转为音频通话（退出了屏幕共享）
                LogUtil.d("ON_FOREGROUND_IN_VOICE_CALL")
                disableScreenShare();
            }
            LifeCycleState.ON_FOREGROUND_IN_VIDEO_CALL.value -> {
                LogUtil.d("ON_FOREGROUND_IN_VIDEO_CALL")
                // 被叫端: 回到前台
                if (mCallType == CallType.MT) {
                    showSketchControlWindow(mCallType);
                }
            }
            LifeCycleState.ON_BACKGROUND.value -> {
                LogUtil.d("ON_BACKGROUND")
                // 被叫端: 退到后台
                if (mCallType == CallType.MT) {
                    exitSketchControlWindow()
                }
            }
        }
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~屏幕涂鸦Begin~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /**
     * 初始化屏幕涂鸦弹窗
     */
    private fun initSketchControlWindow() {
        // 创建涂鸦Window
        if (mSketchWindowHolder == null) {
            LogUtil.d("ScreenShareManager: ", "initSketchControlWindow：new mSketchWindowHolder")
            mSketchWindowHolder = SketchWindowHolder(getActivityOrThrow())
            mSketchWindowHolder?.setSketchWindowListener(object :
                SketchWindowHolder.SketchWindowListener() {
                override fun onExitScreenShareBtnClick(callType: Int) {
                    LogUtil.d("ScreenShareManager: ", "showExitTipDialog")
                    if (activity == null) {
                        return
                    }
                    // 按钮点击：退出屏幕共享
                    disableScreenShare()
                }

                override fun onSketchEvent(sketchView: SketchView, event: Int, callType: Int) {
                    if (sketchView == null) {
                        return
                    }
                    // 被叫：涂鸦完成，回调涂鸦数据
                    if (callType == SketchWindowHolder.CallType.MT) {
                        if (event == SketchView.Event.SKETCH_UP) {
                            onMTSketchDrawingDone(sketchView)
                        }
                    }
                }
            })
        }
    }

    /**
     * 展示屏幕涂鸦悬浮窗
     */
    private fun showSketchControlWindow(role: Int) {
        LogUtil.d("ScreenShareManager: ", "showSketchControlWindow")
        // 主被叫判断
        if (role == ReqControlScreenShare.ROLE_POSITIVE) {
            mCallType = SketchWindowHolder.CallType.MO
        } else {
            mCallType = SketchWindowHolder.CallType.MT
        }
        // 1、检测悬浮窗权限；
        // 2、展示悬浮窗；
        mSketchWindowHolder?.showSketchControlWindow(mCallType)
    }

    /**
     * 退出屏幕涂鸦悬浮窗
     */
    fun exitSketchControlWindow() {
        LogUtil.d("ScreenShareManager: ", "exitScreenShare")
        // 退出：退出屏幕悬浮
        mSketchWindowHolder?.exitSketchControlWindow()
    }

    /**
     * 涂鸦Window弹窗权限申请
     */
    private fun onSketchWindowResult(requestCode: Int, resultCode: Int, data: Intent?) {
        LogUtil.d("ScreenShareManager: ", "onSketchWindowResult")
        // 涂鸦中有权限申请弹窗
        mSketchWindowHolder?.onActivityResult(requestCode, resultCode, data)
    }

    /**
     * 添加外部涂鸦数据
     *
     * @param sketchInfo
     */
    private fun addSketchInfo(sketchInfo: SketchInfoBean?) {
        LogUtil.d("ScreenShareManager: ", "addSketch")
        mSketchWindowHolder?.addSketchInfo(sketchInfo)
    }

    /**
     * 移除列表中的sketch
     *
     * @param sketchIds
     */
    private fun rollBackSketchs(sketchIds: List<String?>?) {
        LogUtil.d("ScreenShareManager: ", "rollBackSketchs")
        mSketchWindowHolder?.rollBackSketchs(sketchIds)
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~涂鸦事件处理Begin~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * 被叫：发送一笔涂鸦数据
     */
    private fun onMTSketchDrawingDone(sketchView: SketchView) {
        LogUtil.d("ScreenShareManager: ", "onMTSketchDrawingUp")
        //  涂鸦完成时，数据回调
        val sketchInfoBean = sketchView.currSketchInfo
        // 手指抬起
        // 生成一个drawding的数据
        val sketchAction = SketchAction.Builder()
            .setActionType(SketchAction.Type.DRAWING)
            .setSketchInfo(sketchInfoBean)
            .build()
        // 构造base64数据
        val jsonData = Gson().toJson(sketchAction)
        val base64Data = Base64Util.strToBase64(jsonData)
        LogUtil.d("ScreenShareManager: ", "sketchJson: $jsonData")
        LogUtil.d("ScreenShareManager: ", "base64Data: $base64Data")

        // 被叫：发送drawing数据
        sendSketchData(base64Data)
        // 被叫：清空这一笔涂鸦数据
        mSketchWindowHolder?.clearSketch()
    }

    /**
     * 主叫：接收到"被叫发送的"涂鸦数据
     */
    private fun onMOSketchDataReceived(data: String) {
        LogUtil.d("ScreenShareManager: ", "onMOSketchDataReceived: $data")
        // 收到的是一个 涂鸦 或 撤销
        var sketchAction: SketchAction? = null
        try {
            val base64Data = JSONObject(data).getString("data")
            val sketchJsonData = Base64Util.base64ToStr(base64Data)
            sketchAction = Gson().fromJson(sketchJsonData, SketchAction::class.java)
            LogUtil.d("ScreenShareManager: ", "base64Data: $base64Data")
            LogUtil.d("ScreenShareManager: ", "sketchJsonData: $sketchJsonData")
            LogUtil.d("ScreenShareManager: ", "sketchAction: $sketchAction")
        } catch (e: JSONException) {
            e.printStackTrace()
            return
        }
        when (sketchAction!!.actionType) {
            // 涂鸦数据
            SketchAction.Type.DRAWING -> {
                if (sketchAction?.drawing != null) {
                    /**
                     * 如果涂鸦蒙版不存在，则展示涂鸦蒙版
                     */
                    mSketchWindowHolder?.showSketchView()

                    /**
                     * 解析与绘制涂鸦数据
                     */
                    val sketchMsgDrawing = sketchAction.drawing
                    val sketchInfoBean = SketchInfoBean(sketchMsgDrawing.sketchId)
                    sketchInfoBean.sketchColor = sketchMsgDrawing.sketchColor
                    sketchInfoBean.sketchDipWidth = sketchMsgDrawing.sketchDipWidth
                    sketchInfoBean.quadMoveTo = sketchMsgDrawing.quadMoveTo
                    sketchInfoBean.quadControlPoints = sketchMsgDrawing.quadControlPoints
                    sketchInfoBean.quadEndPoints = sketchMsgDrawing.quadEndPoints
                    // "主叫"收到涂鸦动作:添加一笔涂鸦数据
                    addSketchInfo(sketchInfoBean);

                }
            }
            SketchAction.Type.UNDO -> {
                // 撤销数据
                if (sketchAction?.undo != null) {
                    val sketchMsgUndo = sketchAction.undo
                    // "主叫"收到撤销动作:添加一笔涂鸦数据
                    rollBackSketchs(sketchMsgUndo.undoSketchIds)
                }
            }
        }
    }

    /**
     * 发送涂鸦数据
     */
    private fun sendSketchData(data: String?) {
        LogUtil.d("ScreenShareManager: ", "sendDrawingRequest")
        if (TextUtils.isEmpty(data)) {
            return;
        }
//        // 构造发送方与接收方数据
//        val mCallInfo: CallInfo? = (extensionManager.networkAdapter as ImsDCNetworkAdapter).getCallInfo()
//        LogUtil.d("ScreenShareManager: ", "mCallInfo: $mCallInfo")
        //
//        try {
//            val jsonObject = JSONObject()
//            jsonObject.putOpt("msgid", UUID.randomUUID().toString())
//            jsonObject.putOpt("msgtype", "request")
//            jsonObject.putOpt("busstype", "screenshare")
//            jsonObject.putOpt("requestuserid", mCallInfo?.localNumber)
//            jsonObject.putOpt("oppositeid", mCallInfo?.remoteNumber)
//            jsonObject.putOpt("data", data)
//            jsonObject.putOpt("datalength", data?.toByteArray()?.size)
//            //
//            val jsonData = jsonObject.toString()
//            LogUtil.d("ScreenShareManager: ", "sketchJson: $jsonData")
//            // 发送消息数据
//            extensionManager.networkAdapter.sendDataOverAppDc(screenShareDataChannelLabel!!, jsonData,
//                object : NetworkAdapter.RequestCallback {
//                    override fun onSendDataCallback(statusCode: Int, errorCode: Int) {
//                        LogUtil.d("SendData statusCode=$statusCode, errCode=$errorCode")
//                    }
//                })
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
    }
}