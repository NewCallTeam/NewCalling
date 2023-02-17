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

package com.cmcc.newcalllib.manage.ext.stt

import android.app.Activity
import android.content.Intent
import android.telecom.Call
import android.text.TextUtils
import com.cmcc.newcalllib.adapter.network.ImsDCNetworkAdapter
import com.cmcc.newcalllib.adapter.network.NetworkAdapter
import com.cmcc.newcalllib.adapter.screenshare.SketchWindowHolder
import com.cmcc.newcalllib.adapter.screenshare.SketchWindowHolder.CallType
import com.cmcc.newcalllib.adapter.screenshare.transferbean.SketchAction
import com.cmcc.newcalllib.expose.ScreenShareHandler
import com.cmcc.newcalllib.expose.ScreenShareStatus
import com.cmcc.newcalllib.expose.ScreenShareStatusListener
import com.cmcc.newcalllib.manage.entity.CallInfo
import com.cmcc.newcalllib.manage.entity.NewCallException
import com.cmcc.newcalllib.manage.entity.Results
import com.cmcc.newcalllib.manage.entity.handler.req.ReqControlScreenShare
import com.cmcc.newcalllib.manage.ext.ExtensionManager
import com.cmcc.newcalllib.manage.support.Callback
import com.cmcc.newcalllib.tool.Base64Util
import com.cmcc.newcalllib.tool.LogUtil
import com.cmcc.newcalllib.tool.toStr
import com.cmcc.widget.SketchView
import com.cmcc.widget.bean.SketchInfoBean
import com.google.gson.Gson
import org.json.JSONException
import org.json.JSONObject
import java.nio.ByteBuffer
import java.util.*

/**
 * STT manager class
 * @author jihongfei
 * @createTime 2023/1/12 14:14
 */
class STTManager(private val extensionManager: ExtensionManager) :
    STTController {
    companion object {
    }

    var activity: Activity? = null

    // DC label for SDK STT use
    var dataChannelLabel: String? = null


    private fun getActivityOrThrow(): Activity {
        if (activity != null) {
            return activity!!
        }
        if (extensionManager.cxt is Activity) {
            return extensionManager.cxt
        }
        throw NewCallException("STT no activity")
    }

    private fun registerDataInterceptor(dcLabel: String) {
        extensionManager.networkAdapter.registerDataInterceptor(object : NetworkAdapter.DataInterceptor {
            override fun provideDataChannelLabel(): String {
                return dcLabel
            }

            override fun onDataArrive(data: ByteBuffer): Boolean {
                // 收到涂鸦数据
                val dataStr = data.toStr()
                LogUtil.d("STT onDataArrive:$dataStr")
                // TODO sdk use
                return true
            }
        })
    }

    override fun enableSTT(
        textSize: Int,
        dcLabel: String,
        callback: Callback<Results<Boolean>>?
    ) {
        LogUtil.d("enableSTT")
//        if (screenShareDataChannelLabel == null) {
//            screenShareDataChannelLabel = dcLabel
//            registerDataInterceptor(dcLabel)
//            LogUtil.i("enableScreen, set dcLabel first: $dcLabel")
//        } else if (screenShareDataChannelLabel != dcLabel) {
//            throw NewCallException("dcLabel different with previous in screenShare")
//        }
        // TODO show window
    }

    override fun disableSTT(callback: Callback<Results<Boolean>>?) {
        LogUtil.d("disableSTT")
        // TODO hide window
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        LogUtil.d( "STTManager, onActivityResult, req=$requestCode, res=$resultCode")
    }

}