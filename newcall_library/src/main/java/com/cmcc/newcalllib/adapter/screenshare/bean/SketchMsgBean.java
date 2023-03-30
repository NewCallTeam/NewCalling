/*
 * Copyright (c) 2023 China Mobile Communications Group Co.,Ltd. All rights reserved.
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
package com.cmcc.newcalllib.adapter.screenshare.bean;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.cmcc.widget.bean.PointBean;

import java.util.List;

/**
 * 涂鸦动作数据：绘制数据
 *
 * @author xiaxueliang@chinamobile.com
 * @since 2022/9/1
 */
public class SketchMsgBean  implements java.io.Serializable{
    // 当前这一笔的唯一标识id
    public String sketchId;
    // 画笔颜色
    @ColorInt
    public int sketchColor;
    // 画笔粗细（DP）
    public float sketchDipWidth;
    // 贝塞尔曲线的 起点
    public PointBean quadMoveTo;
    // 贝塞尔曲线的 控制点
    public List<PointBean> quadControlPoints;
    // 贝塞尔曲线的 终点
    public List<PointBean> quadEndPoints;


    @NonNull
    @Override
    public String toString() {
        return "SketchMsgBean{" +
                "sketchId='" + sketchId +
                ", sketchColor=" + sketchColor +
                ", sketchDipWidth=" + sketchDipWidth +
                ", quadMoveTo=" + quadMoveTo +
                ", quadControlPoints=" + quadControlPoints +
                ", quadEndPoints=" + quadEndPoints +
                "}";
    }




}
