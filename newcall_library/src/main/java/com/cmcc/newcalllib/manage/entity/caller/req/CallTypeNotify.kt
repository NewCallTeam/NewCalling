/*
 * Copyright (c) 2023 China Mobile Communications Group Co.,Ltd. All rights reserved.
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

package com.cmcc.newcalllib.manage.entity.caller.req

import android.telecom.VideoProfile

/**
 * callType: 0 for audioCall, 1 for videoCall
 * videoState of [android.telecom.VideoProfile#mVideoState]
 * @author jihongfei
 * @createTime 2023/3/6 10:56
 */
data class CallTypeNotify(
    val callType: Int
) {
    companion object {
        fun fromVideoState(videoState: Int): Int {
            return when (videoState) {
                VideoProfile.STATE_AUDIO_ONLY -> 1
                else -> 0
            }
        }
    }
}
