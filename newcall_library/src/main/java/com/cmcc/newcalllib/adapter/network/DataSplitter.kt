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

package com.cmcc.newcalllib.adapter.network

import java.nio.ByteBuffer

/**
 * split data before send over data channel
 * @author jihongfei
 * @createTime 2022/11/4 11:00
 */
interface DataSplitter {

    /**
     * split data. eg: insert terminate-flag in the end
     * @param subProtocol sub protocol
     * @param label dc label
     * @param buffer raw data
     * @param limit per chunk size
     * @param callback callback of split data. invoke multiple times for every chunk
     */
    fun split(subProtocol: String, label: String, buffer: ByteBuffer, limit: Long, callback: (buffer: ByteBuffer) -> Any)
}