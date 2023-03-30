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
    // display
    public const val SHOW_AUTO_LOAD = 1
    public const val SHOW_APP_LIST = 2
    // message type
    public const val MESSAGE_ON_BOOTSTRAP_READY = 0
    public const val MESSAGE_REPORT_PARSED_BUSINESS = 1

    private const val RESULT_OK = 1
    private const val RESULT_FAIL = 0
    private const val RESULT_ERR_ALREADY_INIT = -1
    private const val RESULT_ERR_PARAMS_ILLEGAL = -2
    private const val RESULT_ERR_MISMATCH_SESSION = -3

    private var mManagerMap = mutableMapOf<String, NewCallManager>()
    private var mCurrentSessionId: String? = null

    private fun findNewCallManager(sessionId: String): NewCallManager? {
        LogUtil.d("NewCallApi find $sessionId in ${mManagerMap.keys.joinToString()}")
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
        LogUtil.i("NewCallApi initNewCall, info=$info, sessionId=$sessionId, ver=${BuildConfig.VERSION_NAME}")

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
     * Update webView visibility
     * @param sessionId marks one call session. eg: slotId_remoteNumber
     * @param visible true if visible
     */
    fun onWebViewVisibilityChanged(sessionId: String, visible: Boolean): Int {
        LogUtil.i("NewCallApi onWebViewVisibilityChanged, visible=$visible")
        val manager = findNewCallManager(sessionId)
        if (manager == null) {
            return RESULT_ERR_MISMATCH_SESSION
        }
        manager.onWebViewVisibilityChanged(visible)
        return RESULT_OK
    }

    /**
     * Update lifeCycle of InCalActivity
     * @param sessionId marks one call session. eg: slotId_remoteNumber
     * @param state view state
     * 1):ON_CREATE
     * 2):ON_START
     * 3):ON_RESUME
     * 4):ON_PAUSE
     * 5):ON_STOP
     * 6):ON_DESTROY
     */
    fun onActivityLifeCycleChanged(sessionId: String, state: LifeCycleState): Int {
        LogUtil.i("NewCallApi onActivityLifeCycleChanged, state=$state")
        val manager = findNewCallManager(sessionId)
        if (manager == null) {
            return RESULT_ERR_MISMATCH_SESSION
        }
        manager.onActivityLifeCycleChanged(state)
        return RESULT_OK
    }

    /**
     * Update videoState of the call
     * @param sessionId marks one call session. eg: slotId_remoteNumber
     * @param videoState video state
     */
    fun onCallTypeChanged(sessionId: String, videoState: Int): Int {
        LogUtil.i("NewCallApi onCallTypeChanged, videoState=$videoState")
        val manager = findNewCallManager(sessionId)
        if (manager == null) {
            return RESULT_ERR_MISMATCH_SESSION
        }
        manager.onCallTypeChanged(videoState)
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
     * Show mini-app home page.
     * @param sessionId marks one call session. eg: slotId_remoteNumber
     */
    fun showHomePage(sessionId: String, display: Int): Int {
        LogUtil.i("NewCallApi showHomePage display=$display")
        val manager = findNewCallManager(sessionId)
        if (manager == null) {
            return RESULT_ERR_MISMATCH_SESSION
        }
        manager.showHomePage(display)
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