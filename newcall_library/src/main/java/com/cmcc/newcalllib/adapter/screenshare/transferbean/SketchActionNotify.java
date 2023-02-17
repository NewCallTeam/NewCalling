package com.cmcc.newcalllib.adapter.screenshare.transferbean;

import com.cmcc.newcalllib.adapter.screenshare.transferbean.msgbean.SketchMsgDrawing;
import com.cmcc.newcalllib.adapter.screenshare.transferbean.msgbean.SketchMsgUndo;
import com.cmcc.widget.bean.SketchInfoBean;

import java.util.ArrayList;
import java.util.List;

/**
 * 涂鸦传输动作回应：涂鸦绘制动作：Drawing 与 Undo 两个消息是否接收到的响应
 *
 * @author xiaxueliang@chinamobile.com
 * @since 2022/9/1
 */
public class SketchActionNotify {

    // 响应状态码
    public int notifyStatus;
    // 响应消息
    public String notifyMsg;
    // Action对应的数据
    public SketchAction notifyActionData;

    /**
     * 构建动作
     */
    public static class Builder {

        private SketchAction sketchAction;


        /**
         * 生成Action到达状态的响应消息
         *
         * @param sketchAction
         */
        public SketchActionNotify.Builder setSketchAction(SketchAction sketchAction) {
            this.sketchAction = sketchAction;
            return this;
        }

        public SketchActionNotify build() {
            return buildNotify(sketchAction);
        }


        /**
         * 生成noty动作
         *
         * @return
         */
        private SketchActionNotify buildNotify(SketchAction sketchAction) {
            // TODO
            SketchActionNotify sketchActionNotify = new SketchActionNotify();
            sketchActionNotify.notifyStatus = 200;
            sketchActionNotify.notifyMsg = "OK action：" + sketchAction.actionType;
            sketchActionNotify.notifyActionData = sketchAction;
            return sketchActionNotify;
        }

    }


}
