package com.bruhascended.core.analytics

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.preference.PreferenceManager
import com.bruhascended.core.BuildConfig
import com.bruhascended.core.db.Conversation
import com.bruhascended.core.db.MessageDbFactory
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

/*
                    Copyright 2020 Chirag Kalra

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

class AnalyticsLogger(
    private val context: Context
) {

    companion object {
        const val PREF_SEND_SPAM = "report_spam"

        const val PATH_SPAM_REPORTS = "spam_reports"
        const val PATH_BUG_REPORTS = "bug_reports"
        const val PATH_TITLE = "title"
        const val PATH_DETAIL = "detail"

        const val EVENT_BUG_REPORTED = "bug_reported"
        const val EVENT_CONVERSATION_ORGANISED = "conversation_organised"
        const val EVENT_MESSAGE_ORGANISED = "message_organised"


        const val PARAM_DEFAULT = "default"
        const val PARAM_BACKGROUND = "background"
        const val PARAM_INIT = "init"

    }

    private val mPref = PreferenceManager.getDefaultSharedPreferences(context)
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)


    fun reportSpam (conversation: Conversation) {
        if (!mPref.getBoolean(PREF_SEND_SPAM, false)) return

        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("${PATH_SPAM_REPORTS}/${conversation.clean}")
        Thread {
            MessageDbFactory(context).of(conversation.clean).apply {
                myRef.setValue(manager().loadAllSync())
                close()
            }
        }.start()
    }

    fun reportBug (title: String, content: String, fileUri: Uri? = null) {
        val rn = System.currentTimeMillis().toString()
        log(EVENT_BUG_REPORTED)

        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("${PATH_BUG_REPORTS}/${rn}")
        myRef.child(PATH_TITLE).setValue(title)
        myRef.child(PATH_DETAIL).setValue(content)

        if (fileUri != null) {
            val storageRef = FirebaseStorage.getInstance().reference
            val riversRef = storageRef.child("${PATH_BUG_REPORTS}/${rn}")
            riversRef.putFile(fileUri)
        }
    }

    fun log (event: String, param: String = PARAM_DEFAULT) {
        if (BuildConfig.DEBUG) return
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, param)
        }
        firebaseAnalytics.logEvent(event, bundle)
    }
}