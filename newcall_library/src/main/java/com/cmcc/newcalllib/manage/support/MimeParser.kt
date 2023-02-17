package com.cmcc.newcalllib.manage.support

/**
 * @author jihongfei
 * @createTime 2022/8/3 14:19
 */
class MimeParser(mime: String) {
    var mValid: Boolean = true
    var mExt: String = "tmp"

    init {
        if (mime.isNotEmpty()) {
            when (mime) {
                "image/png" -> {
                    mExt = "png"
                }
                "image/jpeg" -> {
                    mExt = "jpg"
                }
                "image/gif" -> {
                    mExt = "gif"
                }
                else -> {
                    mValid = false
                }
            }
        } else {
            mValid = false
        }
    }

    fun isValid(): Boolean {
        return mValid
    }

    fun getExtension(): String {
        return mExt
    }
}