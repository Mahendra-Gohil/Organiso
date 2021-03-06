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

package com.bruhascended.organiso.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.bruhascended.organiso.R

class InfoFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.info_preferences, rootKey)

        val githubPref: Preference = findPreference("github")!!
        val websitePref: Preference = findPreference("website")!!
        val licensePref: Preference = findPreference("license")!!
        val termsPref: Preference = findPreference("tnc")!!
        val privacyPref: Preference = findPreference("privacy")!!
        val bugPref: Preference = findPreference("report_bug")!!

        licensePref.fragment = LicenseFragment::class.qualifiedName
        termsPref.fragment = TermsAndConditionsFragment::class.qualifiedName
        privacyPref.fragment = PrivacyFragment::class.qualifiedName

        githubPref.setOnPreferenceClickListener {
            val link = Uri.parse("https://github.com/ChiragKalra/Organiso")
            requireActivity().startActivity(Intent(Intent.ACTION_VIEW, link))
            false
        }
        websitePref.setOnPreferenceClickListener {
            val link = Uri.parse("https://organiso.web.app/")
            requireActivity().startActivity(Intent(Intent.ACTION_VIEW, link))
            false
        }
        bugPref.setOnPreferenceClickListener {
            requireActivity().startActivity(
                Intent(
                    requireActivity(),
                    BugReportActivity::class.java
                )
            )
            false
        }

    }
}