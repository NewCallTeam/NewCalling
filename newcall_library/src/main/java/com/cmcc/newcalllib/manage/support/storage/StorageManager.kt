package com.cmcc.newcalllib.manage.support.storage

import android.content.Context
import com.cmcc.newcalllib.manage.mna.repo.*
import com.cmcc.newcalllib.manage.support.storage.db.NewCallDatabase

/**
 * Manager of local storage
 * @author jihongfei
 * @createTime 2022/6/27 15:20
 */
class StorageManager constructor(val context: Context, val sessionId: String?) {

    companion object {
        const val SP_NAME_CROSS_SESSION: String = "SP_CROSS_SESSION"
        const val SP_NAME_IN_SESSION: String = "SP_IN_SESSION"
    }
    private lateinit var mDatabase: NewCallDatabase
    private lateinit var mSpUtil: SpUtil
    private lateinit var mSpInSession: SpUtil
    private lateinit var mMiniAppRepo: MiniAppRepo
    private lateinit var mTransferFileRepo: TransferFileRepo
    private lateinit var mMiniAppSpaceRepo: MiniAppSpaceRepo

    fun initStorageManager() {
        mDatabase = NewCallDatabase.initialize(context)
        mSpUtil = SpUtil(context, SP_NAME_CROSS_SESSION)
        mSpInSession = SpUtil(context, SP_NAME_IN_SESSION)

        mMiniAppRepo = MiniAppRepoImpl(mSpUtil, mDatabase.miniAppDao())
        mTransferFileRepo = TransferFileRepoImpl(context, mDatabase.persistentMessageDao())
        mMiniAppSpaceRepo = MiniAppSpaceRepoImpl(mSpUtil)
    }

    fun getDatabase(): NewCallDatabase {
        return mDatabase
    }

    fun getSp(): SpUtil {
        return mSpUtil
    }

    fun getSpInSession(): SpUtil {
        return mSpInSession
    }

    fun getMiniAppRepo(): MiniAppRepo {
        return mMiniAppRepo
    }

    fun getTransferFileRepo(): TransferFileRepo {
        return mTransferFileRepo
    }

    fun getMiniAppSpaceRepo(): MiniAppSpaceRepo {
        return mMiniAppSpaceRepo
    }

    fun resetSpInSession() {
        mSpInSession.clear()
    }
}