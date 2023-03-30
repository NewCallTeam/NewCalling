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


package com.cmcc.newcalllib.tool.constant

import com.cmcc.newcalllib.expose.NewCallApi

/**
 * which page to display
 * @author jihongfei
 * @createTime 2023/1/29 10:33
 */
enum class Display(val display: String) {
    AUTO_LOAD("autoload"),
    APP_LIST("applist");

    companion object {

        fun from(display: Int): Display {
            when (display) {
                NewCallApi.SHOW_AUTO_LOAD -> {
                    return AUTO_LOAD
                }
                else -> {
                    return APP_LIST
                }
            }
        }
    }
}