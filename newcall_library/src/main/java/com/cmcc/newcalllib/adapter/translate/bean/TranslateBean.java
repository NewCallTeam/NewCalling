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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * @author xiaxl
 * @createTime 2023/2/1 15:15
 * <p>
 * // contentId: 消息id
 * // contentType: 1 语音转写;  2 实时翻译
 * // time : 当前时间戳
 * // content: 中文内容
 * // contentEng: 要翻译的语言内容
 * {"contentId":"contentId","contentType":2,"content":"你好!","contentEng":"Hello","time":1676448103104}
 */
public class TranslateBean {

    // 注解仅存在于源码中，在class字节码文件中不包含
    @Retention(RetentionPolicy.SOURCE)
    // 限定取值范围为{SPEECH_TO_TEXT, SPEECH_TRANSLATION}
    @IntDef({Type.SPEECH_TO_TEXT, Type.SPEECH_TRANSLATION})
    public @interface Type {
        int SPEECH_TO_TEXT = 1; // 语音转写
        int SPEECH_TRANSLATION = 2; // 实时翻译
    }


    private String contentId;
    // 1 语音转写  2 实时翻译
    private int contentType;
    private String content;
    private String contentEng;
    private long time;

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public int getContentType() {
        return contentType;
    }

    public void setContentType(int contentType) {
        this.contentType = contentType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentEng() {
        return contentEng;
    }

    public void setContentEng(String contentEng) {
        this.contentEng = contentEng;
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
        sb.append("contentId: ");
        sb.append(contentId);
        sb.append(" contentType: ");
        sb.append(contentType);
        sb.append(" content: ");
        sb.append(content);
        sb.append(" contentEng: ");
        sb.append(contentEng);
        sb.append(" time: ");
        sb.append(time);
        return sb.toString();
    }
}
