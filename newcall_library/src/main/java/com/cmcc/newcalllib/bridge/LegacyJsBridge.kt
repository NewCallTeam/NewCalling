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

package com.cmcc.newcalllib.bridge

import android.webkit.JavascriptInterface
import com.cmcc.newcalllib.manage.bussiness.interact.JsCommunicator
import com.cmcc.newcalllib.tool.LogUtil
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * @author jihongfei
 * @createTime 2022/8/29 11:04
 */
class LegacyJsBridge() {
    companion object {
        const val INTERFACE_NAME = "LegacyJsBridge"
    }

    private var mCommunicator: JsCommunicator? = null

    fun initJsCommunicator(communicator: JsCommunicator) {
        LogUtil.d("initJsCommunicator in LegacyJsBridge")
        mCommunicator = communicator
    }

    fun getInterfaceName(): String {
        return INTERFACE_NAME
    }

    @JavascriptInterface
    fun callFunctionSync(funcName: String, dataFromJs: String): String {
        LogUtil.d("callFunction in legacyJsBridge, " +
                "funcName=$funcName, dataFromJs=$dataFromJs, mCommunicator=$mCommunicator")
        var handleResult = ""
        // convert async process to synchronous
        runBlocking {
            handleResult = handleRequestSync(funcName, dataFromJs)
        }
        return handleResult
    }

    private suspend fun handleRequestSync(funcName: String, dataFromJs: String): String {
        val handleResult = suspendCoroutine<String> { continuation ->
            mCommunicator?.handleJsRequest(funcName, dataFromJs) { callbackParam ->
                continuation.resume(callbackParam)
            }
        }
        return handleResult
    }
}