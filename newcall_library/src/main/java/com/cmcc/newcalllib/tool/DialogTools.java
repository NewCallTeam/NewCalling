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

package com.cmcc.newcalllib.tool;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.cmcc.newcalllib.R;

public class DialogTools {


    public static Dialog showTipDialog(Context context, String title, String tip, String sureStr, String cancelStr,
                                       DialogBtnClickCallBack callBack) {
        return showTipDialog(context, title, tip, sureStr, cancelStr, R.color.holo_blue_dark, R.color.content_color, callBack);
    }

    public static Dialog showTipDialog(final Context context, final String title, final String tip, final String sureStr, final String cancelStr,
                                       final int sureColor, final int cancelColor, final DialogBtnClickCallBack callBack) {
        return DialogTools.showCustomDialog(context, R.layout.dialog_common_tip, new DialogTools.DialogInitListener() {
            @Override
            public void onDialogInitListener(final Dialog dialog) {

                TextView sureTv = (TextView) dialog.findViewById(R.id.ok_tv);
                TextView cancelTv = (TextView) dialog.findViewById(R.id.cancel_tv);

                TextView titleTv = (TextView) dialog.findViewById(R.id.mgs_title);
                TextView content = (TextView) dialog.findViewById(R.id.mgs_content);
                //标题
                if (!TextUtils.isEmpty(title)) {
                    titleTv.setVisibility(View.VISIBLE);
                    titleTv.setText(title);
                } else {
                    titleTv.setVisibility(View.GONE);
                }
                //内容
                if (!TextUtils.isEmpty(tip)) {
                    content.setVisibility(View.VISIBLE);
                    content.setText(tip);
                } else {
                    content.setVisibility(View.GONE);
                }

                //确定按钮
                if (!TextUtils.isEmpty(sureStr)) {
                    sureTv.setText(sureStr);
                } else {
                    sureTv.setText(R.string.dialog_sure);
                }
                //取消按钮
                if (!TextUtils.isEmpty(cancelStr)) {
                    cancelTv.setText(cancelStr);
                } else {
                    cancelTv.setText(R.string.dialog_cancel);
                }
                sureTv.setTextColor(context.getResources().getColor(sureColor));
                cancelTv.setTextColor(context.getResources().getColor(cancelColor));
                sureTv.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        callBack.onSure(dialog);
                    }
                });
                cancelTv.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        callBack.onCancel(dialog);
                    }
                });
            }
        });
    }

    public static Dialog showCustomDialog(Context context, int yourDlgLayout, DialogInitListener l) {
        return showCustomDialog(context, yourDlgLayout, true, l);
    }

    /**
     * 显示自定义Dialog
     *
     * @param context
     * @param yourDlgLayout 你的自定义Dialog布局
     * @param l             初始化Dialog控件
     */
    public static Dialog showCustomDialog(Context context, int yourDlgLayout, boolean cancelable, DialogInitListener l) {
        return showCustomDialog(context, yourDlgLayout, -1, cancelable, true, l);
    }

    /**
     * 显示自定义Dialog
     *
     * @param context
     * @param yourDlgLayout 你的自定义Dialog布局
     * @param l             初始化Dialog控件
     */
    public static Dialog showCustomDialog(Context context,
                                          int yourDlgLayout, int layoutWidth,
                                          boolean cancelableOnTouchOutside, boolean cancelableOnBack,
                                          DialogInitListener l) {
        if (context == null) {
            return null;
        }
        AlertDialog dlg = new AlertDialog.Builder(context).create();
        // 点击其他区域dialog消失
        dlg.setCanceledOnTouchOutside(cancelableOnTouchOutside);
        dlg.setCancelable(cancelableOnBack);
        if (!dlg.isShowing()) {
            try {
                dlg.show();
            } catch (WindowManager.BadTokenException e) {
                return null;
            }
        }
        WindowManager.LayoutParams params = dlg.getWindow().getAttributes();
        if (layoutWidth > 0) {
            params.width = layoutWidth;
        } else {
            params.width = DisplayHelper.dp2px(context, 288);
        }
        dlg.getWindow().setAttributes(params);
        Window window = dlg.getWindow();
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.setContentView(yourDlgLayout);
        if (l != null)
            l.onDialogInitListener(dlg);
        return dlg;
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~回调接口~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public interface DialogInitListener {
        void onDialogInitListener(Dialog dialog);
    }

    public interface DialogBtnClickCallBack {
        void onSure(Dialog dialog);

        void onCancel(Dialog dialog);
    }


}
