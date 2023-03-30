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

package com.cmcc.newcalllib.manage.ext.ar

import com.cmcc.newcalllib.adapter.network.NetworkAdapter
import java.nio.ByteBuffer

/**
 * @author jihongfei
 * @createTime 2022/11/29 10:00
 */
class DataChannelHandlerImpl(val networkAdapter: NetworkAdapter) : DataChannelHandler {

    lateinit var passedDataCallback: (String, ByteBuffer) -> Unit

    override fun sendData(label: String, data: String, callback: (Int) -> Unit) {
        networkAdapter.sendDataOnAppDC(label, data, object : NetworkAdapter.RequestCallback {
            override fun onSendDataCallback(statusCode: Int, errorCode: Int) {
                callback.invoke(statusCode)
            }
        })
    }

    override fun registerDataCallback(callback: (String, ByteBuffer) -> Unit) {
        passedDataCallback = callback
    }
}