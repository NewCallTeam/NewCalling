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
public class TranslateBodyBean implements java.io.Serializable {


    // 是否转写/翻译最终结果：0 否，1 是。说明：转写/翻译过程，会返回所有的转写/翻译中间结果和最终结果，终端侧根据自身需要决定如何做展示。
    private String speechRecogType;
    // 识别出的文字
    private String sourceInfo;
    // 在使用翻译功能时，表示翻译后的内容
    private String targetInfo;
    // 时间戳
    private long time;

    public String getSpeechRecogType() {
        return speechRecogType;
    }

    public void setSpeechRecogType(String speechRecogType) {
        this.speechRecogType = speechRecogType;
    }

    public String getSourceInfo() {
        return sourceInfo;
    }

    public void setSourceInfo(String sourceInfo) {
        this.sourceInfo = sourceInfo;
    }

    public String getTargetInfo() {
        return targetInfo;
    }

    public void setTargetInfo(String targetInfo) {
        this.targetInfo = targetInfo;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(" speechRecogType: ");
        sb.append(speechRecogType);
        sb.append(" sourceInfo: ");
        sb.append(sourceInfo);
        sb.append(" targetInfo: ");
        sb.append(targetInfo);
        sb.append(" time: ");
        sb.append(time);
        return sb.toString();
    }
}
