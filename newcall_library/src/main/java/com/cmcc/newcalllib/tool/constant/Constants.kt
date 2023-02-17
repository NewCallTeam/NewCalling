package com.cmcc.newcalllib.tool.constant

import android.text.TextUtils
import com.cmcc.newcalllib.BuildConfig
import com.cmcc.newcalllib.adapter.network.Origin
import com.cmcc.newcalllib.tool.LogUtil

class Constants {
    companion object {
        const val APP_NAME = "CMCC_NewCall"

        // $FILE_HOME/frame/index.html
        const val FRAME_ROOT_PATH = "frame/"

        // $FILE_HOME/mini_app/$APP_ID/index.html
        const val MINI_APP_ROOT_PATH = "mini_app/"
        const val INDEX_FILE_NAME = "index.html"
        const val GUIDE_FILE_NAME = "index.html"
        const val URI_FILE_PREFIX = "file://"

        const val BOOTSTRAP_MINI_APP_ID = "bootstrap"
        const val BOOTSTRAP_DATA_CHANNEL_LABEL_LOCAL = "local-bdc"//stream-id 0
        const val BOOTSTRAP_DATA_CHANNEL_LABEL_REMOTE = "remote-bdc"//stream-id 100
        const val BOOTSTRAP_DATA_CHANNEL_STREAM_ID_LOCAL = "0"
        const val BOOTSTRAP_DATA_CHANNEL_STREAM_ID_REMOTE = "100"

        const val TRANSFER_FILE_ROOT_PATH = "trans/"
        const val MANUALLY_SAVE_FILE_ROOT_PATH = "save/"
        const val MINI_APP_PRIVATE_SPACE_PATH = "priv/"

        /**
         * 获取bdc label
         */
        fun getBdcLableByOrigin(origin: Origin): String {
            val label = if (origin == Origin.LOCAL) {
                BOOTSTRAP_DATA_CHANNEL_LABEL_LOCAL
            } else {
                BOOTSTRAP_DATA_CHANNEL_LABEL_REMOTE
            }
            return label
        }

        /**
         * 获取bdc label
         */
        fun getBdcLableByStreamId(streamId: String): String {
            LogUtil.d("getBdcLableByStreamId", "streamId=$streamId")
            if (TextUtils.isEmpty(streamId)) {
                return ""
            }
            val label = if ("0" == streamId) {
                BOOTSTRAP_DATA_CHANNEL_LABEL_LOCAL
            } else if ("100" == streamId) {
                BOOTSTRAP_DATA_CHANNEL_LABEL_REMOTE
            } else {
                ""
            }
            return label
        }


    }
}