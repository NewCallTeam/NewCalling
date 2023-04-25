package com.cmcc.newcalllib.adapter.ntv

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cmcc.newcalllib.InjectFragmentActivity
import com.cmcc.newcalllib.adapter.ntv.data.TerminalInfo
import com.cmcc.newcalllib.manage.entity.Results
import com.cmcc.newcalllib.expose.ScreenShareHandler
import com.cmcc.newcalllib.manage.support.Callback
import com.cmcc.newcalllib.manage.support.PathManager
import com.cmcc.newcalllib.tool.DateUtils
import com.cmcc.newcalllib.tool.FileUtil
import com.cmcc.newcalllib.tool.LogUtil
import com.luck.picture.lib.config.SelectMimeType
import com.luck.picture.lib.entity.LocalMedia
import java.io.File
import java.util.*

/**
 * Basic terminal ability provider, including file/media share, location share, screen share.
 * @author jihongfei
 * @createTime 2022/3/16 16:59
 */
class NativeAbilityProviderImpl(
    private val context: Context,
    private val pathManager: PathManager,
) : NativeAbilityProvider {
    companion object {
        const val REQ_INTENT_EXTRA_NAME = "type"
        const val REQ_INTENT_EXTRA_CAPTURE = "capture"
        const val RES_INTENT_EXTRA_NAME = "data"

        const val REQ_OPEN_PIC_BY_CAMERA = 100
        const val REQ_OPEN_PIC_BY_GALLERY = 101
        const val REQ_CHOOSE_FILE = 102
        const val REQ_OPEN_VIDEO_BY_CAMERA = 103
        const val REQ_OPEN_VIDEO_BY_GALLERY = 104

        const val REQ_CODE_CAMERA_PERMISSIONS = 1000 // 相机权限
        const val REQ_CODE_LOCATION_PERMISSIONS = 1001 // 定位权限
    }

    private var mImageCacheFile: File? = null
    private var mVideoCacheFile: File? = null
    private var mActivity: Activity? = null

    // 通过前端 input 标签选文件时使用
    private var mValueCallback: ValueCallback<Array<Uri>>? = null

    // 文件多选时的数量
    private var mCount = 0

    // 通过 js api 选图片、视频、文件时使用
    private var mCallback: Callback<Results<Array<Uri?>>>? = null

    private var method = ::openCameraForPic

    private var mScreenShareHandler: ScreenShareHandler? = null

    private var mLocationCallback: Callback<Results<Pair<Double, Double>>>? = null
    // 获取位置管理器
    private val mLocationManager: LocationManager by lazy { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }

    override fun bindActivity(activity: Activity?) {
        mActivity = activity
    }

    override fun getTerminalInfo(): TerminalInfo {
        return TerminalInfo(android.os.Build.BRAND, android.os.Build.MODEL)
    }

    override fun getSystemInfo() {
        LogUtil.d("getSystemInfo")
    }

    override fun startApp(url: String, callback: Callback<Results<String>>) {
        LogUtil.d("startApp")
        val data = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, data)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            mActivity?.startActivity(intent)
            callback.onResult(Results.success("1"))
        } catch (e: java.lang.Exception) {
            LogUtil.e("没有匹配的 APP ")
            callback.onResult(Results.failure("0"))
        }
    }

    override fun openPicByCamera(callback: Callback<Results<Array<Uri?>>>) {
        LogUtil.d("openPicByCamera")
        mCallback = callback
        method = ::openCameraForPic
        checkPermission()
    }

    override fun openPicByGallery(count: Int, type: String?, callback: Callback<Results<Array<Uri?>>>) {
        LogUtil.d("openPicByGallery")
        mCount = count
        mCallback = callback
        openGalleryForPic(type)
    }

    override fun openVideoByCamera(callback: Callback<Results<Array<Uri?>>>) {
        LogUtil.d("openVideoByCamera")
        mCallback = callback
        method = ::openCameraForVideo
        checkPermission()
    }

    override fun openVideoByGallery(count: Int, type: String?, callback: Callback<Results<Array<Uri?>>>) {
        LogUtil.d("openVideoByGallery")
        mCount = count
        mCallback = callback
        openGalleryForVideo(type)
    }

    override fun chooseFile(count: Int, extension: String, callback: Callback<Results<Array<Uri?>>>) {
        LogUtil.d("chooseFile")
        mCount = count
        mCallback = callback
        chooseFile(extension)
    }

    override fun getLocation(callback: Callback<Results<Pair<Double, Double>>>) {
        mLocationCallback = callback
        if (ContextCompat.checkSelfPermission(mActivity!!, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(mActivity!!, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mActivity!!,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQ_CODE_LOCATION_PERMISSIONS)
        } else {
            getLocationInfo()
        }
    }

    override fun stopGetLocation() {
        mLocationManager.removeUpdates(locationListener)
        mLocationCallback = null
    }

    override fun provideNtvAbilityHandler(): NtvAbilityHandler {
        return object : NtvAbilityHandler {
            override fun onOpenFileChooser(
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: WebChromeClient.FileChooserParams
            ): Boolean {
                LogUtil.d("onOpenFileChooser, " +
                    "fileChooserParams mode=${fileChooserParams.mode} " +
                    "acceptTypes=${Arrays.toString(fileChooserParams.acceptTypes)} " +
                    "isCaptureEnable=${fileChooserParams.isCaptureEnabled} " +
                    "filenameHint=${fileChooserParams.filenameHint}")

                if (mValueCallback != null) {
                    LogUtil.w("Trigger last value callback")
                    mValueCallback?.onReceiveValue(null)
                    mValueCallback = null
                }
                if (mImageCacheFile != null) {
                    LogUtil.w("Last cache file still there")
                    mImageCacheFile = null
                }
                if (mActivity == null) {
                    LogUtil.e("Activity not ready")
                    return false
                }
                mValueCallback = filePathCallback

                try {
                    val acceptTypes = fileChooserParams.acceptTypes
                    if (acceptTypes[0].contains("image")) {
                        if (fileChooserParams.isCaptureEnabled) {
                            method = ::openCameraForPic
                            checkPermission()
                        } else {
                            openGalleryForPic(acceptTypes[0])
                        }
                    } else if (acceptTypes[0].contains("video")) {
                        if (fileChooserParams.isCaptureEnabled) {
                            method = ::openCameraForVideo
                            checkPermission()
                        } else {
                            openGalleryForVideo(acceptTypes[0])
                        }
                    } else {
                        chooseFile(acceptTypes[0])
                    }
                } catch (e: Exception) {
                    LogUtil.e("send intent with exception", e)
                    mValueCallback?.onReceiveValue(null)
                    mValueCallback = null
                }
                return true
            }
        }
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(mActivity!!, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            method()
        } else {
            ActivityCompat.requestPermissions(mActivity!!, arrayOf(Manifest.permission.CAMERA), REQ_CODE_CAMERA_PERMISSIONS)
        }
    }

    private fun openCameraForPic() {
//        val outputFile = buildOutputFile("files", generateFileName("jpg"))
//        mImageCacheFile = outputFile
//        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//        intent.putExtra(MediaStore.EXTRA_OUTPUT, FileUtil.getUri(mActivity, file = outputFile))
//        mActivity?.startActivityForResult(intent, REQ_OPEN_PIC_BY_CAMERA)

        val intent = Intent(mActivity, InjectFragmentActivity::class.java)
        intent.putExtra(REQ_INTENT_EXTRA_NAME, SelectMimeType.TYPE_IMAGE)
        intent.putExtra(REQ_INTENT_EXTRA_CAPTURE, true)
        mActivity?.startActivityForResult(intent, REQ_OPEN_PIC_BY_CAMERA)
    }

    private fun openCameraForVideo() {
//        val outputFile = buildOutputFile("files", generateFileName("mp4"))
//        mVideoCacheFile = outputFile
//        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
//        intent.putExtra(MediaStore.EXTRA_OUTPUT, FileUtil.getUri(mActivity, file = outputFile))
//        mActivity?.startActivityForResult(intent, REQ_OPEN_VIDEO_BY_CAMERA)

        val intent = Intent(mActivity, InjectFragmentActivity::class.java)
        intent.putExtra(REQ_INTENT_EXTRA_NAME, SelectMimeType.TYPE_VIDEO)
        intent.putExtra(REQ_INTENT_EXTRA_CAPTURE, true)
        mActivity?.startActivityForResult(intent, REQ_OPEN_VIDEO_BY_CAMERA)
    }

    private fun openGalleryForPic(type: String?) {
//        val intent = Intent()
//        if (type.isNullOrBlank()) {
//            intent.type = "image/*"
//        } else {
//            intent.type = type
//        }
//        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) // 多选
//        intent.action = Intent.ACTION_OPEN_DOCUMENT // 文件浏览器
//        intent.addCategory(Intent.CATEGORY_OPENABLE)
//        mActivity?.startActivityForResult(intent, REQ_OPEN_PIC_BY_GALLERY)

        val intent = Intent(mActivity, InjectFragmentActivity::class.java)
        intent.putExtra(REQ_INTENT_EXTRA_NAME, SelectMimeType.TYPE_IMAGE)
        intent.putExtra(REQ_INTENT_EXTRA_CAPTURE, false)
        mActivity?.startActivityForResult(intent, REQ_OPEN_PIC_BY_GALLERY)
    }

    private fun openGalleryForVideo(type: String?) {
//        val intent = Intent()
//        if (type.isNullOrBlank()) {
//            intent.type = "video/*"
//        } else {
//            intent.type = type
//        }
//        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
//        intent.action = Intent.ACTION_OPEN_DOCUMENT
//        intent.addCategory(Intent.CATEGORY_OPENABLE)
//        mActivity?.startActivityForResult(intent, REQ_OPEN_VIDEO_BY_GALLERY)

        val intent = Intent(mActivity, InjectFragmentActivity::class.java)
        intent.putExtra(REQ_INTENT_EXTRA_NAME, SelectMimeType.TYPE_VIDEO)
        intent.putExtra(REQ_INTENT_EXTRA_CAPTURE, false)
        mActivity?.startActivityForResult(intent, REQ_OPEN_VIDEO_BY_GALLERY)
    }

    private fun chooseFile(extension: String) {
        val intent = Intent()
        if (extension.isBlank()) {
            intent.type = "*/*"
        } else {
            intent.type = extension
        }
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.action = Intent.ACTION_OPEN_DOCUMENT
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        mActivity?.startActivityForResult(intent, REQ_CHOOSE_FILE)
    }

    private fun generateFileName(suffix: String): String {
        return DateUtils.getCurDate("yyyyMMdd_HHmmss") + ".$suffix"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        LogUtil.d("onActivityResult, reqCode=$requestCode, resCode=$resultCode, data=$data")
        if (resultCode != Activity.RESULT_OK) {
            LogUtil.e("Result code from activity: $resultCode")
            mValueCallback?.onReceiveValue(null)
            mValueCallback = null
            return
        }
        if (mActivity == null) {
            LogUtil.e("Activity not ready")
            return
        }
        // dispatch by requestCode
        when (requestCode) {
            REQ_OPEN_PIC_BY_CAMERA -> {
                val resultData = data?.getParcelableArrayListExtra<LocalMedia>(RES_INTENT_EXTRA_NAME)
                if (resultData != null) {
                    val arrayOfUris = resultData.map {
                        Uri.parse(it.availablePath)
                    }.toTypedArray()
                    if (mValueCallback != null) {
                        mValueCallback?.onReceiveValue(arrayOfUris)
                        mValueCallback = null
                    } else if (mCallback != null) {
                        mCallback?.onResult(Results.success(arrayOfUris))
                        mCallback = null
                    } else {
                        LogUtil.e("Callback not trigger")
                    }
                } else {
                    LogUtil.d("Get photo, cached file=$mImageCacheFile, intent=$data")
                    if (mImageCacheFile != null) {
                        val uri = FileUtil.getUri(mActivity, file = mImageCacheFile)
                        if (mValueCallback != null) {
                            mValueCallback?.onReceiveValue(arrayOf(uri))
                            mValueCallback = null
                        } else if (mCallback != null) {
                            mCallback?.onResult(Results.success(arrayOf(uri)))
                            mCallback = null
                        } else {
                            LogUtil.e("Callback not trigger")
                        }
                        mImageCacheFile = null
                    } else {
                        mValueCallback?.onReceiveValue(null)
                        mValueCallback = null
                        mCallback?.onResult(Results())
                        mCallback = null
                    }
                }
            }
            REQ_OPEN_PIC_BY_GALLERY -> {
                val resultData = data?.getParcelableArrayListExtra<LocalMedia>(RES_INTENT_EXTRA_NAME)
                if (resultData != null) {
                    val arrayOfUris = resultData.map {
                        if (it.isCompressed) {
                            FileUtil.getUri(mActivity, file = File(it.compressPath));
                        } else {
                            Uri.parse(it.path)
                        }
                    }.toTypedArray()
                    if (mValueCallback != null) {
                        mValueCallback?.onReceiveValue(arrayOfUris.requireNoNulls())
                        mValueCallback = null
                    } else if (mCallback != null) {
                        mCallback?.onResult(Results.success(arrayOfUris))
                        mCallback = null
                    } else {
                        LogUtil.e("Callback not trigger")
                    }
                } else {
                    val uri = data?.data
                    if (uri != null) {
                        if (mValueCallback != null) {
                            mValueCallback?.onReceiveValue(arrayOf(uri))
                            mValueCallback = null
                        } else if (mCallback != null) {
                            mCallback?.onResult(Results.success(arrayOf(uri)))
                            mCallback = null
                        } else {
                            LogUtil.e("Callback not trigger")
                        }
                    } else {
                        // 多张图片
                        val clipData = data?.clipData
                        if (clipData != null) {
                            val min = Math.min(mCount, clipData.itemCount)
                            val arrayOfUris = arrayOfNulls<Uri>(min)
                            for (i in 0 until min) {
                                val uri = clipData.getItemAt(i).uri
                                arrayOfUris[i] = uri
                            }
                            LogUtil.d("Get images uri=$arrayOfUris.")
                            if (mValueCallback != null) {
                                mValueCallback?.onReceiveValue(arrayOfUris.requireNoNulls())
                                mValueCallback = null
                            } else if (mCallback != null) {
                                mCallback?.onResult(Results.success(arrayOfUris))
                                mCallback = null
                            } else {
                                LogUtil.e("Callback not trigger")
                            }
                        } else {
                            mValueCallback?.onReceiveValue(null)
                            mValueCallback = null
                            mCallback?.onResult(Results())
                            mCallback = null
                        }
                        mCount = 0
                    }
                }
            }
            REQ_CHOOSE_FILE -> {
                val uri = data?.data
                if (uri != null) {
                    if (mValueCallback != null) {
                        mValueCallback?.onReceiveValue(arrayOf(uri))
                        mValueCallback = null
                    } else if (mCallback != null) {
                        mCallback?.onResult(Results.success(arrayOf(uri)))
                        mCallback = null
                    } else {
                        LogUtil.e("Callback not trigger")
                    }
                } else {
                    val clipData = data?.clipData
                    if (clipData != null) {
                        val min = Math.min(mCount, clipData.itemCount)
                        val arrayOfUris = arrayOfNulls<Uri>(min)
                        for (i in 0 until min) {
                            val uri = clipData.getItemAt(i).uri
                            arrayOfUris[i] = uri
                        }
                        LogUtil.d("Get files uri=$arrayOfUris")
                        if (mValueCallback != null) {
                            mValueCallback?.onReceiveValue(arrayOfUris.requireNoNulls())
                            mValueCallback = null
                        } else if (mCallback != null) {
                            mCallback?.onResult(Results.success(arrayOfUris))
                            mCallback = null
                        } else {
                            LogUtil.e("Callback not trigger")
                        }
                    } else {
                        mValueCallback?.onReceiveValue(null)
                        mValueCallback = null
                        mCallback?.onResult(Results())
                        mCallback = null
                    }
                    mCount = 0
                }
            }
            REQ_OPEN_VIDEO_BY_CAMERA -> {
                val resultData = data?.getParcelableArrayListExtra<LocalMedia>(RES_INTENT_EXTRA_NAME)
                if (resultData != null) {
                    val arrayOfUris = resultData.map {
                        Uri.parse(it.availablePath)
                    }.toTypedArray()
                    if (mValueCallback != null) {
                        mValueCallback?.onReceiveValue(arrayOfUris)
                        mValueCallback = null
                    } else if (mCallback != null) {
                        mCallback?.onResult(Results.success(arrayOfUris))
                        mCallback = null
                    } else {
                        LogUtil.e("Callback not trigger")
                    }
                } else {
                    LogUtil.d("Get video, cached file=$mVideoCacheFile, intent=$data")
                    if (mVideoCacheFile != null) {
                        val uri = FileUtil.getUri(mActivity, file = mVideoCacheFile)
                        if (mValueCallback != null) {
                            mValueCallback?.onReceiveValue(arrayOf(uri))
                            mValueCallback = null
                        } else if (mCallback != null) {
                            mCallback?.onResult(Results.success(arrayOf(uri)))
                            mCallback = null
                        } else {
                            LogUtil.e("Callback not trigger")
                        }
                        mVideoCacheFile = null
                    } else {
                        mValueCallback?.onReceiveValue(null)
                        mValueCallback = null
                        mCallback?.onResult(Results())
                        mCallback = null
                    }
                }
            }
            REQ_OPEN_VIDEO_BY_GALLERY -> {
                val resultData = data?.getParcelableArrayListExtra<LocalMedia>(RES_INTENT_EXTRA_NAME)
                if (resultData != null) {
                    val arrayOfUris = resultData.map {
                        if (it.isCompressed) {
                            FileUtil.getUri(mActivity, file = File(it.compressPath));
                        } else {
                            Uri.parse(it.path)
                        }
                    }.toTypedArray()
                    if (mValueCallback != null) {
                        mValueCallback?.onReceiveValue(arrayOfUris.requireNoNulls())
                        mValueCallback = null
                    } else if (mCallback != null) {
                        mCallback?.onResult(Results.success(arrayOfUris))
                        mCallback = null
                    } else {
                        LogUtil.e("Callback not trigger")
                    }
                } else {
                    val uri = data?.data
                    if (uri != null) {
                        if (mValueCallback != null) {
                            mValueCallback?.onReceiveValue(arrayOf(uri))
                            mValueCallback = null
                        } else if (mCallback != null) {
                            mCallback?.onResult(Results.success(arrayOf(uri)))
                            mCallback = null
                        } else {
                            LogUtil.e("Callback not trigger")
                        }
                    } else {
                        // 多个视频
                        val clipData = data?.clipData
                        if (clipData != null) {
                            val min = Math.min(mCount, clipData.itemCount)
                            val arrayOfUris = arrayOfNulls<Uri>(min)
                            for (i in 0 until min) {
                                val uri = clipData.getItemAt(i).uri
                                arrayOfUris[i] = uri
                            }
                            LogUtil.d("Get videos uri=$arrayOfUris")
                            if (mValueCallback != null) {
                                mValueCallback?.onReceiveValue(arrayOfUris.requireNoNulls())
                                mValueCallback = null
                            } else if (mCallback != null) {
                                mCallback?.onResult(Results.success(arrayOfUris))
                                mCallback = null
                            } else {
                                LogUtil.e("Callback not trigger")
                            }
                        } else {
                            mValueCallback?.onReceiveValue(null)
                            mValueCallback = null
                            mCallback?.onResult(Results())
                            mCallback = null
                        }
                        mCount = 0
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQ_CODE_CAMERA_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                method()
            } else {
                LogUtil.e("Failed to obtain the camera permission.")
            }
        }
        if (requestCode == REQ_CODE_LOCATION_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                getLocationInfo()
            } else {
                LogUtil.i("No location permission")
                mLocationCallback?.onResult(Results.failure("No location permission"))
            }
        }
    }

    private fun buildOutputFile(dirName: String, fName: String): File? {
        var createCacheFile: File? = null
        try {
            createCacheFile = pathManager.createCacheFile(dirName = dirName, fileName = fName)
        } catch (e: Exception) {
            LogUtil.e("buildOutputFile fail", e)
            return null
        }
        return createCacheFile
    }

    private fun getLocationInfo() {
        try {
            val locationProvider: String
            // 获取位置提供器，GPS 或是 NetWork
            val providers = mLocationManager.getProviders(true)
            if (providers.contains(LocationManager.GPS_PROVIDER)) {
                locationProvider = LocationManager.GPS_PROVIDER
                LogUtil.d("The location mode is GPS")
            } else if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
                locationProvider = LocationManager.NETWORK_PROVIDER
                LogUtil.d("The location mode is Network")
            } else {
                LogUtil.i("Please enable location and try again")
                mLocationCallback?.onResult(Results.failure("Please enable location and try again"))
                return
            }
            // 获取上次的位置，一般第一次运行，此值为 null
            val location = mLocationManager.getLastKnownLocation(locationProvider)
            if (location != null) {
                val longitude = location.longitude
                val latitude = location.latitude
                LogUtil.d("Get the last location - longitude and latitude:$longitude $latitude")
                mLocationCallback?.onResult(Results(Pair(longitude, latitude)))
            } else {
                // 监视地理位置变化，第二个和第三个参数分别为更新的最短时间 minTime 和最短距离 minDistace
                mLocationManager.requestLocationUpdates(locationProvider, 3000, 1F, locationListener)
            }
        } catch (e: SecurityException) {
            LogUtil.e("SecurityException:${e.message}")
            mLocationCallback?.onResult(Results.failure("SecurityException:${e.message}"))
        }
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // 当坐标改变时触发此函数，如果 Provider 传进相同的坐标，它就不会被触发
            // 如果位置发生变化，重新显示地理位置经纬度
            val longitude = location.longitude
            val latitude = location.latitude
            LogUtil.d("Monitor location changes - longitude and latitude:$longitude $latitude")
            mLocationCallback?.onResult(Results(Pair(longitude, latitude)))
        }
    }
}