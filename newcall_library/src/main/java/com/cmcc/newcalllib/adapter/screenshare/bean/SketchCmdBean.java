/**
 * Copyright (c) 2022 China Mobile Communications Group Co.,Ltd. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cmcc.newcalllib.adapter.screenshare.bean;

import androidx.annotation.IntDef;

import com.cmcc.widget.bean.SketchInfoBean;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * 涂鸦传输控制命令：
 * 1、控制命令：展示悬浮窗、屏幕共享退出
 * 2、屏幕涂鸦数据
 *
 * @author xiaxueliang@chinamobile.com
 * @since 2022/9/1
 */
public class SketchCmdBean implements java.io.Serializable{

    /**
     * 涂鸦状态回调
     */
    @Retention(RetentionPolicy.SOURCE)
    // 限定取值范围
    @IntDef({CmdType.WINDOW_SHOW, CmdType.WINDOW_EXIT, CmdType.SKETCH_DATA})
    public @interface CmdType {
        // 控制命令：展示悬浮窗
        int WINDOW_SHOW = 1;
        // 控制命令：退出
        int WINDOW_EXIT = 2;
        // 绘制命令：涂鸦数据
        int SKETCH_DATA = 3;
    }

    // 动作id
    private String cmdId;
    // 命令的类型
    @CmdType
    private int cmdType;

    //成员变量
    private SketchMsgBean sketchData;


    public SketchCmdBean() {
        this.cmdId = String.valueOf(System.currentTimeMillis());
    }

    public String getCmdId() {
        return cmdId;
    }

    public int getCmdType() {
        return cmdType;
    }

    public SketchMsgBean getSketchData() {
        return sketchData;
    }

    /**
     * 构建动作
     */
    public static class Builder {

        @CmdType
        private int cmdType = CmdType.WINDOW_SHOW;

        // 涂鸦数据
        private List<SketchInfoBean> sketchInfoBeans;

        /**
         * @param cmdType
         */
        public Builder setCmdType(@CmdType int cmdType) {
            this.cmdType = cmdType;
            return this;
        }

        public Builder setSketchInfo(SketchInfoBean sketchInfoBean) {
            if (sketchInfoBean != null) {
                sketchInfoBeans = new ArrayList<SketchInfoBean>();
                sketchInfoBeans.add(sketchInfoBean);
            }
            return this;
        }

        public Builder setSketchInfoList(List<SketchInfoBean> sketchInfoBeans) {
            this.sketchInfoBeans = sketchInfoBeans;
            return this;
        }

        public SketchCmdBean build() {
            switch (cmdType) {
                // 控制命令
                case CmdType.WINDOW_SHOW:
                case CmdType.WINDOW_EXIT:
                    SketchCmdBean sketchCmdBean = new SketchCmdBean();
                    sketchCmdBean.cmdType = cmdType;
                    return sketchCmdBean;
                // 绘制命令
                case CmdType.SKETCH_DATA:
                    if (sketchInfoBeans != null && sketchInfoBeans.size() > 0) {
                        sketchCmdBean = buildSketchDrawing(sketchInfoBeans.get(0));
                        sketchCmdBean.cmdType = cmdType;
                        return sketchCmdBean;
                    }
                    break;
            }
            return null;
        }


        /**
         * 生成绘制命令
         *
         * @param sketchInfo
         * @return
         */
        private SketchCmdBean buildSketchDrawing(SketchInfoBean sketchInfo) {
            SketchCmdBean sketchCmdBean = new SketchCmdBean();
            //
            SketchMsgBean sketchMsgBean = new SketchMsgBean();
            sketchMsgBean.sketchId = sketchInfo.getSketchId();
            sketchMsgBean.sketchColor = sketchInfo.getSketchColor();
            sketchMsgBean.sketchDipWidth = sketchInfo.getSketchDipWidth();
            sketchMsgBean.quadMoveTo = sketchInfo.getQuadMoveTo();
            sketchMsgBean.quadControlPoints = sketchInfo.getQuadControlPoints();
            sketchMsgBean.quadEndPoints = sketchInfo.getQuadEndPoints();
            //
            sketchCmdBean.sketchData = sketchMsgBean;
            return sketchCmdBean;
        }
    }


    @Override
    public String toString() {
        return "SketchAction{" +
                "id='" + cmdId +
                ", type=" + cmdType +
                ", sketchData=" + sketchData +
                "}";
    }
}
