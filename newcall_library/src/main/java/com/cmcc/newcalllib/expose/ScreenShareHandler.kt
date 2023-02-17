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

package com.cmcc.newcalllib.expose

/**
 * Handler interface for screen share, implement by Dialer.
 */
interface ScreenShareHandler {
    /**
     * try start screen share. need call requestNativeScreenShareAbility first
     * @param listener status listener
     */
    fun startNativeScreenShare(listener: ScreenShareStatusListener)

    /**
     * stop screen share
     */
    fun stopNativeScreenShare()

    /**
     * check screen share env ready or or not.
     * @return true or false
     */
    fun requestNativeScreenShareAbility(): Boolean
}