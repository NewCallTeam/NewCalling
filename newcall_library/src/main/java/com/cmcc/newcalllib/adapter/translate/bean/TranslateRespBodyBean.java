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

package com.cmcc.newcalllib.adapter.translate.bean;

import androidx.annotation.NonNull;


/**
 * @author xiaxl
 * @createTime 2023/2/1 15:15
 * <p>
 * 接口定义 详见《中国移动智能翻译业务数据交互接口规范》 6.2媒体能力平台数据交互接口
 */
public class TranslateRespBodyBean implements java.io.Serializable {

    private String returnCode;
    private String description;

    public String getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(String returnCode) {
        this.returnCode = returnCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(" returnCode: ");
        sb.append(returnCode);
        sb.append(" description: ");
        sb.append(description);
        return sb.toString();
    }
}
