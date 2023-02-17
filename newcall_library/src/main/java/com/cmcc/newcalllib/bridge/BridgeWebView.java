/*
 * Copyright (c) 2023 China Mobile Communications Group Co.,Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cmcc.newcalllib.bridge;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.webkit.WebView;

import com.cmcc.newcalllib.adapter.ntv.NtvAbilityHandler;
import com.cmcc.newcalllib.tool.LogUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressLint("SetJavaScriptEnabled")
public class BridgeWebView extends WebView implements WebViewJavascriptBridge {

	private final String TAG = "BridgeWebView";

	// fmtCbId to CallbackFunction
	Map<String, CallBackFunction> responseCallbacks = new HashMap<String, CallBackFunction>();
	Map<String, BridgeHandler> messageHandlers = new HashMap<String, BridgeHandler>();
	BridgeHandler defaultHandler = new DefaultHandler();

	private List<Message> startupMessage = new ArrayList<Message>();

	public List<Message> getStartupMessage() {
		return startupMessage;
	}

	public void setStartupMessage(List<Message> startupMessage) {
		this.startupMessage = startupMessage;
	}

	public void setNtvAbilityHandler(NtvAbilityHandler ntvAbilityHandler) {
		BridgeWebChromeClient chromeClient = (BridgeWebChromeClient) getWebChromeClient();
		chromeClient.setNtvAbilityHandler(ntvAbilityHandler);
	}

	private long uniqueId = 0;

	public BridgeWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public BridgeWebView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public BridgeWebView(Context context) {
		super(context);
		init();
	}

	/**
	 * 
	 * @param handler
	 *            default handler,handle messages send by js without assigned handler name,
     *            if js message has handler name, it will be handled by named handlers registered by native
	 */
	public void setDefaultHandler(BridgeHandler handler) {
       this.defaultHandler = handler;
	}

    protected void init() {
		this.setVerticalScrollBarEnabled(false);
		this.setHorizontalScrollBarEnabled(false);
		this.getSettings().setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
		this.setWebViewClient(generateBridgeWebViewClient());
		this.setWebChromeClient(generateBridgeWebChromeClient());
	}

    protected BridgeWebViewClient generateBridgeWebViewClient() {
        return new BridgeWebViewClient(this);
    }

    protected BridgeWebChromeClient generateBridgeWebChromeClient() {
        return new BridgeWebChromeClient();
    }

	void handlerReturnData(String url) {
		String functionName = BridgeUtil.getFunctionFromReturnUrl(url);
		CallBackFunction f = responseCallbacks.get(functionName);
		String data = BridgeUtil.getDataFromReturnUrl(url);
		if (f != null) {
			f.onCallBack(data);
			responseCallbacks.remove(functionName);
			return;
		}
	}

	@Override
	public void send(String data) {
		send(data, null);
	}

	@Override
	public void send(String data, CallBackFunction responseCallback) {
		doSend(null, data, responseCallback);
	}

	/**
	 * 缓存Callback，封装Message向JS发送
	 * @param handlerName
	 * @param data
	 * @param responseCallback
	 */
	private void doSend(String handlerName, String data, CallBackFunction responseCallback) {
		Message m = new Message();
		if (!TextUtils.isEmpty(data)) {
			m.setData(data);
		}
		if (responseCallback != null) {
			String callbackStr = String.format(BridgeUtil.CALLBACK_ID_FORMAT, ++uniqueId + (BridgeUtil.UNDERLINE_STR + SystemClock.currentThreadTimeMillis()));
			responseCallbacks.put(callbackStr, responseCallback);
			m.setCallbackId(callbackStr);
		}
		if (!TextUtils.isEmpty(handlerName)) {
			m.setHandlerName(handlerName);
		}
		queueMessage(m);
	}

	/**
	 * 如当前WebView还未完全加载，缓存在startupMessage List。否则序列化后调用特定格式的JS Cmd向JS传递消息
	 * @param m message
	 */
	private void queueMessage(Message m) {
		if (startupMessage != null) {
			startupMessage.add(m);
		} else {
			dispatchMessage(m);
		}
	}

	void dispatchMessage(Message m) {
        String messageJson = m.toJson();
        //escape special characters for json string
        messageJson = messageJson.replaceAll("(\\\\)([^utrn])", "\\\\\\\\$1$2");
        messageJson = messageJson.replaceAll("(?<=[^\\\\])(\")", "\\\\\"");
        String javascriptCommand = String.format(BridgeUtil.JS_HANDLE_MESSAGE_FROM_JAVA, messageJson);
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            this.loadUrl(javascriptCommand);
        }
    }

	/**
	 * 从JS消息队列获取消息，解析为Message后分发
	 */
	void flushMessageQueue() {
		if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
			loadUrl(BridgeUtil.JS_FETCH_QUEUE_FROM_JAVA, new CallBackFunction() {

				@Override
				public void onCallBack(String data) {
					// deserializeMessage
					List<Message> list = null;
					try {
						list = Message.toArrayList(data);
					} catch (Exception e) {
                        e.printStackTrace();
						return;
					}
					if (list == null || list.size() == 0) {
						return;
					}
					for (int i = 0; i < list.size(); i++) {
						Message m = list.get(i);
						String responseId = m.getResponseId();
						CallBackFunction function;
						// 是否是response
						if (!TextUtils.isEmpty(responseId)
								&& (function = responseCallbacks.get(responseId)) != null) {
							String responseData = m.getResponseData();
							function.onCallBack(responseData);
							responseCallbacks.remove(responseId);
						} else {
							CallBackFunction responseFunction = null;
							// if had callbackId
							final String callbackId = m.getCallbackId();
							if (!TextUtils.isEmpty(callbackId)) {
								responseFunction = new CallBackFunction() {
									@Override
									public void onCallBack(String data) {
										// data from native func result
										Message responseMsg = new Message();
										responseMsg.setResponseId(callbackId);
										responseMsg.setResponseData(data);
										queueMessage(responseMsg);
									}
								};
							} else {
								responseFunction = new CallBackFunction() {
									@Override
									public void onCallBack(String data) {
										// do nothing
									}
								};
							}
							BridgeHandler handler;
							if (!TextUtils.isEmpty(m.getHandlerName())) {
								handler = messageHandlers.get(m.getHandlerName());
							} else {
								handler = defaultHandler;
							}
							if (handler != null){
								handler.handler(m.getData(), responseFunction);
							}
						}
					}
				}
			});
		}
	}

	public void loadUrl(String jsUrl, CallBackFunction returnCallback) {
		this.loadUrl(jsUrl);
		responseCallbacks.put(BridgeUtil.parseFunctionName(jsUrl), returnCallback);
	}

	/**
	 * register handler,so that javascript can call it
	 * 
	 * @param handlerName
	 * @param handler
	 */
	public void registerHandler(String handlerName, BridgeHandler handler) {
		if (handler != null) {
			messageHandlers.put(handlerName, handler);
		}
	}

	/**
	 * call javascript registered handler
	 *
     * @param handlerName
	 * @param data
	 * @param callBack
	 */
	public void callHandler(String handlerName, String data, CallBackFunction callBack) {
        doSend(handlerName, data, callBack);
	}
}
