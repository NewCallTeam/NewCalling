/*
 * Copyright (c) 2023 China Mobile Communications Group Co.,Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cmcc.newcalllib;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.cmcc.newcalllib.adapter.ntv.NativeAbilityProviderImpl;
import com.cmcc.newcalllib.tool.LogUtil;
import com.luck.picture.lib.PictureSelectorFragment;
import com.luck.picture.lib.app.PictureAppMaster;
import com.luck.picture.lib.basic.IBridgePictureBehavior;
import com.luck.picture.lib.basic.PictureCommonFragment;
import com.luck.picture.lib.basic.PictureContextWrapper;
import com.luck.picture.lib.basic.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.config.PictureSelectionConfig;
import com.luck.picture.lib.config.SelectMimeType;
import com.luck.picture.lib.dialog.RemindDialog;

import com.luck.picture.lib.engine.UriToFileTransformEngine;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.entity.MediaExtraInfo;
import com.luck.picture.lib.immersive.ImmersiveManager;
import com.luck.picture.lib.interfaces.OnKeyValueResultCallbackListener;
import com.luck.picture.lib.permissions.PermissionConfig;
import com.luck.picture.lib.style.BottomNavBarStyle;
import com.luck.picture.lib.style.PictureSelectorStyle;
import com.luck.picture.lib.style.SelectMainStyle;
import com.luck.picture.lib.style.TitleBarStyle;
import com.luck.picture.lib.utils.DensityUtil;
import com.luck.picture.lib.utils.MediaUtils;
import com.luck.picture.lib.utils.SandboxTransformUtils;
import com.luck.picture.lib.widget.MediumBoldTextView;
import com.luck.lib.camerax.CameraImageEngine;
import com.luck.lib.camerax.SimpleCameraX;
import com.luck.lib.camerax.listener.OnSimpleXPermissionDeniedListener;
import com.luck.lib.camerax.listener.OnSimpleXPermissionDescriptionListener;
import com.luck.lib.camerax.permissions.SimpleXPermissionUtil;

import java.util.ArrayList;

public class InjectFragmentActivity extends AppCompatActivity implements IBridgePictureBehavior {

    private final static String TAG = "InjectFragmentActivity";
    private final static String TAG_EXPLAIN_VIEW = "TAG_EXPLAIN_VIEW";

    private TextView mGallery;
    private View mGalleryLine;
    private TextView mCamera;
    private View mCameraLine;

    private Intent mIntent;
    private PictureSelectorStyle selectorStyle;

    private PictureSelectorFragment mSelectorFragment;
    private PictureCameraFragment mCameraFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int color = ContextCompat.getColor(this, R.color.ps_color_white);
        ImmersiveManager.immersiveAboveAPI23(this, color, color, true);
        setContentView(R.layout.activity_inject_fragment);
        initView();

        mIntent = getIntent();
        final boolean isCaptureEnabled = mIntent.getBooleanExtra(NativeAbilityProviderImpl.REQ_INTENT_EXTRA_CAPTURE, false);
        selectorStyle = new PictureSelectorStyle();
        setStyle();

        if (isCaptureEnabled) {
            pitchOnCamera();
            addCameraFragment();
        } else {
            pitchOnGallery();
            addSelectorFragment();
        }
    }

    private void initView() {
        final View statusBar = findViewById(R.id.status_bar);
        ViewGroup.LayoutParams layoutParams = statusBar.getLayoutParams();
        layoutParams.height = DensityUtil.getStatusBarHeight(getContext());

        findViewById(R.id.exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSelectorFragment != null) {
                    mSelectorFragment.onKeyBackFragmentFinish();
                }
                finish();
            }
        });

        mGallery = findViewById(R.id.gallery);
        mGalleryLine = findViewById(R.id.gallery_line);
        mCamera = findViewById(R.id.camera);
        mCameraLine = findViewById(R.id.camera_line);

        mGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pitchOnGallery();
                addSelectorFragment();
            }
        });
        mCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pitchOnCamera();
                addCameraFragment();
            }
        });
    }

    private void pitchOnGallery() {
        if (mGalleryLine.getVisibility() == View.VISIBLE) {
            return;
        }
        mGallery.setTextColor(ContextCompat.getColor(getContext(), R.color.ps_color_white));
        mGalleryLine.setVisibility(View.VISIBLE);
        mCamera.setTextColor(ContextCompat.getColor(getContext(), R.color.ps_color_half_grey));
        mCameraLine.setVisibility(View.INVISIBLE);
    }

    private void addSelectorFragment() {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        final Fragment selectorFragment = fragmentManager.findFragmentByTag(PictureSelectorFragment.TAG);
        final Fragment cameraFragment = fragmentManager.findFragmentByTag(PictureCameraFragment.TAG);
        final FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (selectorFragment != null) {
            if (cameraFragment != null) {
                transaction.hide(cameraFragment);
            }
            transaction.show(selectorFragment);
            transaction.commit();
            return;
        }
        mSelectorFragment = PictureSelector.create(this)
                .openGallery(mIntent.getIntExtra(NativeAbilityProviderImpl.REQ_INTENT_EXTRA_NAME, SelectMimeType.ofAll()))
                .setSelectorUIStyle(selectorStyle)
                .setImageEngine(NewCallGlide.createGlideEngine())
                .isOriginalControl(true) // 是否开启原图
                .setSandboxFileEngine(new MeSandboxFileEngine())
                .isDisplayCamera(false)
                .setMaxSelectNum(1)
                .build();
        fragmentManager.beginTransaction()
                .add(R.id.fragment_content, mSelectorFragment, mSelectorFragment.getFragmentTag())
                .commitAllowingStateLoss();
    }

    private void pitchOnCamera() {
        if (mCameraLine.getVisibility() == View.VISIBLE) {
            return;
        }
        mGallery.setTextColor(ContextCompat.getColor(getContext(), R.color.ps_color_half_grey));
        mGalleryLine.setVisibility(View.INVISIBLE);
        mCamera.setTextColor(ContextCompat.getColor(getContext(), R.color.ps_color_white));
        mCameraLine.setVisibility(View.VISIBLE);
    }

    private void addCameraFragment() {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        final Fragment cameraFragment = fragmentManager.findFragmentByTag(PictureCameraFragment.TAG);
        final Fragment selectorFragment = fragmentManager.findFragmentByTag(PictureSelectorFragment.TAG);
        final FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (cameraFragment != null) {
            if (selectorFragment != null) {
                transaction.hide(selectorFragment);
            }
            transaction.show(cameraFragment);
            transaction.commit();
            return;
        }
        SimpleCameraX camera = SimpleCameraX.of();
        camera.isAutoRotation(true);
        camera.setCameraMode(mIntent.getIntExtra(NativeAbilityProviderImpl.REQ_INTENT_EXTRA_NAME, SelectMimeType.ofAll()));
        camera.setVideoFrameRate(25);
        camera.setVideoBitRate(3 * 1024 * 1024);
        camera.isDisplayRecordChangeTime(true);
        camera.isManualFocusCameraPreview(true);
        camera.isZoomCameraPreview(true);
        camera.setOutputPathDir(getSandboxCameraOutputPath());
        camera.setPermissionDeniedListener(getSimpleXPermissionDeniedListener());
        camera.setPermissionDescriptionListener(getSimpleXPermissionDescriptionListener());
        camera.setImageEngine(new CameraImageEngine() {
            @Override
            public void loadImage(Context context, String url, ImageView imageView) {
                Glide.with(context).load(url).into(imageView);
            }
        });
        final Intent intent = camera.getIntent(InjectFragmentActivity.this);
        mCameraFragment = PictureCameraFragment.newInstance(intent);
        fragmentManager.beginTransaction()
                .add(R.id.fragment_content, mCameraFragment, mCameraFragment.getFragmentTag())
                .commitAllowingStateLoss();
        mCameraFragment.setFragmentListener(new CameraFragmentListener() {
            @Override
            public void handleCameraSuccess() {
                pitchOnGallery();
                addSelectorFragment();
                mSelectorFragment.onActivityResult(PictureConfig.REQUEST_CAMERA, Activity.RESULT_OK, getIntent());
            }

            @Override
            public void handleCameraCancel() {
                LogUtil.INSTANCE.d(TAG, "handleCameraCancel");
                pitchOnGallery();
                addSelectorFragment();
            }
        });
    }

    private void setStyle() {
        // 主体风格
        SelectMainStyle numberSelectMainStyle = new SelectMainStyle();
        numberSelectMainStyle.setSelectNumberStyle(true);
        numberSelectMainStyle.setPreviewSelectNumberStyle(false);
        numberSelectMainStyle.setPreviewDisplaySelectGallery(true);
        numberSelectMainStyle.setSelectBackground(R.drawable.ps_default_num_selector);
        numberSelectMainStyle.setAdapterSelectStyleGravity(new int[]{RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.ALIGN_PARENT_END});
        numberSelectMainStyle.setPreviewSelectBackground(R.drawable.ps_preview_checkbox_selector);
        numberSelectMainStyle.setSelectNormalBackgroundResources(R.drawable.ps_select_complete_normal_bg);
        numberSelectMainStyle.setSelectNormalTextColor(ContextCompat.getColor(getContext(), R.color.ps_color_9b));
        numberSelectMainStyle.setSelectNormalText(getString(R.string.ps_send));
        numberSelectMainStyle.setAdapterPreviewGalleryBackgroundResource(R.drawable.ps_preview_gallery_bg);
        numberSelectMainStyle.setAdapterPreviewGalleryItemSize(DensityUtil.dip2px(getContext(), 52));
        numberSelectMainStyle.setPreviewSelectText(getString(R.string.ps_select));
        numberSelectMainStyle.setPreviewSelectTextSize(14);
        numberSelectMainStyle.setPreviewSelectTextColor(ContextCompat.getColor(getContext(), R.color.ps_color_white));
        numberSelectMainStyle.setPreviewSelectMarginRight(DensityUtil.dip2px(getContext(), 6));
        numberSelectMainStyle.setSelectBackgroundResources(R.drawable.ps_select_complete_bg);
        numberSelectMainStyle.setSelectText(getString(R.string.ps_send_num));
        numberSelectMainStyle.setSelectTextColor(ContextCompat.getColor(getContext(), R.color.ps_color_white));
        numberSelectMainStyle.setMainListBackgroundColor(ContextCompat.getColor(getContext(), R.color.ps_color_black));
        numberSelectMainStyle.setCompleteSelectRelativeTop(false);
        numberSelectMainStyle.setPreviewSelectRelativeBottom(true);
        numberSelectMainStyle.setAdapterItemIncludeEdge(false);

        // 头部TitleBar 风格
        TitleBarStyle numberTitleBarStyle = new TitleBarStyle();
        numberTitleBarStyle.setHideTitleBar(true);

        // 底部NavBar 风格
        BottomNavBarStyle numberBottomNavBarStyle = new BottomNavBarStyle();
        numberBottomNavBarStyle.setBottomNarBarBackgroundColor(ContextCompat.getColor(getContext(), R.color.ps_color_black));
        numberBottomNavBarStyle.setBottomPreviewNarBarBackgroundColor(ContextCompat.getColor(getContext(), R.color.ps_color_half_grey));
        numberBottomNavBarStyle.setBottomPreviewNormalText(getString(R.string.ps_preview));
        numberBottomNavBarStyle.setBottomPreviewNormalTextColor(ContextCompat.getColor(getContext(), R.color.ps_color_9b));
        numberBottomNavBarStyle.setBottomPreviewNormalTextSize(16);
        numberBottomNavBarStyle.setCompleteCountTips(false);
        numberBottomNavBarStyle.setBottomPreviewSelectText(getString(R.string.ps_preview));
        numberBottomNavBarStyle.setBottomPreviewSelectTextColor(ContextCompat.getColor(getContext(), R.color.ps_color_white));

        selectorStyle.setTitleBarStyle(numberTitleBarStyle);
        selectorStyle.setBottomBarStyle(numberBottomNavBarStyle);
        selectorStyle.setSelectMainStyle(numberSelectMainStyle);
    }

    @Override
    public void onSelectFinish(PictureCommonFragment.SelectorResult result) {
        setTranslucentStatusBar();
        if (result == null) {
            return;
        }
        if (result.mResultCode == RESULT_OK) {
            ArrayList<LocalMedia> selectorResult = PictureSelector.obtainSelectorList(result.mResultData);
            analyticalSelectResults(selectorResult);
            mIntent.putParcelableArrayListExtra(NativeAbilityProviderImpl.RES_INTENT_EXTRA_NAME, selectorResult);
            setResult(RESULT_OK, mIntent);
            finish();
        } else if (result.mResultCode == RESULT_CANCELED) {
            LogUtil.INSTANCE.i(TAG, "onSelectFinish PictureSelector Cancel");
            setTranslucentStatusBar();
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    /**
     * 处理选择结果
     *
     * @param result
     */
    private void analyticalSelectResults(ArrayList<LocalMedia> result) {
        StringBuilder builder = new StringBuilder();
        builder.append("Result").append("\n");
        for (LocalMedia media : result) {
            if (media.getWidth() == 0 || media.getHeight() == 0) {
                if (PictureMimeType.isHasImage(media.getMimeType())) {
                    MediaExtraInfo imageExtraInfo = MediaUtils.getImageSize(this, media.getPath());
                    media.setWidth(imageExtraInfo.getWidth());
                    media.setHeight(imageExtraInfo.getHeight());
                } else if (PictureMimeType.isHasVideo(media.getMimeType())) {
                    MediaExtraInfo videoExtraInfo = MediaUtils.getVideoSize(PictureAppMaster.getInstance().getAppContext(), media.getPath());
                    media.setWidth(videoExtraInfo.getWidth());
                    media.setHeight(videoExtraInfo.getHeight());
                }
            }
            builder.append(media.getAvailablePath()).append("\n");
            Log.i(TAG, "文件名: " + media.getFileName());
            Log.i(TAG, "是否压缩:" + media.isCompressed());
            Log.i(TAG, "压缩:" + media.getCompressPath());
            Log.i(TAG, "原图:" + media.getPath());
            Log.i(TAG, "绝对路径:" + media.getRealPath());
            Log.i(TAG, "是否裁剪:" + media.isCut());
            Log.i(TAG, "裁剪:" + media.getCutPath());
            Log.i(TAG, "是否开启原图:" + media.isOriginal());
            Log.i(TAG, "原图路径:" + media.getOriginalPath());
            Log.i(TAG, "沙盒路径:" + media.getSandboxPath());
            Log.i(TAG, "原始宽高: " + media.getWidth() + "x" + media.getHeight());
            Log.i(TAG, "裁剪宽高: " + media.getCropImageWidth() + "x" + media.getCropImageHeight());
            Log.i(TAG, "文件大小: " + media.getSize());
        }
    }

    /**
     * 设置状态栏字体颜色
     */
    private void setTranslucentStatusBar() {
        ImmersiveManager.translucentStatusBar(InjectFragmentActivity.this, true);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(PictureContextWrapper.wrap(newBase,
                PictureSelectionConfig.getInstance().language,PictureSelectionConfig.getInstance().defaultLanguage));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LogUtil.INSTANCE.d(TAG, "onActivityResult: requestCode = " + requestCode + ", resultCode = " + resultCode + ", data = " + data);
        /*if (CustomCameraConfig.explainListener != null) {
            CustomCameraConfig.explainListener.onDismiss(mCameraView);
        }
        if (requestCode == PermissionChecker.PERMISSION_SETTING_CODE) {
            if (PermissionChecker.checkSelfPermission(this, new String[]{Manifest.permission.CAMERA})) {
                mCameraView.buildUseCameraCases();
            } else {
                SimpleXSpUtils.putBoolean(this, Manifest.permission.CAMERA, true);
                handleCameraCancel();
            }
        } else if (requestCode == PermissionChecker.PERMISSION_RECORD_AUDIO_SETTING_CODE) {
            if (!PermissionChecker.checkSelfPermission(this, new String[]{Manifest.permission.RECORD_AUDIO})) {
                SimpleXSpUtils.putBoolean(this, Manifest.permission.RECORD_AUDIO, true);
                Toast.makeText(InjectFragmentActivity.this.getApplicationContext(), "Missing recording permission", Toast.LENGTH_SHORT).show();
            }
        }*/
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        LogUtil.INSTANCE.d(TAG, "onRequestPermissionsResult");
        if (mCameraFragment != null) {
            mCameraFragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * 自定义沙盒文件处理
     */
    private static class MeSandboxFileEngine implements UriToFileTransformEngine {

        @Override
        public void onUriToFileAsyncTransform(Context context, String srcPath, String mineType, OnKeyValueResultCallbackListener call) {
            if (call != null) {
                call.onCallback(srcPath, SandboxTransformUtils.copyPathToSandbox(context, srcPath, mineType));
            }
        }
    }

    /**
     * 创建相机自定义输出目录
     *
     * @return
     */
    private String getSandboxCameraOutputPath() {
        /*if (cb_custom_sandbox.isChecked()) {
            File externalFilesDir = getContext().getExternalFilesDir("");
            File customFile = new File(externalFilesDir.getAbsolutePath(), "Sandbox");
            if (!customFile.exists()) {
                customFile.mkdirs();
            }
            return customFile.getAbsolutePath() + File.separator;
        } else {
            return "";
        }*/
        return "";
    }

    /**
     * SimpleCameraX权限拒绝后回调
     *
     * @return
     */
    private OnSimpleXPermissionDeniedListener getSimpleXPermissionDeniedListener() {
        return new MeOnSimpleXPermissionDeniedListener();
    }

    /**
     * SimpleCameraX添加权限说明
     */
    private static class MeOnSimpleXPermissionDeniedListener implements OnSimpleXPermissionDeniedListener {

        @Override
        public void onDenied(Context context, String permission, int requestCode) {
            String tips;
            if (TextUtils.equals(permission, Manifest.permission.RECORD_AUDIO)) {
                tips = "缺少麦克风权限\n可能会导致录视频无法采集声音";
            } else {
                tips = "缺少相机权限\n可能会导致不能使用摄像头功能";
            }
            RemindDialog dialog = RemindDialog.buildDialog(context, tips);
            dialog.setButtonText("去设置");
            dialog.setButtonTextColor(0xFF7D7DFF);
            dialog.setContentTextColor(0xFF333333);
            dialog.setOnDialogClickListener(new RemindDialog.OnDialogClickListener() {
                @Override
                public void onClick(View view) {
                    SimpleXPermissionUtil.goIntentSetting((Activity) context, requestCode);
                    dialog.dismiss();
                }
            });
            dialog.show();
        }
    }

    /**
     * SimpleCameraX权限说明
     *
     * @return
     */
    private OnSimpleXPermissionDescriptionListener getSimpleXPermissionDescriptionListener() {
        return new MeOnSimpleXPermissionDescriptionListener();
    }

    /**
     * SimpleCameraX添加权限说明
     */
    private static class MeOnSimpleXPermissionDescriptionListener implements OnSimpleXPermissionDescriptionListener {

        @Override
        public void onPermissionDescription(Context context, ViewGroup viewGroup, String permission) {
            addPermissionDescription(true, viewGroup, new String[]{permission});
        }

        @Override
        public void onDismiss(ViewGroup viewGroup) {
            removePermissionDescription(viewGroup);
        }
    }

    /**
     * 添加权限说明
     *
     * @param viewGroup
     * @param permissionArray
     */
    private static void addPermissionDescription(boolean isHasSimpleXCamera, ViewGroup viewGroup, String[] permissionArray) {
        int dp10 = DensityUtil.dip2px(viewGroup.getContext(), 10);
        int dp15 = DensityUtil.dip2px(viewGroup.getContext(), 15);
        MediumBoldTextView view = new MediumBoldTextView(viewGroup.getContext());
        view.setTag(TAG_EXPLAIN_VIEW);
        view.setTextSize(14);
        view.setTextColor(Color.parseColor("#333333"));
        view.setPadding(dp10, dp15, dp10, dp15);

        String title;
        String explain;

        if (TextUtils.equals(permissionArray[0], PermissionConfig.CAMERA[0])) {
            title = "相机权限使用说明";
            explain = "相机权限使用说明\n用户app用于拍照/录视频";
        } else if (TextUtils.equals(permissionArray[0], Manifest.permission.RECORD_AUDIO)) {
            if (isHasSimpleXCamera) {
                title = "麦克风权限使用说明";
                explain = "麦克风权限使用说明\n用户app用于录视频时采集声音";
            } else {
                title = "录音权限使用说明";
                explain = "录音权限使用说明\n用户app用于采集声音";
            }
        } else {
            title = "存储权限使用说明";
            explain = "存储权限使用说明\n用户app写入/下载/保存/读取/修改/删除图片、视频、文件等信息";
        }
        int startIndex = 0;
        int endOf = startIndex + title.length();
        SpannableStringBuilder builder = new SpannableStringBuilder(explain);
        builder.setSpan(new AbsoluteSizeSpan(DensityUtil.dip2px(viewGroup.getContext(), 16)), startIndex, endOf, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        builder.setSpan(new ForegroundColorSpan(0xFF333333), startIndex, endOf, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        view.setText(builder);
        view.setBackground(ContextCompat.getDrawable(viewGroup.getContext(), R.drawable.picture_ic_camera));

        if (isHasSimpleXCamera) {
            RelativeLayout.LayoutParams layoutParams =
                    new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.topMargin = DensityUtil.getStatusBarHeight(viewGroup.getContext());
            layoutParams.leftMargin = dp10;
            layoutParams.rightMargin = dp10;
            viewGroup.addView(view, layoutParams);
        } else {
            ConstraintLayout.LayoutParams layoutParams =
                    new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.topToBottom = R.id.title_bar;
            layoutParams.leftToLeft = ConstraintSet.PARENT_ID;
            layoutParams.leftMargin = dp10;
            layoutParams.rightMargin = dp10;
            viewGroup.addView(view, layoutParams);
        }
    }

    /**
     * 移除权限说明
     *
     * @param viewGroup
     */
    private static void removePermissionDescription(ViewGroup viewGroup) {
        View tagExplainView = viewGroup.findViewWithTag(TAG_EXPLAIN_VIEW);
        viewGroup.removeView(tagExplainView);
    }

    public Context getContext() {
        return this;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        LogUtil.INSTANCE.d(TAG, "onKeyDown: keyCode = " + keyCode);
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mCameraFragment != null) {
                final PictureCameraFragment fragment = (PictureCameraFragment) getSupportFragmentManager().findFragmentByTag(mCameraFragment.getFragmentTag());
                if (fragment != null && !fragment.isHidden()) {
                    fragment.onKeyDowm();
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}