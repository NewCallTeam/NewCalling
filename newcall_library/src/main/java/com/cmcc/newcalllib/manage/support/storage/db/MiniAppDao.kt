package com.cmcc.newcalllib.manage.support.storage.db

import androidx.room.*

/**
 * @author jihongfei
 * @createTime 2022/3/18 11:22
 */
@Dao
interface MiniAppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg miniApps: MiniApp)

    @Update
    fun update(vararg miniApps: MiniApp)

    @Delete
    fun delete(miniApp: MiniApp)

    @Query("SELECT * FROM MiniApp WHERE appId = :appId")
    fun getOne(appId: String): MiniApp?

    @Query("SELECT * FROM MiniApp WHERE appId IN (:appIds)")
    fun getList(appIds: List<String>): List<MiniApp>

    @Query("SELECT * FROM MiniApp")
    fun getAll(): List<MiniApp>
}