package com.cmcc.newcalllib.expose;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebSettings;

import androidx.annotation.Nullable;

import com.cmcc.newcalllib.bridge.LegacyJsBridge;
import com.cmcc.newcalllib.bridge.BridgeHandler;
import com.cmcc.newcalllib.bridge.BridgeWebView;
import com.cmcc.newcalllib.bridge.CallBackFunction;
import com.cmcc.newcalllib.bridge.DefaultHandler;
import com.cmcc.newcalllib.manage.bussiness.interact.JsCommunicator;
import com.cmcc.newcalllib.tool.LogUtil;
import com.cmcc.newcalllib.tool.thread.ThreadPoolUtil;

import java.util.concurrent.Callable;

/**
 * WebView which supports JsBridge
 */
public class CmccBridgeWebView extends BridgeWebView {
    private final String TAG = CmccBridgeWebView.class.getSimpleName();
    private @Nullable JsCommunicator mCommunicator;
    private LegacyJsBridge mLegacyJsBridge;

    public CmccBridgeWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CmccBridgeWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public CmccBridgeWebView(Context context) {
        super(context);
    }

    @Override
    protected void init() {
        super.init();
        initCmccWebSettings();
        initListeners();
        initLegacyJsBridge();
    }

    private void initLegacyJsBridge() {
        mLegacyJsBridge = new LegacyJsBridge();
        addJavascriptInterface(mLegacyJsBridge, mLegacyJsBridge.getInterfaceName());
    }

    private void initListeners() {
        setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(KeyEvent.KEYCODE_BACK == keyCode && 0 == event.getRepeatCount()
                        && KeyEvent.ACTION_DOWN == event.getAction()) {
                    if (canGoBack()) {
                        LogUtil.INSTANCE.i("Webview go back on key_down action");
                        goBack();
                        // handle mna stack
                        if (mCommunicator != null) {
                            mCommunicator.getMiniAppManager().popMiniApp(null);
                        }
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private void initJsFuncCaller() {
        if (mCommunicator == null) {
            return;
        }
        mCommunicator.setJsFuncCaller((handleName, data, callBackFunction) -> {
            LogUtil.INSTANCE.d("callHandler on main thread, name=" + handleName);
            ThreadPoolUtil.INSTANCE.runOnUiThread(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    CmccBridgeWebView.this.callHandler(handleName, data, callBackFunction);
                    return null;
                }
            });
            return null;
        });
    }

    private void initJsBridgeHandlers() {
        if (mCommunicator == null) {
            return;
        }
        for (final String name : mCommunicator.getRegisteredFunctionName()) {
            registerHandler(name, new BridgeHandler() {
                @Override
                public void handler(String dataFromJs, CallBackFunction function) {
                    LogUtil.INSTANCE.d("CmccBridgeWebView: registerHandler.dataFromJs=" + dataFromJs);
                    if(!TextUtils.isEmpty(dataFromJs)){
                        mCommunicator.handleJsRequest(name, dataFromJs, function);
                    }
                }
            });
        }
        setDefaultHandler(new DefaultHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                LogUtil.INSTANCE.d("CmccBridgeWebView: setDefaultHandler.data=" + data);
                if(!TextUtils.isEmpty(data)){
                    mCommunicator.handleJsRequest(null, data, function);
                }
            }
        });
    }

    protected void initCmccWebSettings() {
        // TODO check settings
        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        // set zoom support
//        settings.setSupportZoom(true);
//        settings.setBuiltInZoomControls(true);
//        settings.setDisplayZoomControls(false);

        // enable database
        settings.setDatabaseEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAppCacheEnabled(false);

        // hide horizontal scroll bar
        setHorizontalScrollBarEnabled(false);
        setScrollbarFadingEnabled(true);
        setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        setOverScrollMode(View.OVER_SCROLL_NEVER);

        //set cache.
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        settings.setLoadsImagesAutomatically(true);
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
    }

    /**
     * Do not override
     */
    public void setupWithJsCommunicator(JsCommunicator communicator) {
        mCommunicator = communicator;
        initJsBridgeHandlers();
        initJsFuncCaller();
        if (mCommunicator != null) {
            mLegacyJsBridge.initJsCommunicator(mCommunicator);
        }
    }

}
