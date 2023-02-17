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

import android.content.Intent
import com.cmcc.newcalllib.manage.entity.Results
import com.cmcc.newcalllib.manage.support.Callback

/**
 * @author jihongfei
 * @createTime 2023/1/12 16:00
 */
interface STTController {

    /**
     * try start STT window
     */
    fun enableSTT(textSize: Int, dcLabel: String, callback: Callback<Results<Boolean>>?)

    /**
     * try stop STT window
     */
    fun disableSTT(callback: Callback<Results<Boolean>>?)

    /**
     * handle intent result
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)

}