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

package com.cmcc.newcalllib.manage.ext.screenshare

import android.content.Intent
import com.cmcc.newcalllib.manage.entity.Results
import com.cmcc.newcalllib.manage.support.Callback

/**
 * @author jihongfei
 * @createTime 2022/9/22 16:00
 */
interface ScreenShareController {

    /**
     * try start screen share
     */
    fun enableScreenShare(role: Int, dcLabel: String, callback: Callback<Results<Boolean>>)

    /**
     * try stop screen share
     */
    fun disableScreenShare()

    /**
     * check screen share env
     */
    fun isScreenShareAvailable(): Boolean

    /**
     * handle intent result
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)

    /**
     * handle call state change
     */
    fun onCallStateChanged(state: Int)

    /**
     * activity 显示\隐藏状态通知与回调
     */
    fun onActivityVisibilityNotify(state: Int)
}