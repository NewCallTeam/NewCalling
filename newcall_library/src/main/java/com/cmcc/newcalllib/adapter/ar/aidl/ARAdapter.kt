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

package com.cmcc.newcalllib.adapter.ar.aidl

import android.view.Surface
import com.cmcc.newcalllib.manage.entity.Results
import com.cmcc.newcalllib.manage.support.Callback

/**
 * adapter of AR call, call AIDL API
 * @author jihongfei
 * @createTime 2022/12/9 11:56
 */
interface ARAdapter {
    fun startARAbility(slotId: Int, callId: String, callback: Callback<Results<Int>>?)
    fun stopARAbility(slotId: Int, callId: String, callback: Callback<Results<Int>>?)
    fun setARCallback(callback: ARCallback)

    interface ARCallback {
        fun onGetSurface(surface: Surface, slotId: Int, callId: String)
    }
}