package com.cmcc.newcalllib.adapter.screenshare;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;

import com.cmcc.newcalllib.R;
import com.cmcc.newcalllib.adapter.screenshare.widget.SketchCtrlLayout;
import com.cmcc.newcalllib.tool.DialogTools;
import com.cmcc.newcalllib.tool.DisplayHelper;
import com.cmcc.newcalllib.tool.LogUtil;
import com.cmcc.widget.SketchView;
import com.cmcc.widget.bean.SketchInfoBean;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Task;

/**
 * 屏幕涂鸦悬浮窗封装类，对于ScreenShareController来说，类似于viewholder
 *
 * @author xiaxueliang@chinamobile.com
 * @since 2022/9/1
 */
public class SketchWindowHolder {

    private static final String TAG = "SketchWindowHolder";

    // 悬浮窗权限请求 ALART_WINDOW_PERMISSION_CODE
    private static final int ALART_WINDOW_PERMISSION_CODE = 10001;

    // 注解仅存在于源码中，在class字节码文件中不包含
    @Retention(RetentionPolicy.SOURCE)
    // 限定取值范围为{MO, MT}
    @IntDef({CallType.MO, CallType.MT})
    public @interface CallType {
        int MO = 1; // 主叫
        int MT = 2; // 被叫
    }

    // 涂鸦蒙版是否存在
    private boolean mSketchViewVisible = false;

    /**
     * 悬浮窗
     */
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mSketchLPs;
    private WindowManager.LayoutParams mCtrlLPs;
    //
    // view
    private View mSketchLayout;
    // 涂鸦View
    private SketchView mSketchView;
    //
    // 涂鸦控制器
    private SketchCtrlLayout mCtrlLayout;
    // 退出屏幕共享
    private View mScreenShareExitBtn;
    // 涂鸦开关
    private View mSketchSwitchBtn;

    /**
     * 上下文对象 与 后台录制服务
     */
    // 记录上下文对象
    private Activity mActivity;
    private SketchWindowListener mCallback;


    /**
     * 属性动画
     */
    private ObjectAnimator mSketchAnimator;

    /**
     *
     */
    // 主叫/被叫
    @CallType
    private int mCallType = CallType.MO;


    /**
     * 主/被叫初始化Activity
     *
     * @param activity
     */
    public SketchWindowHolder(Activity activity) {
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
            case SketchWindowHolder.ALART_WINDOW_PERMISSION_CODE:
                // 检测悬浮窗权限，展示悬浮窗
                showSketchControlWindow(mCallType);
                break;
        }
    }

    /**
     * 监听涂鸦事件回调
     *
     * @param callback
     */
    public void setSketchWindowListener(SketchWindowListener callback) {
        mCallback = callback;
    }


    /**
     * 检测悬浮窗权限，启动悬浮窗
     *
     * @param callType 主叫/被叫
     */
    public void showSketchControlWindow(@SketchWindowHolder.CallType int callType) {
        this.mCallType = callType;
        if (mActivity == null) {
            return;
        }
        // 没有悬浮窗权限，展示悬浮窗权限申请
        if (!Settings.canDrawOverlays(mActivity)) {
            showWindowTipOnUiThread();
            return;
        }
        /**
         * 1、进入后台进行录制
         */
        goToBackground(mActivity);
        /**
         * 2、显示悬浮窗：
         */
        addSketchView(callType);

        /**
         * 3、注册home键监听
         */
        registerHomeKeyReceiver();
    }

    /**
     * 退出悬浮窗
     */
    public void exitSketchControlWindow() {
        // 清理全部涂鸦数据
        clearSketch();

        // 取消home键监听
        unRegisterHomeKeyReceiver();
        // 移除对应的view
        if (mWindowManager != null) {
            mWindowManager.removeView(mSketchLayout);
            mWindowManager.removeView(mCtrlLayout);
        }
        mWindowManager = null;
    }

    /**
     * 获取当前这一笔的涂鸦数据
     *
     * @return
     */
    public SketchInfoBean getCurrSketchInfo() {
        //
        if (mWindowManager == null) {
            return null;
        }
        //
        boolean isSketchShow = (mSketchView.getVisibility() == View.VISIBLE);
        if (!isSketchShow) {
            return null;
        }
        //
        SketchInfoBean sketchInfoBean = mSketchView.getCurrSketchInfo();
        return sketchInfoBean;
    }

    /**
     * 添加外部涂鸦数据
     *
     * @param sketchInfo
     */
    public void addSketchInfo(SketchInfoBean sketchInfo) {
        //
        if (mWindowManager == null) {
            return;
        }
        //
        boolean isSketchShow = (mSketchView.getVisibility() == View.VISIBLE);
        if (!isSketchShow) {
            return;
        }
        // 收到一笔对方发来的涂鸦
        mSketchView.addSketchInfo(sketchInfo);

        // 主叫：收到一笔被叫发来的涂鸦
        if (mCallType == CallType.MO) {
            // 添加一笔涂鸦
            stopSketchAnima();
            // 重置Alpha
            resetSketchAlpha();
            // 主叫 完成一笔涂鸦
            onMoSketchDone();
        }
    }


    /**
     * 接口：撤销涂鸦
     */
    public void rollBackSketchs(List<String> sketchIds) {
        //
        if (mWindowManager == null) {
            return;
        }
        if (sketchIds == null || sketchIds.size() == 0) {
            return;
        }
        boolean isSketchShow = (mSketchView.getVisibility() == View.VISIBLE);
        if (!isSketchShow) {
            return;
        }
        // 撤销
        for (int i = 0; i < sketchIds.size(); i++) {
            mSketchView.rollBackSketch(sketchIds.get(i));
        }
    }

    /**
     * 接口：清空全部涂鸦数据
     */
    public void clearSketch() {
        //
        if (mWindowManager == null) {
            return;
        }
        // 撤销
        mSketchView.clearSketch();
    }

    /**
     * 展示涂鸦蒙版
     */
    public void showSketchView() {
        if (mWindowManager == null) {
            return;
        }
        // 如果不存在，则展示涂鸦蒙版
        if (!mSketchViewVisible) {
            // 展示标记提醒
            if (mCallType == CallType.MO) {
                Toast.makeText(mActivity, R.string.sketch_tip, Toast.LENGTH_SHORT).show();
            }
            setSketchViewVisibility(true);
        }
    }


    /**
     * 显示控制按钮
     *
     * @param callType
     */
    private void addSketchView(@SketchWindowHolder.CallType int callType) {
        //设置悬浮窗布局属性
        if (mWindowManager != null) {
            LogUtil.INSTANCE.d("SketchWindowHolder","SketchWindow is showing");
            return;
        }
        mWindowManager = (WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE);

        /**
         * 涂鸦View
         */
        //设置悬浮窗的布局
        mSketchLayout = LayoutInflater.from(mActivity).inflate(R.layout.sketch_view_layout, null);
        mSketchLPs = getWindowLayoutParams();
        //设置悬浮窗的宽/高
        mSketchLPs.width = WindowManager.LayoutParams.MATCH_PARENT;
        mSketchLPs.height = WindowManager.LayoutParams.MATCH_PARENT;
        //加载显示悬浮窗
        mWindowManager.addView(mSketchLayout, mSketchLPs);
        // 涂鸦画板
        mSketchView = mSketchLayout.findViewById(R.id.sketch_view);
        mSketchView.setSketchCallback(new SketchView.SketchCallback() {
            @Override
            public void onSketchEvent(SketchView sketchView, int event) {
                if (mCallback != null) {
                    mCallback.onSketchEvent(sketchView, event, callType);
                }
                // 主叫：新的一笔涂鸦开始
                if (event == SketchView.Event.SKETCH_DOWN) {
                    stopSketchAnima();
                    resetSketchAlpha();
                }
                // 主叫：完成一笔涂鸦
                else if (event == SketchView.Event.SKETCH_UP) {
                    onMoSketchDone();
                }
            }
        });
        // 初始颜色值
        initSketchPaintColor(callType);

        /**
         * 控制器View
         */
        //设置悬浮窗的布局
        mCtrlLayout = (SketchCtrlLayout) LayoutInflater.from(mActivity).inflate(R.layout.sketch_ctrl_layout, null);
        // LayoutParams
        mCtrlLPs = getWindowLayoutParams();
        // 屏幕宽高
        int screenWidth = DisplayHelper.getWidth();
        int screenHeight = DisplayHelper.getHeight();
        int controllerHeight = mCtrlLayout.getResources().getDimensionPixelSize(R.dimen.sketch_ctrl_height);
        int controllerWidth = mCtrlLayout.getResources().getDimensionPixelSize(R.dimen.sketch_ctrl_width);
        if (mCallType == CallType.MT) {
            controllerWidth = controllerWidth / 2; // 被叫只有一个按钮，宽度减半
        }
        //设置x、y轴偏移量
        mCtrlLPs.x = (screenWidth - controllerWidth) / 2;
        mCtrlLPs.y = screenHeight - controllerHeight;
        //加载显示悬浮窗
        mWindowManager.addView(mCtrlLayout, mCtrlLPs);
        // 屏幕旋转
        mCtrlLayout.setOnScreenChangedListener(mScreenChangedListener);
        // 返回键拦截
        mCtrlLayout.setOnSketchCtrlKeyListener(mCtrlLayoutKeyListener);
        // 悬浮窗可拖动
        mCtrlLayout.setOnTouchListener(mCtrlLayoutTouchListener);
        // 退出屏幕共享
        mScreenShareExitBtn = mCtrlLayout.findViewById(R.id.exit_btn);
        mScreenShareExitBtn.setVisibility(mCallType == CallType.MO ? View.VISIBLE : View.GONE);
        mScreenShareExitBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View view) {
                LogUtil.INSTANCE.d(TAG, "mScreenShareExitBtn click");
                // 隐藏涂鸦窗口
                setSketchViewVisibility(false);
                // 显示退出提示框
                Dialog dialog = DialogTools.showTipDialog(mActivity, "提醒", "确认要退出屏幕共享吗！", "确定", "取消", new DialogTools.DialogBtnClickCallBack() {
                    @Override
                    public void onSure(Dialog dialog) {
                        // 停止屏幕共享
                        if (mCallback != null) {
                            mCallback.onExitScreenShareBtnClick(callType);
                        }
                        //
                        dialog.cancel();
                    }

                    @Override
                    public void onCancel(Dialog dialog) {
                        // 取消：没权限
                        dialog.cancel();
                    }
                });
                dialog.show();
            }
        });
        // 涂鸦开关
        mSketchSwitchBtn = mCtrlLayout.findViewById(R.id.sketch_drawing_btn);
        mSketchSwitchBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View view) {
                // 准备：屏幕涂鸦
                // 显示 || 隐藏
                boolean visible = (mSketchLayout.getVisibility() != View.VISIBLE);
                setSketchViewVisibility(visible);
            }
        });
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
                Dialog dialog = DialogTools.showTipDialog(mActivity, "权限申请", "应用需申请悬浮窗权限，以展示屏幕共享控制按钮！", "确定", "取消", new DialogTools.DialogBtnClickCallBack() {
                    @Override
                    public void onSure(Dialog dialog) {
                        // 进入设置页面
                        String ACTION_MANAGE_OVERLAY_PERMISSION = "android.settings.action.MANAGE_OVERLAY_PERMISSION";
                        Intent intent = new Intent(ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + mActivity.getPackageName()));
                        mActivity.startActivityForResult(intent, SketchWindowHolder.ALART_WINDOW_PERMISSION_CODE);
                        //
                        dialog.cancel();
                    }

                    @Override
                    public void onCancel(Dialog dialog) {
                        // 取消：没权限
                        dialog.cancel();
                    }
                });
                dialog.show();
                return true;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }


    /**
     * 显示或隐藏SketchView
     */
    private void setSketchViewVisibility(boolean visible) {
        //设置悬浮窗的宽度
        if (mWindowManager == null) {
            return;
        }
        mSketchViewVisible = visible;
        // 隐藏前 清理涂鸦数据
        if (!visible) {
            clearSketch();
        }
        // 是否选中状态
        if (visible) {
            mSketchSwitchBtn.setSelected(true); // 选中
        } else {
            mSketchSwitchBtn.setSelected(false); // 未选中
        }
        // 更新UI：sketch 展示状态
        mSketchLayout.setVisibility(visible ? View.VISIBLE : View.GONE);
        // 更新是否拦截Key事件
        updateCtrlLayoutFlag(visible);
    }

    /**
     * 接口：上一笔涂鸦绘制
     */
    private void rollBackPreSketch() {
        //
        if (mWindowManager == null) {
            return;
        }
        //
        boolean isSketchShow = (mSketchView.getVisibility() == View.VISIBLE);
        if (!isSketchShow) {
            return;
        }
        // 撤销
        mSketchView.rollBackPreSketch();
    }


    /**
     * 进入后台
     *
     * @param activity
     */
    private void goToBackground(Context activity) {
//        if (activity != null) {
//            Intent home = new Intent(Intent.ACTION_MAIN);
//            home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//            home.addCategory(Intent.CATEGORY_HOME);
//            activity.startActivity(home);
//        }
    }


    public static abstract class SketchWindowListener {
        // 退出屏幕共享按钮的点击事件
        public void onExitScreenShareBtnClick(@SketchWindowHolder.CallType int callType) {
        }

        // 涂鸦事件回调
        public void onSketchEvent(SketchView sketchView, @SketchView.Event int event, @SketchWindowHolder.CallType int callType) {
        }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~悬浮窗位置 拖拽~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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
        lps.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lps.height = WindowManager.LayoutParams.WRAP_CONTENT;
        // 屏幕左上角为起始点
        lps.gravity = Gravity.LEFT | Gravity.TOP;
        //设置x、y轴偏移量
        lps.x = 0;
        lps.y = 0;
        return lps;
    }

    /**
     * 悬浮窗位置
     */
    View.OnTouchListener mCtrlLayoutTouchListener = new View.OnTouchListener() {
        // 记录上次移动的位置
        private float mPreTouchX = 0;
        private float mPreTouchY = 0;
        // 是否是移动事件
        boolean isMoved = false;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // 当前点坐标
            float x = event.getRawX();
            float y = event.getRawY();
            //
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isMoved = false;
                    // 记录按下位置
                    mPreTouchX = x;
                    mPreTouchY = y;
                    break;
                case MotionEvent.ACTION_MOVE:
                    // 移动的距离
                    float dx = x - mPreTouchX;
                    float dy = y - mPreTouchY;
                    // 更新位置
                    updateCtrlLayoutLocation(dx, dy);
                    // 当前点操作点
                    mPreTouchX = x;
                    mPreTouchY = y;
                    //
                    isMoved = true;
                case MotionEvent.ACTION_CANCEL:
                    isMoved = true;
                    break;
            }
            return isMoved;
        }
    };

    SketchCtrlLayout.OnScreenChangedListener mScreenChangedListener = new SketchCtrlLayout.OnScreenChangedListener() {
        @Override
        public void onScreenChanged(Configuration newConfig) {
            // 重置窗口显示位置
            resetCtrlLayoutLocation(newConfig);
        }
    };

    SketchCtrlLayout.OnSketchCtrlKeyListener mCtrlLayoutKeyListener = new SketchCtrlLayout.OnSketchCtrlKeyListener() {
        @Override
        public void onKeyEvent(View v, int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                // 隐藏涂鸦View
                setSketchViewVisibility(false);
            }
        }
    };

    /**
     * 更新控制按钮Layout位置
     *
     * @param dx
     * @param dy
     */
    private void updateCtrlLayoutLocation(float dx, float dy) {
        if (mCtrlLayout != null && mWindowManager != null) {
            WindowManager.LayoutParams param = (WindowManager.LayoutParams) mCtrlLayout.getLayoutParams();
            param.x += dx;
            param.y += dy;
            mWindowManager.updateViewLayout(mCtrlLayout, param);
        }
    }

    /**
     * 重置控制按钮位置
     *
     * @param newConfig
     */
    private void resetCtrlLayoutLocation(Configuration newConfig) {
        // 屏幕宽高
        int screenWidth = 0;
        int screenHeight = 0;
        int controllerHeight = mCtrlLayout.getResources().getDimensionPixelSize(R.dimen.sketch_ctrl_height);
        int controllerWidth = mCtrlLayout.getResources().getDimensionPixelSize(R.dimen.sketch_ctrl_width);
        if (mCallType == CallType.MT) {
            controllerWidth = controllerWidth / 2; // 被叫只有一个按钮，宽度减半
        }
        //
        switch (newConfig.orientation) {
            // 竖屏
            case Configuration.ORIENTATION_PORTRAIT:
                if (DisplayHelper.getHeight() > DisplayHelper.getWidth()) {
                    screenWidth = DisplayHelper.getWidth();
                    screenHeight = DisplayHelper.getHeight();
                } else {
                    screenWidth = DisplayHelper.getHeight();
                    screenHeight = DisplayHelper.getWidth();
                }
                break;
            // 横屏
            case Configuration.ORIENTATION_LANDSCAPE:
                if (DisplayHelper.getWidth() > DisplayHelper.getHeight()) {
                    screenWidth = DisplayHelper.getWidth();
                    screenHeight = DisplayHelper.getHeight();
                } else {
                    screenWidth = DisplayHelper.getHeight();
                    screenHeight = DisplayHelper.getWidth();
                }
                break;
        }
        // 重置显示位置
        if (mWindowManager != null && mCtrlLayout != null) {
            WindowManager.LayoutParams param = (WindowManager.LayoutParams) mCtrlLayout.getLayoutParams();
            //设置x、y轴偏移量
            mCtrlLPs.x = (screenWidth - controllerWidth) / 2;
            mCtrlLPs.y = screenHeight - controllerHeight;
            mWindowManager.updateViewLayout(mCtrlLayout, param);
        }
    }

    /**
     * @param showSketchView
     */
    private void updateCtrlLayoutFlag(boolean showSketchView) {
        if (mWindowManager == null || mCtrlLayout == null) {
            return;
        }
        WindowManager.LayoutParams param = (WindowManager.LayoutParams) mCtrlLayout.getLayoutParams();
        if (showSketchView) {
            // 拦截key
            param.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        } else {
            // 不拦截key
            param.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        }
        mWindowManager.updateViewLayout(mCtrlLayout, param);
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~home键与menu键监听~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * 监听home键广播
     */
    private BroadcastReceiver homeListenerReceiver = new BroadcastReceiver() {
        final String SYSTEM_DIALOG_REASON_KEY = "reason";
        final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
        final String SYSTEM_DIALOG_REASON_RECENT_KEY = "recentapps";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
                if (!TextUtils.isEmpty(reason)) {
                    if (reason.equals(SYSTEM_DIALOG_REASON_HOME_KEY)
                            || reason.equals(SYSTEM_DIALOG_REASON_RECENT_KEY)) {
                        // 隐藏涂鸦View
                        setSketchViewVisibility(false);
                    }
                }
            }
        }
    };

    /**
     * 注册home键监听
     */
    private void registerHomeKeyReceiver() {
        if (mActivity == null) {
            return;
        }
        IntentFilter homeFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        mActivity.registerReceiver(homeListenerReceiver, homeFilter);
    }

    /**
     * 取消home键监听
     */
    private void unRegisterHomeKeyReceiver() {
        if (mActivity == null) {
            return;
        }
        try {
            mActivity.unregisterReceiver(homeListenerReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~消失动画~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


    private void onMoSketchDone() {
        if (mSketchView == null || mSketchView.getVisibility() != View.VISIBLE) {
            return;
        }
        // 主叫&View展示
        if (mCallType == CallType.MO) {
            startSketchAlphaAnima(new Animator.AnimatorListener() {
                boolean cancel = false;

                @Override
                public void onAnimationStart(Animator animation) {
                    cancel = false;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    cancel = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    // 清理全部涂鸦数据
                    if (!cancel) {
                        clearSketch();
                    }
                    cancel = false;
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
        }
    }

    /**
     * 开启Alpha 动画
     */
    private void startSketchAlphaAnima(Animator.AnimatorListener listener) {
        if (mSketchView == null) {
            return;
        }
        //
        mSketchAnimator = ObjectAnimator.ofFloat(mSketchView, "alpha", 1f, 0f);
        mSketchAnimator.setDuration(2000);
        mSketchAnimator.setStartDelay(3000);
        mSketchAnimator.setInterpolator(new DecelerateInterpolator());
        if (listener != null) {
            mSketchAnimator.addListener(listener);
        }
        mSketchAnimator.start();
    }

    /**
     * 清理目标View的动画
     */
    private void stopSketchAnima() {
        if (mSketchAnimator != null) {
            mSketchAnimator.cancel();
            mSketchAnimator = null;
        }
    }

    /**
     * 重置View动画
     */
    private void resetSketchAlpha() {
        if (mSketchView != null) {
            mSketchView.setAlpha(1.0f);
        }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~初始颜色值~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private void initSketchPaintColor(@SketchWindowHolder.CallType int callType) {
        // 主叫红色
        if (callType == CallType.MO) {
            mSketchView.setPaintColor(getHoloRedColor());
        } else {
            // 被叫蓝色
            mSketchView.setPaintColor(getBlueDarkColor());
        }
    }

    @ColorInt
    private int getHoloRedColor() {
        if (mActivity != null) {
            return mActivity.getResources().getColor(android.R.color.holo_red_light);
        }
        return Color.parseColor("#ffff4444");
    }

    @ColorInt
    private int getBlueDarkColor() {
        if (mActivity != null) {
            return mActivity.getResources().getColor(android.R.color.holo_blue_dark);
        }
        return Color.parseColor("#ff0099cc");
    }

}
