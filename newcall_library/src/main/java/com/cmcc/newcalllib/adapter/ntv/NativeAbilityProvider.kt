package com.cmcc.newcalllib.adapter.ntv

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.cmcc.newcalllib.adapter.ntv.data.TerminalInfo
import com.cmcc.newcalllib.manage.support.Callback
import com.cmcc.newcalllib.manage.entity.Results
import com.cmcc.newcalllib.manage.entity.handler.resp.RespGetLocation

/**
 * @author jihongfei
 * @createTime 2022/2/22 11:41
 */
interface NativeAbilityProvider {
    fun getSystemInfo()
    fun startApp(url: String, callback: Callback<Results<String>>)
    fun openPicByCamera(callback: Callback<Results<Array<Uri?>>>)
    fun openPicByGallery(count: Int, type: String?, callback: Callback<Results<Array<Uri?>>>)
    fun openVideoByCamera(callback: Callback<Results<Array<Uri?>>>)
    fun openVideoByGallery(count: Int, type: String?, callback: Callback<Results<Array<Uri?>>>)
    fun chooseFile(count: Int, extension: String, callback: Callback<Results<Array<Uri?>>>)
    fun getLocation(callback: Callback<Results<Pair<Double, Double>>>)
    fun stopGetLocation()
    /**
     * return handler implemented in NativeAbilityProvider
     */
    fun provideNtvAbilityHandler(): NtvAbilityHandler

    /**
     * handle intent result
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray)

    /**
     * set activity of Dialer
     */
    fun bindActivity(activity: Activity?)

    /**
     * return terminal info, such as vendor and model
     */
    fun getTerminalInfo(): TerminalInfo
}