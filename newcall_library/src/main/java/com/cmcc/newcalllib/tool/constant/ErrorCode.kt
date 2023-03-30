package com.cmcc.newcalllib.tool.constant

/**
 * @author jihongfei
 * @createTime 2022/2/22 14:26
 */
enum class ErrorCode(private val code: Int, private val reason: String) {
    UNKNOWN(1000, "Unknown failure"),

    // for JS
    BS_TOKEN_INVALID(1001, "Token from bootstrap mini-app is invalid"),
    JSON_PARSE_WRONG(1002, "Json parse with exception"),
    ARGUMENTS_ILLEGAL(1003, "Given arguments is illegal"),
    DATA_CHANNEL_NOT_AVAILABLE(1004, "DataChannel not available"),
    MINI_APP_STACK_WRONG(1005, "Mini-app stack in chaos"),
    TRANSFER_FILE_NOT_SAVE(1006, "Transfer file save fail"),
    DECOMPRESS_FILE_FAILED(1007, "The path does not end with the zip specified suffix"),
    FILE_OR_FOLDER_NOT_EXIST(1008, "The file or folder for this path does not exist"),
    FOLDER_IS_EMPTY(1008, "The folder for this path is empty"),

    // for APP
    BS_DATA_CHANNEL_ESTABLISH_FAIL(2000, "Bootstrap DC can not establish"),
    LAUNCH_MINI_APP_FAIL(2001, "Launch mini-app fail"),
    ACTIVITY_NOT_BIND(2002, "Need bind InCallUI activity first"),
    WEBVIEW_NOT_AVAILABLE(2003, "WebView is null"),
    ;

    fun code(): Int {
        return code
    }
    fun reason(): String {
        return reason
    }

    companion object {
        fun valueOf(code: Int): ErrorCode? {
            return values().firstOrNull { code == it.code() }
        }
    }
}