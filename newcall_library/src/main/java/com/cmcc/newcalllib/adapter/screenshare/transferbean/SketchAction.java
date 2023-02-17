package com.cmcc.newcalllib.adapter.screenshare.transferbean;

import androidx.annotation.IntDef;

import com.cmcc.newcalllib.adapter.screenshare.transferbean.msgbean.SketchMsgDrawing;
import com.cmcc.newcalllib.adapter.screenshare.transferbean.msgbean.SketchMsgUndo;
import com.cmcc.widget.bean.SketchInfoBean;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * 涂鸦传输动作：屏幕涂鸦的协商数据
 *
 * @author xiaxueliang@chinamobile.com
 * @since 2022/9/1
 */
public class SketchAction {

    /**
     * 涂鸦状态回调
     */
    @Retention(RetentionPolicy.SOURCE)
    // 限定取值范围
    @IntDef({SketchAction.Type.DRAWING, SketchAction.Type.UNDO})
    public @interface Type {
        // 绘制动作
        int DRAWING = 1;
        // 撤销动作
        int UNDO = 2;
    }

    // 动作id
    public String actionId;
    // 动作类型
    @SketchAction.Type
    public int actionType;
    // 绘制数据
    public SketchMsgDrawing drawing;
    // 撤销数据
    public SketchMsgUndo undo;


    public SketchAction() {
        this.actionId = String.valueOf(System.currentTimeMillis());
    }


    /**
     * 构建动作
     */
    public static class Builder {

        @SketchAction.Type
        private int actionType;
        // 涂鸦数据
        private List<SketchInfoBean> sketchInfoBeans;
        // 用于构建响应消息
        private SketchAction sketchAction;

        public Builder setActionType(int actionType) {
            this.actionType = actionType;
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

        /**
         * 生成Action到达状态的响应消息
         *
         * @param sketchAction
         */
        public Builder setSketchAction(SketchAction sketchAction) {
            this.sketchAction = sketchAction;
            return this;
        }

        public SketchAction build() {

            switch (actionType) {
                case Type.DRAWING:
                    if (sketchInfoBeans != null && sketchInfoBeans.size() > 0) {
                        SketchInfoBean sketchInfo = sketchInfoBeans.get(0);
                        return buildDrawingAction(sketchInfo);
                    }
                    break;
                case Type.UNDO:
                    if (sketchInfoBeans != null && sketchInfoBeans.size() > 0) {
                        return buildUndoAction(sketchInfoBeans);
                    }
                    break;
            }
            return null;
        }


        /**
         * 生成绘制动作
         *
         * @param sketchDrawingInfo
         * @return
         */
        private SketchAction buildDrawingAction(SketchInfoBean sketchDrawingInfo) {
            SketchAction sketchAction = new SketchAction();
            sketchAction.actionType = Type.DRAWING;
            //
            SketchMsgDrawing sketchMsgDrawing = new SketchMsgDrawing();
            sketchMsgDrawing.sketchId = sketchDrawingInfo.getSketchId();
            sketchMsgDrawing.sketchColor = sketchDrawingInfo.getSketchColor();
            sketchMsgDrawing.sketchDipWidth = sketchDrawingInfo.getSketchDipWidth();
            sketchMsgDrawing.quadMoveTo = sketchDrawingInfo.getQuadMoveTo();
            sketchMsgDrawing.quadControlPoints = sketchDrawingInfo.getQuadControlPoints();
            sketchMsgDrawing.quadEndPoints = sketchDrawingInfo.getQuadEndPoints();
            sketchAction.drawing = sketchMsgDrawing;
            return sketchAction;
        }

        /**
         * 生成undo动作
         *
         * @param sketchInfoBeans
         * @return
         */
        private SketchAction buildUndoAction(List<SketchInfoBean> sketchInfoBeans) {
            SketchAction sketchAction = new SketchAction();
            sketchAction.actionType = Type.UNDO;
            //
            SketchMsgUndo sketchMsgUndo = new SketchMsgUndo();
            //
            List<String> undoSketchIds = new ArrayList<String>();
            for (SketchInfoBean bean : sketchInfoBeans) {
                String sketchId = bean.getSketchId();
                undoSketchIds.add(sketchId);
            }
            sketchMsgUndo.undoSketchIds = undoSketchIds;
            sketchAction.undo = sketchMsgUndo;
            return sketchAction;
        }
    }


    @Override
    public String toString() {
        return "SketchAction{" +
                "id='" + actionId + '\'' +
                ", type=" + actionType +
                ", drawing=" + drawing +
                ", undo=" + undo +
                '}';
    }
}
