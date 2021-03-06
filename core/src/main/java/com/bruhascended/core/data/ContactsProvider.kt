package com.bruhascended.core.data

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.bruhascended.core.constants.*
import com.bruhascended.core.db.Contact
import com.bruhascended.core.db.ContactDatabase

class ContactsProvider (mContext: Context) {

    companion object {
        private var mDb: ContactDatabase? = null
        private var mCache: Array<Contact>? = null
    }

    private val keyLastInternal = "KEY_LAST_INTERNAL_REFRESH"
    private val mCm = ContactsManager(mContext)
    private val mMainDaoProvider = MainDaoProvider(mContext)
    private val mPref = PreferenceManager.getDefaultSharedPreferences(mContext)

    init {
        if (mDb == null) mDb = Room.databaseBuilder(
            mContext, ContactDatabase::class.java, "contacts"
        ).allowMainThreadQueries().build()
    }

    fun updateAsync() {
        if (System.currentTimeMillis() - mPref.getLong(KEY_LAST_REFRESH, 0) < 10*1000) return
        Thread {
            mPref.edit().putLong(KEY_LAST_REFRESH, System.currentTimeMillis()).apply()
            val loaded = mCm.getContactsList()
            val cached = mDb!!.manager().loadAllSync()
            mDb!!.manager().insertAll(loaded)
            cached.forEach {
                if (it !in loaded) {
                    mDb!!.manager().delete(it)
                }
            }
        }.start()
    }

    fun getSync(): Array<Contact> {
        mCache = mDb!!.manager().loadAllSync()
        return mCache!!
    }

    fun getPaged(key: String) =
        mDb!!.manager().searchPaged("$key%", "% $key%")

    fun getNameOrNull(number: String): String? {
        Thread {
            if (mCache == null || mPref.getLong(KEY_LAST_REFRESH, 0) > mPref.getLong(keyLastInternal, 0)) {
                mPref.edit().putLong(keyLastInternal, System.currentTimeMillis()).apply()
                mCache = getSync()
            }
        }.start()
        return if (mCache == null) {
            mDb!!.manager().findByNumber(number)?.name
        } else {
            mCache!!.firstOrNull { number == it.number }?.name
        }
    }

    fun get(number: String) = mDb!!.manager().findByNumber(number)

    fun getLive(number: String) = mDb!!.manager().getLive(number)
}