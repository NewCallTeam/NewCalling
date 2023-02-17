package com.cmcc.newcalllib.adapter.translate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.cmcc.newcalllib.R;
import com.cmcc.newcalllib.adapter.translate.bean.TranslateBean;
import com.cmcc.newcalllib.tool.DisplayHelper;
import com.cmcc.newcalllib.tool.TimeUtil;

import java.util.concurrent.Callable;

import bolts.Task;

/**
 * 智能翻译：语音转写；实时翻译
 *
 * @author xiaxueliang@chinamobile.com
 * @since 2022/12/21
 */
public class TranslateWindowHolder {

    private static final String TAG = "TranslateViewHolder";

    // 悬浮窗权限请求 ALART_WINDOW_PERMISSION_CODE
    private static final int ALART_WINDOW_PERMISSION_CODE = 10002;

    /**
     * 悬浮窗
     */
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mCtrlLPs;
    //
    // 涂鸦控制器
    private RelativeLayout mCtrlLayout;
    // 英语内容
    private TextView mContentEngTv;
    // 内容
    private TextView mContentTv;
    // 时间
    private TextView mTimeTv;

    /**
     * 上下文对象 与 后台录制服务
     */
    // 记录上下文对象
    private Activity mActivity;


    /**
     *
     */
    // 展示数据
    private TranslateBean mTranslateBean;


    /**
     * 主/被叫初始化Activity
     *
     * @param activity
     */
    public TranslateWindowHolder(Activity activity) {
        this.mActivity = activity;
        // 1、获取屏幕的宽高
        DisplayHelper.initDisplaySize(mActivity);
    }


    /**
     * 悬浮窗权限申请处理结果
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // 悬浮窗权限
            case TranslateWindowHolder.ALART_WINDOW_PERMISSION_CODE:
                // 检测悬浮窗权限，展示悬浮窗
                showTranslateWindow(mTranslateBean);
                break;
        }
    }


    /**
     * 检测悬浮窗权限，启动悬浮窗
     */
    public void showTranslateWindow(TranslateBean bean) {
        this.mTranslateBean = bean;
        if (mActivity == null) {
            return;
        }
        if (mTranslateBean == null) {
            return;
        }
        // 没有悬浮窗权限，展示悬浮窗权限申请
        if (!Settings.canDrawOverlays(mActivity)) {
            showWindowTipOnUiThread();
            return;
        }
        // 初始化UI
        initTranslateView();
        // 更新UI
        updateTranslateView(bean);
    }

    /**
     * 退出悬浮窗
     */
    public void hideTranslateWindow() {
        // 移除对应的view
        if (mWindowManager != null) {
            mWindowManager.removeView(mCtrlLayout);
        }
        mWindowManager = null;
    }


    /**
     * 显示控制按钮
     */
    private void initTranslateView() {
        //设置悬浮窗布局属性
        if (mWindowManager == null) {
            mWindowManager = (WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE);
            /**
             * 控制器View
             */
            //设置悬浮窗的布局
            mCtrlLayout = (RelativeLayout) LayoutInflater.from(mActivity).inflate(R.layout.translate_ctrl_layout, null);
            // LayoutParams
            mCtrlLPs = getWindowLayoutParams();
            //加载显示悬浮窗
            mWindowManager.addView(mCtrlLayout, mCtrlLPs);
            //
            mContentEngTv = mCtrlLayout.findViewById(R.id.translate_content_eng_tv);
            mContentTv = mCtrlLayout.findViewById(R.id.translate_content_tv);
            mTimeTv = mCtrlLayout.findViewById(R.id.translate_time_tv);
        }
    }

    /**
     * 数据更新
     *
     * @param bean
     */
    private void updateTranslateView(TranslateBean bean) {
        if (mWindowManager == null || mActivity == null) {
            return;
        }
        // 没有数据，不做UI刷新
        if (bean == null) {
            return;
        }
        // 实时翻译
        if (bean.getContentType() == TranslateBean.Type.SPEECH_TRANSLATION) {
            mContentEngTv.setVisibility(View.VISIBLE);
        } else {
            mContentEngTv.setVisibility(View.GONE);
        }
        //内容展示
        mContentEngTv.setText(bean.getContentEng());
        mContentTv.setText(bean.getContent());
        // 显示时间
        if (bean.getTime() > 0) {
            mTimeTv.setVisibility(View.VISIBLE);
            mTimeTv.setText(TimeUtil.getFormatHHMM(bean.getTime()));
        } else {
            mTimeTv.setVisibility(View.GONE);
        }
    }

    /**
     * 更新字体大小
     *
     * @param txtSizeDp
     */
    public void updateTranslateTextSize(int txtSizeDp) {
        // 字体大小
        if (txtSizeDp > 0) {
            int txtSize = DisplayHelper.dp2px(mActivity, txtSizeDp);
            mContentEngTv.setTextSize(txtSize);
            mContentTv.setTextSize(txtSize);
        }
    }


    //运行在UI线程中
    private Task<Boolean> showWindowTipOnUiThread() {
        //
        return Task.call(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                // UI_Thread
                if (mActivity == null) {
                    return true;
                }
                // 没有悬浮窗权限
                AlertDialog dialog = new AlertDialog
                        .Builder(mActivity).setTitle("申请权限").setMessage("应用需申请悬浮窗权限，以展示屏幕共享控制按钮！").setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO 取消：没权限
                    }
                }).setPositiveButton("设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 进入设置页面
                        String ACTION_MANAGE_OVERLAY_PERMISSION = "android.settings.action.MANAGE_OVERLAY_PERMISSION";
                        Intent intent = new Intent(ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + mActivity.getPackageName()));
                        mActivity.startActivityForResult(intent, TranslateWindowHolder.ALART_WINDOW_PERMISSION_CODE);
                    }
                }).create();
                dialog.show();
                return true;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }


    /**
     * WindowManager.LayoutParams
     */
    private WindowManager.LayoutParams getWindowLayoutParams() {
        WindowManager.LayoutParams lps = new WindowManager.LayoutParams();
        //设置类型
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            lps.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            lps.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        // 设置行为选项
        lps.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        //设置悬浮窗的显示位置
        lps.gravity = Gravity.TOP | Gravity.LEFT;
        //如果悬浮窗图片为透明图片，需要设置该参数为PixelFormat.RGBA_8888
        lps.format = PixelFormat.RGBA_8888;
        //设置悬浮窗的宽/高
        lps.width = WindowManager.LayoutParams.MATCH_PARENT;
        lps.height = WindowManager.LayoutParams.WRAP_CONTENT;
        // 屏幕左下角为起始点
        lps.gravity = Gravity.LEFT | Gravity.BOTTOM;
        //设置x、y轴偏移量
        lps.x = 0;
        lps.y = DisplayHelper.dp2px(mActivity, 185);
        return lps;
    }


}
