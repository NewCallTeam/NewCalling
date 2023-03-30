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

package com.cmcc.newcalllib.adapter.ar.local

import android.view.Surface

/**
 * work with AR-SDK included
 * @author jihongfei
 * @createTime 2022/12/9 11:56
 */
interface LocalARAdapter {
    fun checkARAbility(): Boolean
    fun startARAbility(surface: Surface?, callback: LocalARCallback?)
    fun stopARAbility(callback: LocalARCallback?)
}