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
 * @author jihongfei
 * @createTime 2022/11/15 11:10
 */
class LabelDecoratorImpl: LabelDecorator {
    companion object {
        const val DELIMITER = "_"
    }
    override fun parseAppId(label: String): String? {
        if (label.isNotEmpty()) {
            val arr = label.split(DELIMITER)
            if (arr.size == 4) {
                return arr[1]
            } else if (arr.size == 3) {
                return arr[0]
            }
        }
        return null
    }

    override fun addOrigin(origin: String, label: String): String {
        if (label.isNotEmpty()) {
            val arr = label.split(DELIMITER)
            if (arr.size == 4) {
                return label
            } else if (arr.size == 3) {
                return "$origin$DELIMITER$label"
            }
        }
        return label
    }

    override fun removeOrigin(label: String): String {
        if (label.isNotEmpty()) {
            val arr = label.split(DELIMITER)
            if (arr.size == 4) {
                return "${arr[1]}$DELIMITER${arr[2]}$DELIMITER${arr[3]}"
            }
        }
        return label
    }
}