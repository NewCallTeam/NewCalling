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
 * aggregate data received from data channel
 * @author jihongfei
 * @createTime 2022/11/4 11:02
 */
interface DataAggregator {
    /**
     * aggregate data
     * @param subProtocol subProtocol of dc, such as HTTP
     * @param dcLabel label of dc
     * @param byteBuffer input data
     * @param callback aggregate data handle callback. invoke callback if completely received
     */
    fun aggregate(subProtocol: String, dcLabel: String, byteBuffer: ByteBuffer,
                  callback: (buffer: ByteBuffer) -> Any)
}