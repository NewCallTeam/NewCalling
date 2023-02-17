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

import com.cmcc.newcalllib.manage.entity.Results
import com.cmcc.newcalllib.manage.support.Callback

/**
 * @author jihongfei
 * @createTime 2022/11/29 16:00
 */
interface AugmentedRealityController {
    fun isARCallAvailable(callback: Callback<Results<Boolean>>)
    fun startARCall(labels: List<String>, slotId: Int, callId: String, callback: Callback<Results<Boolean>>?)
    fun stopARCall(slotId: Int, callId: String, callback: Callback<Results<Boolean>>?)
}