package com.cmcc.newcalllib.adapter.screenshare.transferbean.msgbean;

import androidx.annotation.ColorInt;

import com.cmcc.widget.bean.PointBean;

import java.util.List;

/**
 * 涂鸦动作数据：绘制数据
 *
 * @author xiaxueliang@chinamobile.com
 * @since 2022/9/1
 */
public class SketchMsgDrawing {
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

}
