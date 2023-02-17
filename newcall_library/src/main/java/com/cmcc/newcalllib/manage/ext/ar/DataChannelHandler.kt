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

import java.nio.ByteBuffer

/**
 * TODO replace interface provided by ar-sdk
 * @author jihongfei
 * @createTime 2022/11/29 9:59
 */
interface DataChannelHandler {

    /**
     * send data over given dc.
     * @param label dcLabel
     * @param data string data to send
     * @param callback with send status code
     */
    fun sendData(
        label: String,
        data: String,
        callback: ((Int) -> Unit)
    )

    /**
     * register dc data callback
     * @param callback dcLabel in String, data in ByteBuffer
     */
    fun registerDataCallback(
        callback: ((String, ByteBuffer) -> Unit)
    )
}