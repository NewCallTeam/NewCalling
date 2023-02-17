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

package com.cmcc.newcalllib.tool.constant

enum class CustomizationCallState(val state: Int) {

    // For Vendor(Vivo) telecom customization. State ALERTING(101) would be reported by telecom
    // during precall phase.
    VIVO_ALERTING(101);

}