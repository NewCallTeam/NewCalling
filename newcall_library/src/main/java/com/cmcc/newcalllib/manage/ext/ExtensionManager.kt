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

package com.cmcc.newcalllib.manage.ext

import android.content.Context
import android.content.Intent
import com.cmcc.newcalllib.adapter.network.NetworkAdapter
import com.cmcc.newcalllib.adapter.ntv.NativeAbilityProvider
import com.cmcc.newcalllib.manage.bussiness.NewCallManager
import com.cmcc.newcalllib.manage.ext.screenshare.ScreenShareManager
import com.cmcc.newcalllib.manage.ext.ar.AugmentedRealityManager
import com.cmcc.newcalllib.manage.ext.stt.STTManager

/**
 * Extended services manager, with basic network and native ability
 * @author jihongfei
 * @createTime 2022/9/27 11:16
 */
class ExtensionManager(
    val cxt: Context,
    val networkAdapter: NetworkAdapter,
    val nativeAbilityProvider: NativeAbilityProvider,
    val handler: NewCallManager.MainThreadEventHandler
) {
    private val screenShareManager: ScreenShareManager = ScreenShareManager(this)
    private val arManager: AugmentedRealityManager = AugmentedRealityManager(this)
    private val sttManager: STTManager = STTManager(this)

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        screenShareManager.onActivityResult(requestCode, resultCode, data)
        sttManager.onActivityResult(requestCode, resultCode, data)
    }

    fun getScreenShareManager(): ScreenShareManager {
        return screenShareManager
    }

    fun getARManager(): AugmentedRealityManager {
        return arManager
    }

    fun getSTTManager(): STTManager {
        return sttManager
    }

    fun onCallStateChanged(state: Int) {
        screenShareManager.onCallStateChanged(state)
        sttManager.onCallStateChanged(state)
    }

    fun onRelease(slotId: Int, callId: String) {
        screenShareManager.disableScreenShare()
        arManager.stopARCall(slotId, callId, null)
        sttManager.disableSTT(null)
    }
}