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

/**
 * apply operation on dc label
 * @author jihongfei
 * @createTime 2022/11/10 12:59
 */
interface LabelDecorator {
    /**
     * parse appId from dc label
     * @param label dc label
     * @return appId
     */
    fun parseAppId(label: String): String?

    /**
     * add origin(local or remote) in dc label if origin absent
     * @param label original dc label
     * @return dc label with origin
     */
    fun addOrigin(origin: String, label: String): String

    /**
     * remove origin(local or remote) in dc label if origin exits
     * @param label original dc label
     * @return dc label
     */
    fun removeOrigin(label: String): String
}