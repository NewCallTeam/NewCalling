package com.cmcc.newcalllib.manage.bussiness.interact

import com.cmcc.newcalllib.manage.ext.ExtensionManager
import com.cmcc.newcalllib.adapter.network.NetworkAdapter
import com.cmcc.newcalllib.adapter.ntv.NativeAbilityProvider
import com.cmcc.newcalllib.bridge.CallBackFunction
import com.cmcc.newcalllib.manage.bussiness.NewCallManager.MainThreadEventHandler
import com.cmcc.newcalllib.manage.support.storage.StorageManager

/**
 * @author jihongfei
 * @createTime 2022/2/22 10:43
 */
abstract class BaseJsCommunicator(
    open val handler: MainThreadEventHandler,
    open val networkAdapter: NetworkAdapter,
    open val abilityProvider: NativeAbilityProvider,
    open val extensionManager: ExtensionManager,
    open val storageManager: StorageManager
) : JsCaller, JsHandler {

    var jsFuncCaller: ((String, String, CallBackFunction?) -> Unit)? = null

    /**
     * call js function if needed
     */
    override fun callJsFunction(jsFuncName: String, data: String, cb: CallBackFunction?) {
        jsFuncCaller?.invoke(jsFuncName, data, cb)
    }
}