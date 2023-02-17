/*
 * Copyright (c) 2022 China Mobile Communications Group Co.,Ltd. All rights reserved.
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

package com.cmcc.newcalllib.manage.ext.ar

import android.view.Surface
import com.cmcc.newcalllib.adapter.ar.aidl.ARAdapter
import com.cmcc.newcalllib.adapter.network.NetworkAdapter
import com.cmcc.newcalllib.manage.entity.Results
import com.cmcc.newcalllib.manage.ext.ExtensionManager
import com.cmcc.newcalllib.manage.support.Callback
import com.cmcc.newcalllib.tool.LogUtil
import com.cmcc.newcalllib.tool.toStr
import java.nio.ByteBuffer

/**
 * AugmentedReality manager class
 * @author jihongfei
 * @createTime 2022/9/27 11:14
 */
class AugmentedRealityManager(private val extensionManager: ExtensionManager) :
    AugmentedRealityController {
    companion object {
    }
    private var mARInitial = false
    private var mDataChannelLabels = mutableListOf<String>()
    private lateinit var mDataChannelHandler: DataChannelHandlerImpl
    private val localARAdapter = null/*LocalARAdapterImpl(extensionManager.cxt)*/
    private val aidlARAdapter = extensionManager.networkAdapter as ARAdapter

    override fun isARCallAvailable(callback: Callback<Results<Boolean>>) {
        // TODO ask dialer or chip
        callback.onResult(Results(true))
    }

    override fun startARCall(labels: List<String>, slotId: Int, callId: String, callback: Callback<Results<Boolean>>?) {
        LogUtil.d("startARCall")
        val localARAdapter = localARAdapter
        if (!mARInitial) {
            init(labels)
        }
//        if (localARAdapter.checkARAbility()) {
//            // aidl start
//            aidlARAdapter.setARCallback(object : ARAdapter.ARCallback {
//                override fun onGetSurface(surface: Surface, slotId: Int, callId: String) {
//                    LogUtil.d("onGetSurface")
//                    // call ar sdk to start
////                    localARAdapter.startARAbility(surface, object: SimpleLocalARCallback() {
////                        override fun onStartARCallback(status: Int) {
////                            LogUtil.d("localARAdapter startARAbility status=$status")
////                            // TODO check status code
////                            callback?.onResult(Results.success(true))
////                        }
////                    })
//                }
//            })
//            aidlARAdapter.startARAbility(slotId, callId, object : Callback<Results<Int>> {
//                override fun onResult(t: Results<Int>) {
//                    LogUtil.d("aidlARAdapter startARAbility onResult, ${t.value}")
//                }
//            })
//        } else {
//            callback?.onResult(Results.failure("ar ability absent"))
//        }
    }

    override fun stopARCall(slotId: Int, callId: String, callback: Callback<Results<Boolean>>?) {
        LogUtil.d("stopARCall")
        // call ar sdk to stop
//        localARAdapter.stopARAbility(object: SimpleLocalARCallback() {
//            override fun onStopARCallback(status: Int) {
//                LogUtil.d("localARAdapter stopARAbility status=$status")
//                // aidl stop
//                aidlARAdapter.stopARAbility(slotId, callId, object : Callback<Results<Int>> {
//                    override fun onResult(t: Results<Int>) {
//                        LogUtil.d("aidlARAdapter stopARAbility onResult, ${t.value}")
//                        // TODO check status code
//                        callback?.onResult(Results.success(true))
//                    }
//                })
//            }
//        })

        // state reset
        reset()
    }

    private fun init(labels: List<String>) {
        LogUtil.d("init ar manager")
        mARInitial = true
        mDataChannelLabels.clear()
        mDataChannelLabels.addAll(labels)


        // TODO init ar sdk with dc handler
        // setStoragePath
        // setDataChannelLabels
        // setDataChannelHandler
        mDataChannelHandler = DataChannelHandlerImpl(extensionManager.networkAdapter)

        // intercept data arrive
        labels.forEach {
            registerDataInterceptor(it)
        }
    }

    private fun reset() {
        mARInitial = false
        mDataChannelLabels.clear()
    }

    private fun registerDataInterceptor(dcLabel: String) {
        extensionManager.networkAdapter.registerDataInterceptor(object : NetworkAdapter.DataInterceptor {
            override fun provideDataChannelLabel(): String {
                return dcLabel
            }

            override fun onDataArrive(data: ByteBuffer): Boolean {
                val dataStr = data.toStr()
                LogUtil.d("dc($dcLabel) onDataArrive:$dataStr")
                mDataChannelHandler.passedDataCallback.invoke(dcLabel, data)
                return true
            }
        })
    }
}