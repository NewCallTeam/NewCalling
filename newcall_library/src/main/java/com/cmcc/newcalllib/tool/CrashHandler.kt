package com.cmcc.newcalllib.tool

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*

object CrashHandler : Thread.UncaughtExceptionHandler {

    private const val TAG = "CrashHandler "

    private lateinit var mContext: Context
    private val mInfos: MutableMap<String, String> = HashMap()
    private lateinit var mDefaultHandler: Thread.UncaughtExceptionHandler

    fun init(context: Context) {
        mContext = context
        // 获取系统默认的 UncaughtException 处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        // 设置 CrashHandler 为程序默认的 UncaughtException 处理器
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        if (!handleException(throwable) && mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(thread, throwable)
        } else {
            try {
                Thread.sleep(1000)
            } catch (e: InterruptedException) {
                Log.e(TAG, "error: ${e.message}")
            }
            Process.killProcess(Process.myPid())
            System.exit(1)
        }
    }

    private fun handleException(throwable: Throwable): Boolean {
        if (throwable == null) {
            return false
        }
        collectDeviceInfo(mContext)
        saveCrashInfo2File(throwable)
        return true
    }

    /**
     * 收集设备信息
     *
     * @param context
     */
    private fun collectDeviceInfo(context: Context) {
        try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES)
            if (packageInfo != null) {
                val versionName = packageInfo.versionName ?: "null"
                val versionCode = packageInfo.versionCode.toString()
                mInfos["versionName"] = versionName
                mInfos["versionCode"] = versionCode
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "an error occured when collect package info ${e.message}")
        }

        val fields = Build::class.java.declaredFields
        for (field in fields) {
            try {
                field.isAccessible = true
                mInfos[field.name] = field.get(null).toString()
                Log.d(TAG, "${field.name} : ${field.get(null)}")
            } catch (e: Exception) {
                Log.e(TAG, "an error occured when collect package info ${e.message}")
            }
        }
    }

    /**
     * 保存信息到文件
     *
     * @param throwable
     */
    private fun saveCrashInfo2File(throwable: Throwable) {
        val sb = StringBuffer()
        for (entry in mInfos) {
            sb.append("${entry.key}=${entry.value}${System.getProperty("line.separator")}")
        }

        val writer = StringWriter()
        val printWriter = PrintWriter(writer)
        throwable.printStackTrace(printWriter)
        var cause = throwable.cause
        while (cause != null) {
            cause.printStackTrace(printWriter)
            cause = cause.cause
        }
        printWriter.close()

        val result = writer.toString()
        sb.append(result)

        val timestamp = System.currentTimeMillis()
        val time: String = DateUtils.getCurDate("yyyy-MM-dd HH:mm:ss")
        val fullPath = "${mContext.cacheDir}${File.separator}crash${File.separator}crash-$time-$timestamp.log"
        if (!FileUtil.exists(fullPath, false)) {
            FileUtil.createFile(fullPath)
        }
        FileUtil.writeFileFromString(fullPath, sb.toString(), true)
    }

}