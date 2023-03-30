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
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.luck.lib.camerax.CustomCameraConfig;
import com.luck.lib.camerax.CustomCameraView;
import com.luck.lib.camerax.listener.CameraListener;
import com.luck.lib.camerax.listener.ClickListener;
import com.luck.lib.camerax.listener.IObtainCameraView;
import com.luck.lib.camerax.listener.ImageCallbackListener;
import com.luck.lib.camerax.permissions.PermissionChecker;
import com.luck.lib.camerax.permissions.PermissionResultCallback;
import com.luck.lib.camerax.utils.SimpleXSpUtils;

public class PictureCameraFragment extends Fragment implements IObtainCameraView {

    public static final String TAG = PictureCameraFragment.class.getSimpleName();

    private Intent mIntent;
    private CustomCameraView mCameraView;

    /**
     * PermissionResultCallback
     */
    private PermissionResultCallback mPermissionResultCallback;
    private FragmentListener mFragmentListener;

    public PictureCameraFragment() {
        // Required empty public constructor
    }

    public static PictureCameraFragment newInstance(Intent intent) {
        PictureCameraFragment fragment = new PictureCameraFragment();
        fragment.setIntent(intent);
        return fragment;
    }

    private void setIntent(Intent intent) {
        mIntent = intent;
    }

    public String getFragmentTag() {
        return TAG;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "PictureCameraFragment onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "PictureCameraFragment onCreateView");
        mCameraView = new CustomCameraView(getContext());
        final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        mCameraView.setLayoutParams(layoutParams);
        initView();
        return mCameraView;
    }

    private void initView() {
        mCameraView.post(new Runnable() {
            @Override
            public void run() {
                mCameraView.setCameraConfig(mIntent);
            }
        });
        mCameraView.setImageCallbackListener(new ImageCallbackListener() {
            @Override
            public void onLoadImage(String url, ImageView imageView) {
                if (CustomCameraConfig.imageEngine != null) {
                    CustomCameraConfig.imageEngine.loadImage(imageView.getContext(), url, imageView);
                }
            }
        });
        mCameraView.setCameraListener(new CameraListener() {
            @Override
            public void onPictureSuccess(@NonNull String url) {
                handleCameraSuccess();
            }

            @Override
            public void onRecordSuccess(@NonNull String url) {
                handleCameraSuccess();
            }

            @Override
            public void onError(int videoCaptureError, String message, Throwable cause) {
                Toast.makeText(getContext().getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
        mCameraView.setOnCancelClickListener(new ClickListener() {
            @Override
            public void onClick() {
                handleCameraCancel();
            }
        });

//        PermissionChecker.getInstance().setPermissionCallback(new OnPermissionCallback() {
//            @Override
//            public void onCallback(PermissionResultCallback callback) {
//                mPermissionResultCallback = callback;
//            }
//        });
    }

    private void handleCameraSuccess() {
        Log.d(TAG, "PictureCameraFragment handleCameraSuccess");
        final Uri uri = getActivity().getIntent().getParcelableExtra(MediaStore.EXTRA_OUTPUT);
        Log.d(TAG, "PictureCameraFragment handleCameraSuccess uri = " + uri);
        if (mFragmentListener != null) {
            mFragmentListener.handleCameraSuccess();
        }
    }

    private void handleCameraCancel() {
        Log.d(TAG, "PictureCameraFragment handleCameraCancel");
        if (mFragmentListener != null) {
            mFragmentListener.handleCameraCancel();
        }
    }

    public void setFragmentListener(FragmentListener fragmentListener) {
        mFragmentListener = fragmentListener;
    }

    public void onKeyDowm() {
        Log.d(TAG, "PictureCameraFragment onKeyDowm");
        mCameraView.onCancelMedia();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "PictureCameraFragment onConfigurationChanged");
        mCameraView.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "PictureCameraFragment onActivityResult: requestCode = " + requestCode + ", resultCode = " + resultCode + ", data = " + data);
        if (CustomCameraConfig.explainListener != null) {
            CustomCameraConfig.explainListener.onDismiss(mCameraView);
        }
        if (requestCode == PermissionChecker.PERMISSION_SETTING_CODE) {
            if (PermissionChecker.checkSelfPermission(getContext(), new String[]{Manifest.permission.CAMERA})) {
                mCameraView.buildUseCameraCases();
            } else {
                SimpleXSpUtils.putBoolean(getContext(), Manifest.permission.CAMERA, true);
                handleCameraCancel();
            }
        } else if (requestCode == PermissionChecker.PERMISSION_RECORD_AUDIO_SETTING_CODE) {
            if (!PermissionChecker.checkSelfPermission(getContext(), new String[]{Manifest.permission.RECORD_AUDIO})) {
                SimpleXSpUtils.putBoolean(getContext(), Manifest.permission.RECORD_AUDIO, true);
                Toast.makeText(getContext().getApplicationContext(), "Missing recording permission", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /*public void setPermissionsResultAction(PermissionResultCallback callback) {
        mPermissionResultCallback = callback;
    }*/

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "PictureCameraFragment onRequestPermissionsResult");
        if (mPermissionResultCallback != null) {
            PermissionChecker.getInstance().onRequestPermissionsResult(grantResults, mPermissionResultCallback);
            mPermissionResultCallback = null;
        }
    }

    @Override
    public ViewGroup getCustomCameraView() {
        return mCameraView;
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "PictureCameraFragment onDestroyView");
        mCameraView.onDestroy();
        CustomCameraConfig.destroy();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "PictureCameraFragment onDestroy");
        super.onDestroy();
    }
}