package com.bruhascended.organiso.ui.scheduled

import android.content.Context
import android.view.View
import android.view.View.VISIBLE
import android.widget.TextView
import com.bruhascended.core.data.ContactsProvider
import com.bruhascended.core.db.ScheduledMessage
import com.bruhascended.organiso.R
import com.bruhascended.organiso.common.MediaViewHolder

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

class ScheduledViewHolder(
    private val mContext: Context,
    root: View,
) : MediaViewHolder(mContext, root) {

    lateinit var message: ScheduledMessage

    private val timeTextView: TextView = root.findViewById(R.id.time)
    private val statusTextView: TextView = root.findViewById(R.id.status)

    override fun getUid(): Long = message.time
    override fun getDataPath(): String = message.path!!

    override fun hideMedia() {
        super.hideMedia()
        content.setBackgroundResource(R.drawable.bg_message_out)
    }

    fun onBind() {
        hideMedia()

        messageTextView.text = message.text
        timeTextView.text = dtp.getFull(message.time)

        statusTextView.visibility = VISIBLE

        if (message.time > System.currentTimeMillis()) {
            statusTextView.setTextColor(mContext.getColor(R.color.green))
            statusTextView.text = mContext.getString(
                R.string.scheduled_to_sender,
                ContactsProvider(mContext).getNameOrNull(message.cleanAddress)
                    ?: message.cleanAddress
            )
        } else {
            statusTextView.setTextColor(mContext.getColor(R.color.red))
            statusTextView.text = mContext.getString(R.string.failed)
        }

        if (message.path != null) {
            showMedia()
        }
    }
}