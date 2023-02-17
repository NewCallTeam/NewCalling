package com.cmcc.newcalllib.manage.bussiness

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.telecom.Call
import com.cmcc.newcalllib.manage.ext.ExtensionManager
import com.cmcc.newcalllib.adapter.network.ImsDCNetworkAdapter
import com.cmcc.newcalllib.adapter.network.LabelDecoratorImpl
import com.cmcc.newcalllib.adapter.network.NetworkAdapter
import com.cmcc.newcalllib.adapter.network.data.NetworkConfig
import com.cmcc.newcalllib.adapter.ntv.NativeAbilityProvider
import com.cmcc.newcalllib.adapter.ntv.NativeAbilityProviderImpl
import com.cmcc.newcalllib.expose.*
import com.cmcc.newcalllib.manage.bussiness.interact.BootstrapJsHandler
import com.cmcc.newcalllib.manage.bussiness.interact.CommonMiniAppJsHandler
import com.cmcc.newcalllib.manage.bussiness.interact.JsCommunicator
import com.cmcc.newcalllib.manage.entity.CallInfo
import com.cmcc.newcalllib.manage.entity.Results
import com.cmcc.newcalllib.manage.entity.event.NotifyAppMessage
import com.cmcc.newcalllib.manage.entity.event.NotifyJsMessage
import com.cmcc.newcalllib.manage.entity.WebViewSize
import com.cmcc.newcalllib.manage.entity.event.RequestWebViewChangeData
import com.cmcc.newcalllib.manage.event.EventAppNotify
import com.cmcc.newcalllib.manage.event.EventInternal
import com.cmcc.newcalllib.manage.event.EventJsNotify
import com.cmcc.newcalllib.manage.event.EventRender
import com.cmcc.newcalllib.manage.mna.MiniAppManager
import com.cmcc.newcalllib.manage.support.*
import com.cmcc.newcalllib.manage.support.storage.StorageManager
import com.cmcc.newcalllib.tool.LogUtil
import com.cmcc.newcalllib.tool.constant.*

/**
 * Business management layer
 */
class NewCallManager(override val sessionId: String, private val callInfo: CallInfo) : CallSession {
    companion object {
    }

    private var mWebView: CmccBridgeWebView? = null
    private lateinit var mContext: Context
    private lateinit var mJsCommunicator: JsCommunicator
    private lateinit var mNetworkAdapter: NetworkAdapter
    private lateinit var mNativeAbilityProvider: NativeAbilityProvider
    private lateinit var mExtensionManager: ExtensionManager
    private lateinit var mMiniAppManager: MiniAppManager
    private lateinit var mHandler: MainThreadEventHandler
    private lateinit var mPathManager: PathManager
    private lateinit var mStorageManager: StorageManager
    private var mErrListener: ErrorListener? = null
    private var mMsgListener: MessageListener? = null
    private var mMiniAppChangeListener: MiniAppChangeListener? = null
    private var mCurrCallState : Int = Call.STATE_NEW
    private var mCurrPagePhase : String = Phase.PRE_CALL.phaseName
    private var mHasReleased = false
    private var mHasBsPageShownInPreCall = false
    private var mHasBsPageShownInInCall = false
    private var mHasBsPageShowing = false

    private val mCallStateCallback: Call.Callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            handleCallStateChange(state)
        }
    }

    /**
     * init newcall manager
     */
    fun init(context: Context, call: Call): Boolean {
        call.registerCallback(mCallStateCallback)
        mContext = context
        mHandler = MainThreadEventHandler()
        mPathManager = PathManager(context, sessionId)
        CallSessionManager.init(sessionId, callInfo)
        // init call state, in case of listener state loss
        mCurrCallState = call.state
        callInfo.callStatus = mCurrCallState
        updateCurrentPagePhase()
        LogUtil.i("init, callState=${callStateToString(mCurrCallState)}, callPhase=$mCurrPagePhase")

        initNetAdapter(context)
        initNtvAbilityProvider(context)
        initExtManager(context)
        initMiniAppManager(context)
        initJsCommunicator()
        return true
    }

    /**
     * release newcall manager
     */
    fun release() {
        if (!mHasReleased) {
            mHandler.removeCallbacksAndMessages(null)
            mNetworkAdapter.release()
            mExtensionManager.onRelease(callInfo.slotId, callInfo.callId)
        }
        // reset call state
        mCurrCallState = Call.STATE_NEW
        callInfo.callStatus = Call.STATE_NEW
        mHasReleased = true
        // clear webView
        mWebView?.clearHistory()
        mWebView?.clearCache(false)
    }

    fun setErrListeners(errListener: ErrorListener?) {
        mErrListener = errListener
    }

    fun setMessageListener(msgListener: MessageListener?) {
        mMsgListener = msgListener
    }

    fun setMiniAppChangeListener(miniAppChangeListener: MiniAppChangeListener?) {
        mMiniAppChangeListener = miniAppChangeListener
    }

    fun onLifeCycleChanged(state: LifeCycleState) {
        mJsCommunicator.visibilityNotify(state.value, null)
        mExtensionManager.getScreenShareManager().onActivityVisibilityNotify(state.value)
    }

    fun bindInCallUI(activity: Activity?) {
        mNativeAbilityProvider.bindActivity(activity)
        mExtensionManager.getScreenShareManager().activity = activity
    }

    private fun initNetAdapter(context: Context) {
        mNetworkAdapter = ImsDCNetworkAdapter(
                handler = mHandler,
                networkConfig = NetworkConfig.Builder()
                    .setHost("")
                    .setSlotId(callInfo.slotId)
                    .setBufferAmount(Constants.DC_BUFFER_AMOUNT_BYTES)
                    .build(),
                labelDecorator = LabelDecoratorImpl()
        )
        mNetworkAdapter.init(context)
    }

    private fun initNtvAbilityProvider(context: Context) {
        mNativeAbilityProvider = NativeAbilityProviderImpl(context, mPathManager)
    }

    private fun initExtManager(context: Context) {
        mExtensionManager = ExtensionManager(context,
            mNetworkAdapter,
            mNativeAbilityProvider,
            mHandler)
    }

    private fun initMiniAppManager(context: Context) {
        mStorageManager = StorageManager(context, sessionId)
        mStorageManager.initStorageManager()
        mStorageManager.resetSpInSession()
        mMiniAppManager = MiniAppManager(
            mNetworkAdapter,
            mNativeAbilityProvider,
            mStorageManager.getMiniAppRepo(),
            mStorageManager.getTransferFileRepo(),
            mStorageManager.getMiniAppSpaceRepo(),
            mPathManager
        )
        mMiniAppManager.initMiniAppManager()
    }

    private fun initJsCommunicator() {
        mJsCommunicator = JsCommunicator(
            mHandler,
            mNetworkAdapter,
            mNativeAbilityProvider,
            mExtensionManager,
            mStorageManager,
            mMiniAppManager,
            callInfo
        )
        mJsCommunicator.setBootstrapJsHandler(BootstrapJsHandler())
        mJsCommunicator.setCommonJsHandler(CommonMiniAppJsHandler())
    }

    /**
     * listen call state change
     */
    private fun handleCallStateChange(state: Int) {
        mCurrCallState = state
        callInfo.callStatus = state
        LogUtil.d("onCallStateChange, state change to ${callStateToString(state)}")

        val newPhase = Phase.fromCallState(state).phaseName
        when (state) {
            Call.STATE_NEW -> {
            }
            Call.STATE_CONNECTING, Call.STATE_DIALING, CustomizationCallState.VIVO_ALERTING.state -> {
                // pre call
                loadCachedHomePage(newPhase)
            }
            Call.STATE_RINGING -> {

            }
            Call.STATE_ACTIVE -> {
                // in call
                loadCachedHomePage(newPhase)
            }
            Call.STATE_HOLDING -> {
            }
            Call.STATE_DISCONNECTING, Call.STATE_DISCONNECTED -> {
                release()
            }
        }
        updateCurrentPagePhase()
        // notify mini app
        mJsCommunicator.callStateNotify(state, null)
        // notify extension manager
        mExtensionManager.onCallStateChanged(state)
    }

    private fun callStateToString(state: Int): String {
        return when (state) {
            Call.STATE_NEW -> "NEW"
            Call.STATE_RINGING -> "RINGING"
            Call.STATE_DIALING -> "DIALING"
            Call.STATE_ACTIVE -> "ACTIVE"
            Call.STATE_HOLDING -> "HOLDING"
            Call.STATE_DISCONNECTED -> "DISCONNECTED"
            Call.STATE_CONNECTING -> "CONNECTING"
            Call.STATE_DISCONNECTING -> "DISCONNECTING"
            Call.STATE_SELECT_PHONE_ACCOUNT -> "SELECT_PHONE_ACCOUNT"
            Call.STATE_SIMULATED_RINGING -> "SIMULATED_RINGING"
            Call.STATE_AUDIO_PROCESSING -> "AUDIO_PROCESSING"
            CustomizationCallState.VIVO_ALERTING.state -> "VIVO_ALERTING"
            else -> {
                "UNKNOWN"
            }
        }
    }

    /**
     * try find local bootstrap mini-app, then load with phase
     */
    private fun loadCachedHomePage(phase: String) {
        LogUtil.i("Load home page with phase=$phase")
        if (mWebView == null) {
            LogUtil.w("loadCachedHomePage, WebView is null")
            return
        }
//        if (mCurrPagePhase == phase) {
//            LogUtil.w("loadCachedHomePage, Already in phase: $mCurrPagePhase")
//            return
//        }
        if (!mMiniAppManager.getBsAppPrepared()) {
            LogUtil.i("loadCachedHomePage, Bootstrap mini-app not prepared, no need to load now")
            return
        }
        if ((phase == Phase.PRE_CALL.phaseName && mHasBsPageShownInPreCall)
            || (phase == Phase.IN_CALL.phaseName && mHasBsPageShownInInCall)) {
            LogUtil.i("loadCachedHomePage, Bootstrap mini-app already shown")
            return
        }
        findLocalBsAppThenRender(phase)
    }

    fun setWebView(webView: CmccBridgeWebView?) {
        LogUtil.d("setWebView in curr phase=$mCurrPagePhase")
        // keep
        mWebView = webView
        // setup
        @Suppress("IfThenToSafeAccess")
        if (webView != null) {
            webView.setupWithJsCommunicator(mJsCommunicator)
            webView.setNtvAbilityHandler(mNativeAbilityProvider.provideNtvAbilityHandler())
            ConfigManager.webViewSize = WebViewSize(webView.width, webView.height)
            // try render
            when (mCurrPagePhase) {
                Phase.PRE_CALL.phaseName -> {
                    loadCachedHomePage(mCurrPagePhase)
                }
                Phase.IN_CALL.phaseName -> {
                }
            }
        } else {
            mHasBsPageShownInPreCall = false
            mHasBsPageShownInInCall = false
            getMiniAppManager().clearMiniAppStack()
        }
    }

    fun getWebView(): CmccBridgeWebView? {
        return mWebView
    }

    fun getMiniAppManager(): MiniAppManager {
        return mMiniAppManager
    }

    fun getCurrState(): Int {
        return mCurrCallState
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        mNativeAbilityProvider.onActivityResult(requestCode, resultCode, data)
        mExtensionManager.onActivityResult(requestCode, resultCode, data)
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        mNativeAbilityProvider.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    fun launchMiniApp(appId: String) {
        LogUtil.w("launchMiniApp not support yet")
    }

    fun showMiniAppList() {
        LogUtil.d("try showMiniAppList")
        if (mWebView == null || mCurrPagePhase != Phase.IN_CALL.phaseName) {
            LogUtil.w("showMiniAppList fail, mWebView=$mWebView")
            return
        }
        if (mMiniAppManager.getBsAppPrepared()) {
            findLocalBsAppThenRender(mCurrPagePhase, true)
        } else {
            LogUtil.d("showMiniAppList on listener")
            // waiting bootstrap prepared
            mMiniAppManager.setBootstrapMiniAppPrepareListener(object :
                MiniAppManager.BootstrapMiniAppPrepareListener {
                override fun onPrepared() {
                    findLocalBsAppThenRender(mCurrPagePhase, true)
                }
            })
        }
    }

    fun setScreenShareHandler(handler: ScreenShareHandler) {
        mExtensionManager.getScreenShareManager().screenShareHandler = handler
    }

    fun setCallState(callState: Int) {
        LogUtil.d("setCallState $callState and update phase")
        mCurrCallState = callState
        updateCurrentPagePhase()
    }

    /**
     * find cached bootstrap mini-app first, if exists then render index.html with
     * token and phase
     */
    private fun findLocalBsAppThenRender(curPhase: String, showAppList: Boolean = false) {
        LogUtil.i("findLocalBsAppThenRender, curPhase=$curPhase, showList=$showAppList")
        if (mHasBsPageShowing) {
            LogUtil.w("findLocalBsAppThenRender, page loading")
            return
        }
        mHasBsPageShowing = true
        mMiniAppManager.findBootstrapAppPath(object : Callback<Results<String>> {
            override fun onResult(t: Results<String>) {
                if (t.getOrNull() != null) {
                    val queryStr = MiniAppManager.buildBsAppQuery(curPhase)
                    val path = t.getOrNull()!!
                    // use route 'applist', or auto launch app by bs app
                    val ev = if (!showAppList) {
                        EventRender(path = path, query = queryStr)
                    } else {
                        EventRender(path = "$path#/applist", query = queryStr)
                    }
                    doRender(ev)
                    // update flag
                    updateBsShownFlag(curPhase)
                } else {
                    LogUtil.e("Handle load home page failure", t.exception())
                }
            }
        })
    }

    private fun updateBsShownFlag(curPhase: String) {
        if (curPhase == Phase.PRE_CALL.phaseName) {
            mHasBsPageShownInPreCall = true
        } else if (curPhase == Phase.IN_CALL.phaseName) {
            mHasBsPageShownInInCall = true
        }
        mHasBsPageShowing = false
    }

    /**
     * render by passed url or local path
     */
    private fun doRender(event: EventRender) {
        if (getWebView() != null) {
            LogUtil.i("Do render loadUrl $event")
            getWebView()!!.loadUrl(event.url)
        } else {
            LogUtil.e("Render with webView null")
        }
    }

    private fun updateCurrentPagePhase() {
        val phase = Phase.fromCallState(getCurrState()).phaseName
        LogUtil.d("updateCurrentPagePhase, " +
                "mCurrPagePhase=$mCurrPagePhase, phase=$phase")
        // update phase
        if (phase != mCurrPagePhase) {
            LogUtil.i("update phase: $mCurrPagePhase to $phase")
            mCurrPagePhase = phase
        }
    }

    /**
     * try render bootstrap mini-app, download if not exist or version out of date
     */
    private fun prepareAndRenderBsApp() {
        try {
            getMiniAppManager().prepareBootstrapMiniApp(object :
                Callback<Results<String>> {
                override fun onResult(t: Results<String>) {
                    val currState = getCurrState()
                    LogUtil.d("Bootstrap prepare finish, t=$t, curr callState=$currState")

                    if (t.isSuccess() && t.getOrNull() != null) {
                        val path = t.getOrNull()!!
                        if (Phase.isPreCallOrInCall(currState)
                            && getWebView() != null
                        ) {
                            // render with phase and token
                            val phase = Phase.fromCallState(currState).phaseName
                            val queryStr = MiniAppManager.buildBsAppQuery(
                                phase
                            )
                            val ev = EventRender(path, queryStr)
                            doRender(ev)
                            // update flag
                            updateBsShownFlag(phase)
                        } else {
                            LogUtil.i("Stop render, call state=${callStateToString(currState)}, webView=${getWebView()}")
                        }
                    } else {
                        LogUtil.e(
                            "Handle bootstrap app prepare failure"
                        )
                    }

                }
            })
        } catch (e: Exception) {
            LogUtil.e("Prepare bootstrap mini-app fail", e)
        }
    }

    /**
     * Event handler
     * @author jihongfei
     * @createTime 2022/3/18 16:23
     */
    @SuppressLint("HandlerLeak")
    inner class MainThreadEventHandler : Handler(Looper.getMainLooper()) {

        /**
         * send error event
         */
        fun sendErrorCode(ec: ErrorCode) {
            sendMessageType(EventType.EXCEPTION, ec)
        }

        /**
         * send message with eventType
         */
        fun sendMessageType(mt: EventType, arg: Any?) {
            val msg = Message.obtain()
            msg.what = mt.code()
            msg.obj = arg
            sendMessage(msg)
        }

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                // error
                EventType.EXCEPTION.code() -> {
                    // TODO ExceptionHandler
                    val ec = msg.obj as ErrorCode
                    mErrListener?.onFail(ec.code(), ec.reason())
                    LogUtil.e("Receive exception event=${ec.code()}")
                }
                // render
                EventType.RENDER.code() -> {
                    val event = msg.obj as EventRender
                    LogUtil.d("Receive render event=${event}")
                    doRender(event)
                }
                // internal event
                EventType.INTERNAL.code() -> {
                    val event = msg.obj as EventInternal
                    LogUtil.d("Receive internal event=$event")
                    when (event.eventType) {
                        EventInternal.ET_BOOTSTRAP_CREATED -> {
                            // request bootstrap app
                            prepareAndRenderBsApp()
                        }
                    }
                }
                // js notify
                EventType.JS_NOTIFY.code() -> {
                    val event = msg.obj as EventJsNotify<*>
                    LogUtil.d("Receive js notify event=$event")
                    val data = event.data as NotifyJsMessage
//                    val label = ""//check label
//                    mJsCommunicator.messageNotify(label, JsonUtil.toJson(data), null)
                }
                // app notify
                EventType.APP_NOTIFY.code() -> {
                    val event = msg.obj as EventAppNotify<*>
                    LogUtil.d("Receive app notify event=$event")
                    when(event.eventType) {
                        EventAppNotify.ET_REQUEST_WEB_VIEW_CHANGE -> {
                            val data = event.data as RequestWebViewChangeData
                            if (data.width != null && data.height != null) {
                                mMiniAppChangeListener?.requestWebViewSizeChange(data.width, data.height)
                                ConfigManager.webViewSize = WebViewSize(data.width, data.height)
                            }
                            if (data.visibility != null) {
                                mMiniAppChangeListener?.requestWebViewVisibilityChange(data.visibility)
                            }
                        }
                        EventAppNotify.ET_NOTIFY_APP_BY_JS -> {
                            LogUtil.d("notify app by js")
                            val message = event.data as NotifyAppMessage
                            mMsgListener?.onMessage(message.type, message.data)
                        }
                    }
                }
            }
        }
    }
}