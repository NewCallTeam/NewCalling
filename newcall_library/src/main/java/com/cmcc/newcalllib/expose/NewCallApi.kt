package com.cmcc.newcalllib.expose

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.telecom.Call
import com.cmcc.newcalllib.BuildConfig
import com.cmcc.newcalllib.manage.bussiness.NewCallManager
import com.cmcc.newcalllib.manage.entity.CallInfo
import com.cmcc.newcalllib.manage.support.ConfigManager
import com.cmcc.newcalllib.tool.CrashHandler
import com.cmcc.newcalllib.tool.LogUtil

/**
 * @author jihongfei
 * @createTime 2022/2/11 16:53
 */
@Suppress("unused", "FoldInitializerAndIfToElvis")
object NewCallApi {
    private const val RESULT_OK = 1
    private const val RESULT_FAIL = 0
    private const val RESULT_ERR_ALREADY_INIT = -1
    private const val RESULT_ERR_PARAMS_ILLEGAL = -2
    private const val RESULT_ERR_MISMATCH_SESSION = -3

    private var mManagerMap = mutableMapOf<String, NewCallManager>()
    private var mCurrentSessionId: String? = null

    private fun findNewCallManager(sessionId: String): NewCallManager? {
        return mManagerMap[sessionId]
    }

    /**
     * Init newCall SDK.
     * @param context context from Dialer
     * @param call object of [android.telecom.Call]
     * @param info info of this call
     * @param sessionId marks one call session. eg: slotId_remoteNumber
     */
    fun initNewCall(context: Context, call: Call, info: CallInfo?, sessionId: String): Int {
        // init base tools
        CrashHandler.init(context.applicationContext)
        LogUtil.initLog(context, true, true)
        if (BuildConfig.DEBUG) {
            LogUtil.setLogLevel(LogUtil.VERBOSE)
        } else {
            LogUtil.setLogLevel(LogUtil.DEBUG)
        }
        LogUtil.i("NewCallApi initNewCall, info=$info")

        // init sdk manager
        // pre-check call validity
        if (info == null) {
            return RESULT_ERR_PARAMS_ILLEGAL
        }
        if (mManagerMap.containsKey(sessionId)) {
            return RESULT_ERR_ALREADY_INIT
        }
        val newCallManager = NewCallManager(sessionId, info)
        val initRet = newCallManager.init(context, call)
        // update session and map
        mCurrentSessionId = newCallManager.sessionId
        mManagerMap[newCallManager.sessionId] = newCallManager
        return if (initRet) RESULT_OK else RESULT_FAIL
    }

    /**
     * Release newCall SDK.
     * @param sessionId marks one call session. eg: slotId_remoteNumber
     */
    fun release(sessionId: String): Int {
        LogUtil.i("NewCallApi release")
        val manager = findNewCallManager(sessionId)
        if (manager == null) {
            return RESULT_ERR_MISMATCH_SESSION
        }
        if (mCurrentSessionId == null) {
            return RESULT_FAIL
        }
        manager.release()
        // update session and map
        mManagerMap.remove(mCurrentSessionId)
        mCurrentSessionId = null
        return RESULT_OK
    }

    /**
     * Set error listener
     * @param sessionId marks one call session. eg: slotId_remoteNumber
     * @param errListener error listener
     */
    fun setErrorListener(sessionId: String, errListener: ErrorListener?): Int {
        LogUtil.i("NewCallApi setErrorListener")
        val manager = findNewCallManager(sessionId)
        if (manager == null) {
            return RESULT_ERR_MISMATCH_SESSION
        }
        manager.setErrListeners(errListener)
        return RESULT_OK
    }

    /**
     * Set message listener
     * @param sessionId marks one call session. eg: slotId_remoteNumber
     * @param msgListener message listener
     */
    fun setMessageListener(sessionId: String, msgListener: MessageListener?): Int {
        LogUtil.i("NewCallApi setMessageListener, $msgListener")
        val manager = findNewCallManager(sessionId)
        if (manager == null) {
            return RESULT_ERR_MISMATCH_SESSION
        }
        manager.setMessageListener(msgListener)
        return RESULT_OK
    }

    /**
     * Set mini-app change listener
     * @param sessionId marks one call session. eg: slotId_remoteNumber
     * @param miniAppChangeListener msg listener
     */
    fun setMiniAppChangeListener(
        sessionId: String,
        miniAppChangeListener: MiniAppChangeListener?
    ): Int {
        LogUtil.i("NewCallApi setMiniAppChangeListener, $miniAppChangeListener")
        val manager = findNewCallManager(sessionId)
        if (manager == null) {
            return RESULT_ERR_MISMATCH_SESSION
        }
        manager.setMiniAppChangeListener(miniAppChangeListener)
        return RESULT_OK
    }


    /**
     * Update webView in Dialer
     * @param sessionId marks one call session. eg: slotId_remoteNumber
     * @param webView CmccBridgeWebView obj, null if webView no more use
     */
    fun setWebView(sessionId: String, webView: CmccBridgeWebView?): Int {
        LogUtil.i("NewCallApi setWebView, $webView")
        val manager = findNewCallManager(sessionId)
        if (manager == null) {
            return RESULT_ERR_MISMATCH_SESSION
        }
        manager.setWebView(webView)
        return RESULT_OK
    }

    /**
     * Call this method when onActivityResult of activity get called
     * @param sessionId marks one call session. eg: slotId_remoteNumber
     * @param requestCode requestCode
     * @param resultCode resultCode
     * @param data data Intent
     * Call this method when intent handle result got.
     */
    fun onActivityResult(sessionId: String, requestCode: Int, resultCode: Int, data: Intent?): Int {
        LogUtil.i("NewCallApi onActivityResult, reqCode=$requestCode, resultCode=$resultCode, data=${data?.dataString}")
        val manager = findNewCallManager(sessionId)
        if (manager == null) {
            return RESULT_ERR_MISMATCH_SESSION
        }
        manager.onActivityResult(requestCode, resultCode, data)
        return RESULT_OK
    }

    /**
     * Call this method when onRequestPermissionsResult of activity get called
     * @param sessionId marks one call session. eg: slotId_remoteNumber
     * @param requestCode requestCode
     * @param permissions permissions
     * @param grantResults grantResults
     */
    fun onRequestPermissionsResult(
        sessionId: String,
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ): Int {
        LogUtil.i("NewCallApi onRequestPermissionsResult, reqCode=$requestCode, perm=$permissions, grant=$grantResults")
        val manager = findNewCallManager(sessionId)
        if (manager == null) {
            return RESULT_ERR_MISMATCH_SESSION
        }
        manager.onRequestPermissionsResult(requestCode, permissions, grantResults)
        return RESULT_OK
    }

    /**
     * Update current view state.
     * @param sessionId marks one call session. eg: slotId_remoteNumber
     * @param state view state
     * 1：ON_FOREGROUND_IN_VOICE_CALL
     * 2：ON_BACKGROUND
     * 3：ON_FOREGROUND_IN_VIDEO_CALL
     */
    fun onLifeCycleChanged(sessionId: String, state: LifeCycleState): Int {
        LogUtil.i("NewCallApi onLifeCycleChanged, state=$state")
        val manager = findNewCallManager(sessionId)
        if (manager == null) {
            return RESULT_ERR_MISMATCH_SESSION
        }
        manager.onLifeCycleChanged(state)
        return RESULT_OK
    }

    /**
     * Launch mini-app by appId. Only local cached mini-app will be rendered on shown webView.
     * @param sessionId marks one call session. eg: slotId_remoteNumber
     * @param appId id of the mini-app to launch
     */
    fun launchMiniApp(sessionId: String, appId: String): Int {
        LogUtil.i("NewCallApi launchMiniApp, appId=$appId")
        val manager = findNewCallManager(sessionId)
        if (manager == null) {
            return RESULT_ERR_MISMATCH_SESSION
        }
        manager.launchMiniApp(appId)
        return RESULT_OK
    }

    /**
     * Bind activity of InCallUI.
     * @param sessionId marks one call session. eg: slotId_remoteNumber
     * @param activity activity of InCallUI
     */
    fun bindInCallUI(sessionId: String, activity: Activity?): Int {
        LogUtil.i("NewCallApi bindInCallUI, $activity")
        val manager = findNewCallManager(sessionId)
        if (manager == null) {
            return RESULT_ERR_MISMATCH_SESSION
        }
        manager.bindInCallUI(activity)
        return RESULT_OK
    }

    /**
     * Show mini-app list.
     * @param sessionId marks one call session. eg: slotId_remoteNumber
     */
    fun showMiniAppList(sessionId: String): Int {
        LogUtil.i("NewCallApi showMiniAppList")
        val manager = findNewCallManager(sessionId)
        if (manager == null) {
            return RESULT_ERR_MISMATCH_SESSION
        }
        manager.showMiniAppList()
        return RESULT_OK
    }

    /**
     * set screenShare handler
     * @param sessionId marks one call session. eg: slotId_remoteNumber
     */
    fun setScreenShareHandler(sessionId: String, handler: ScreenShareHandler): Int {
        LogUtil.i("NewCallApi setScreenShareHandler")
        val manager = findNewCallManager(sessionId)
        if (manager == null) {
            return RESULT_ERR_MISMATCH_SESSION
        }
        manager.setScreenShareHandler(handler)
        return RESULT_OK
    }

    /**
     * set call state manually
     * @param sessionId marks one call session. eg: slotId_remoteNumber
     */
    fun setCallState(sessionId: String, callState: Int): Int {
        LogUtil.i("NewCallApi setCallState $callState")
        val manager = findNewCallManager(sessionId)
        if (manager == null) {
            return RESULT_ERR_MISMATCH_SESSION
        }
        manager.setCallState(callState)
        return RESULT_OK
    }
}