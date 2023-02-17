package com.cmcc.newcalllib.tool

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import com.cmcc.newcalllib.BuildConfig
import java.io.File
import java.util.concurrent.Executors

@Suppress("MemberVisibilityCanBePrivate")
object LogUtil {
    // log levels
    const val VERBOSE = Log.VERBOSE
    const val DEBUG = Log.DEBUG
    const val INFO = Log.INFO
    const val WARN = Log.WARN
    const val ERROR = Log.ERROR

    private var mInitial = false
    private var mEnable = false
    private var mWriteToFile = false
    private var mCacheDir: File? = null
    private lateinit var mLogStackTraceOrigin: String
    private val EXECUTOR = Executors.newSingleThreadExecutor()
    private var mLogLevel: Int = DEBUG
    private const val BASE_TAG = "CMCC_NC"
    private const val LOG_FILES_DIR = "cmcc_logs"
    private const val LOG_FILE_PREFIX = ""
    private const val LOG_FILE_EXTENSION = "-nc.log"
    private const val ENABLE_SHRINK = true
    private const val SHRINK_MAXIMUM = 2048
    private val mRegexToReplace = arrayOf("base64,[\\/]?([\\da-zA-Z]+[\\\\\\/+]+)*[\\da-zA-Z]+([+=]{1,2}|[\\/])?")

    fun initLog(context: Context, enable: Boolean = BuildConfig.DEBUG, writeToFile: Boolean = false) {
        if (!mInitial) {
            mEnable = enable
            mWriteToFile = writeToFile
            mCacheDir = context.cacheDir
            // assign package name
            mLogStackTraceOrigin = BuildConfig.LIBRARY_PACKAGE_NAME
            mInitial = true
        }
    }

    fun setLogLevel(level: Int) {
        mLogLevel = level
    }

    private fun getApplication(context: Context): Application? {
        if (context is Application) {
            return context
        }
        if (context is Activity) {
            return context.application
        }
        return null
    }

    private fun defaultTag(): String {
        val ste = Thread.currentThread().stackTrace[4]
        return "$BASE_TAG-[T:${Thread.currentThread().name}, ${ste.className.substringAfterLast(".")}${getTrace()}]"
    }

    private fun print2File(msg: String) {
        if (!mWriteToFile || mCacheDir == null) {
            return
        }
        EXECUTOR.execute(object : Runnable {
            override fun run() {
                input2File(msg)
            }
        })
    }

    fun shrink(msg: String): String {
        var ret = msg
        if (ENABLE_SHRINK) {
            mRegexToReplace.forEach {
                ret = msg.replace(Regex(it)) { mr ->
                    "[md5:${StringUtil.md5(mr.value)}]"
                }
            }
            if (ret.length > SHRINK_MAXIMUM) {
                ret = ret.substring(0, SHRINK_MAXIMUM)
            }
        }
        return ret
    }

    private fun input2File(msg: String) {
        val format: String = DateUtils.getCurDate("yyyy-MM-dd HH:mm:ss.SSS")
        val date = format.substring(0, 10)
        val time = format.substring(11)
        val fullPath = "${mCacheDir?.absolutePath}${File.separator}$LOG_FILES_DIR${File.separator}" +
            "$LOG_FILE_PREFIX$date$LOG_FILE_EXTENSION"

        if (!FileUtil.exists(fullPath, false)) {
            FileUtil.createFile(fullPath)
            printDeviceInfo(fullPath, date)
        }
        val content = "$date $time $msg${System.getProperty("line.separator")}"
        FileUtil.writeFileFromString(fullPath, content, true)
    }

    private fun printDeviceInfo(fullPath: String, date: String) {
        val head = """
            ************* Log Head ****************
            Date of Log        : $date
            Device Manufacturer: ${Build.MANUFACTURER}
            Device Model       : ${Build.MODEL}
            Android Version    : ${Build.VERSION.RELEASE}
            Android SDK        : ${Build.VERSION.SDK_INT}
            App VersionName    : ${BuildConfig.VERSION_CODE}
            App VersionCode    : ${BuildConfig.VERSION_NAME}
            ************* Log Head ****************${System.getProperty("line.separator")}
            """.trimIndent()
        FileUtil.writeFileFromString(fullPath, head, true)
    }

    private fun getTrace(): String {
        val sb = StringBuilder()
        val e = Exception()
        val traceA = e.stackTrace
        if (traceA == null || traceA.size <= 0) {
            return ""
        }
        val traceB: Array<StackTraceElement?> = getRealStackTrack(traceA, null)
        if (traceB == null || traceB.size <= 0) {
            return ""
        }
        sb.append(".")
        sb.append(traceB[0]?.methodName)
        sb.append("(" + traceB[0]?.fileName + ":" + traceB[0]?.lineNumber + ")")
        return sb.toString()
    }

    private fun getRealStackTrack(stackTrace: Array<StackTraceElement>, stackTraceOrigin: String?): Array<StackTraceElement?> {
        var ignoreDepth = 0
        val allDepth = stackTrace.size
        var className: String
        for (i in allDepth - 1 downTo 0) {
            className = stackTrace[i].className
            if (!::mLogStackTraceOrigin.isInitialized) {
                mLogStackTraceOrigin = ""
            }
            if (className.startsWith(mLogStackTraceOrigin)
                || stackTraceOrigin != null
                && className.startsWith(stackTraceOrigin)) {
                ignoreDepth = i
                break
            }
        }
        val realDepth = allDepth - ignoreDepth
        val realStack = arrayOfNulls<StackTraceElement>(realDepth)
        System.arraycopy(stackTrace, ignoreDepth, realStack, 0, realDepth)
        return realStack
    }

    fun v(msg: String) {
        v(defaultTag(), msg)
    }

    fun d(msg: String) {
        d(defaultTag(), msg)
    }

    fun i(msg: String) {
        i(defaultTag(), msg)
    }

    fun w(msg: String) {
        w(defaultTag(), msg)
    }

    fun e(msg: String, t: Throwable? = null) {
        e(defaultTag(), msg, t)
    }

    fun v(tag: String, msg: String) {
        if (!mEnable) {
            return
        }
        if (mLogLevel > VERBOSE) {
            return
        }
        val shrinkMsg = shrink(msg)
        Log.v(tag, shrinkMsg)
        print2File("V/$tag: $shrinkMsg")
    }

    fun d(tag: String, msg: String) {
        if (!mEnable) {
            return
        }
        if (mLogLevel > DEBUG) {
            return
        }
        val shrinkMsg = shrink(msg)
        Log.d(tag, shrinkMsg)
        print2File("D/$tag: $shrinkMsg")
    }

    fun i(tag: String, msg: String) {
        if (!mEnable) {
            return
        }
        if (mLogLevel > INFO) {
            return
        }
        val shrinkMsg = shrink(msg)
        Log.i(tag, shrinkMsg)
        print2File("I/$tag: $shrinkMsg")
    }

    fun w(tag: String, msg: String) {
        if (!mEnable) {
            return
        }
        if (mLogLevel > WARN) {
            return
        }
        val shrinkMsg = shrink(msg)
        Log.w(tag, shrinkMsg)
        print2File("W/$tag: $shrinkMsg")
    }

    fun e(tag: String, msg: String, t: Throwable?) {
        if (!mEnable) {
            return
        }
        if (mLogLevel > ERROR) {
            return
        }
        val shrinkMsg = shrink(msg)
        Log.e(tag, shrinkMsg, t)
        print2File("E/$tag: $shrinkMsg ${t?.message}")
    }
}