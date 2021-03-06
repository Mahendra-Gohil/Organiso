package com.bruhascended.core.analytics

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.preference.PreferenceManager
import com.bruhascended.core.constants.*
import com.bruhascended.core.BuildConfig
import com.google.firebase.FirebaseApp
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
    private val mPref = PreferenceManager.getDefaultSharedPreferences(context)
    private var firebaseAnalytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(context)

    fun reportBug (title: String, deviceDetails: String, fileUri: Uri? = null) {
        val content = "version=${Build.VERSION.SDK_INT}, " +
                "manufacturer=${Build.MANUFACTURER}, " +
                "model=${Build.MODEL}"
        val rn = System.currentTimeMillis().toString()
        log(EVENT_BUG_REPORTED)

        FirebaseApp.initializeApp(context)
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("${PATH_BUG_REPORTS}/${rn}")
        myRef.child(PATH_TITLE).setValue(title)
        myRef.child(PATH_DETAIL).setValue(content)
        myRef.child(PATH_DEVICE).setValue(deviceDetails)

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