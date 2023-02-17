package com.cmcc.newcalllib.manage.bussiness.interact;

import android.content.Context;
import android.webkit.DownloadListener;

import com.cmcc.newcalllib.manage.support.PathManager;
import com.cmcc.newcalllib.tool.LogUtil;
import com.cmcc.newcalllib.tool.StringUtil;
import com.cmcc.newcalllib.tool.thread.TaskCallback;
import com.cmcc.newcalllib.tool.thread.ThreadPoolUtil;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * @author jihongfei
 * @createTime 2022/8/3 10:02
 */
@Deprecated
public class CmccWebViewDownLoadListener implements DownloadListener {
    private final Context mContext;
    private final String mRemoteNum;
    private final PathManager mPathManager;
    private DownloadFinishCallback mFinishCallback;

    public CmccWebViewDownLoadListener(Context context, String remoteNum, PathManager pathManager) {
        mContext = context;
        mRemoteNum = remoteNum;
        mPathManager = pathManager;
    }

    public void setFinishCallback(DownloadFinishCallback callback) {
        mFinishCallback = callback;
    }

    @Override
    public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype,
                                long contentLength) {
        LogUtil.INSTANCE.d("onDownloadStart, len=" + contentLength + ", disposition=" + contentDisposition + ", mime=" + mimetype + ", ua=" + userAgent + ", url=" + url);
        StringUtil stringUtil = StringUtil.INSTANCE;
        String mime = stringUtil.extractMimeType(url);
        String fileName = mPathManager.genTransferFileName(mime);
        File file = mPathManager.getTransferFile(mRemoteNum, fileName);
        if (file.exists()) {
            ThreadPoolUtil.INSTANCE.runOnIOThenUIThread(new Callable<Boolean>() {
                @Override
                public Boolean call() {
                    // base64 to file
                    boolean result = stringUtil.base64ToFile(stringUtil.extractPureBase64(url), file.getPath());
                    // TODO gen thumbnail if needed
                    LogUtil.INSTANCE.i("save base64 to file, result=" + result);
                    return result;
                }
            }, new TaskCallback<Boolean, Boolean>() {
                @Override
                public Boolean process(Boolean result) {
                    if (mFinishCallback != null) {
                        mFinishCallback.onResult(result, file);
                    }
                    return result;
                }
            });
        } else {
            LogUtil.INSTANCE.w("file not exists");
        }
    }

    public interface DownloadFinishCallback {
        void onResult(boolean success, File file);
    }
}
