package com.bruhascended.organiso.ui.conversation

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.*
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.Contacts
import android.view.Gravity
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.RecyclerView
import com.bruhascended.core.analytics.AnalyticsLogger
import com.bruhascended.core.constants.*
import com.bruhascended.core.data.ContactsProvider
import com.bruhascended.core.data.MainDaoProvider
import com.bruhascended.core.db.Conversation
import com.bruhascended.organiso.ConversationActivity
import com.bruhascended.organiso.MainActivity
import com.bruhascended.organiso.R
import com.bruhascended.organiso.ScheduledActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

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

class ConversationMenuOptions(
    private val mContext: Context,
    private val conversation: Conversation,
    private val searchResult: ActivityResultLauncher<Intent>? = null,
    private val cancelCallBack: ((RecyclerView.ViewHolder?) -> Unit)? = null,
    private val itemViewHolder: RecyclerView.ViewHolder? = null,
) {

    private val analyticsLogger: AnalyticsLogger = AnalyticsLogger(mContext)
    private val contactsProvider: ContactsProvider = ContactsProvider(mContext)

    private val colorRes = mContext.resources.getIntArray(R.array.colors)

    private fun getRoundedCornerBitmap(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(
            bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)
        val color = -0xbdbdbe
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)
        val roundPx = bitmap.width.toFloat()
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }

    private fun getSenderIcon(): Icon {
        val bg = ContextCompat.getDrawable(mContext, R.drawable.bg_notification_icon)?.apply {
            setTint(colorRes[conversation.id % colorRes.size])
        }

        val dp = File(mContext.filesDir, conversation.number)
        return when {
            conversation.isBot -> {
                val bot = ContextCompat.getDrawable(mContext, R.drawable.ic_bot)
                val finalDrawable = LayerDrawable(arrayOf(bg, bot))
                finalDrawable.setLayerGravity(1, Gravity.CENTER)
                Icon.createWithBitmap(finalDrawable.toBitmap())
            }
            dp.exists() -> {
                val bm = getRoundedCornerBitmap(BitmapFactory.decodeFile(dp.absolutePath))
                Icon.createWithBitmap(bm)
            }
            else -> {
                val person = ContextCompat.getDrawable(mContext, R.drawable.ic_person)
                val finalDrawable = LayerDrawable(arrayOf(bg, person))
                finalDrawable.setLayerGravity(1, Gravity.CENTER)
                Icon.createWithBitmap(finalDrawable.toBitmap())
            }
        }
    }

    fun onOptionsItemSelected(item: MenuItem? = null, itemId: Int = 0): Boolean {
        mContext.apply {
            val display = contactsProvider.getNameOrNull(conversation.number)
                ?: conversation.number
            when (item?.itemId ?: itemId) {
                R.id.action_block -> {
                    AlertDialog.Builder(mContext)
                        .setTitle(getString(R.string.block_sender_query, display))
                        .setPositiveButton(getString(R.string.block)) { dialog, _ ->
                            analyticsLogger.log("${conversation.label}_to_5")
                            conversation.moveTo(LABEL_BLOCKED, mContext)
                            Toast.makeText(
                                mContext,
                                getString(R.string.sender_blocked),
                                Toast.LENGTH_LONG
                            ).show()
                            dialog.dismiss()
                        }.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                            dialog.dismiss()
                        }.setOnDismissListener {
                            cancelCallBack?.invoke(itemViewHolder)
                        }.create().show()
                }
                R.id.action_report_spam -> {
                    AlertDialog.Builder(mContext)
                        .setTitle(getString(R.string.report_sender_as_spam_query, display))
                        .setPositiveButton(getString(R.string.report)) { dialog, _ ->
                            analyticsLogger.log("${conversation.label}_to_4")
                            conversation.moveTo(LABEL_SPAM, mContext)
                            Toast.makeText(
                                mContext,
                                getString(R.string.reported_spam),
                                Toast.LENGTH_LONG
                            ).show()
                            dialog.dismiss()
                        }.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                            dialog.dismiss()
                        }.setOnDismissListener {
                            cancelCallBack?.invoke(itemViewHolder)
                        }.create().show()
                }
                R.id.action_delete -> {
                    AlertDialog.Builder(mContext)
                        .setTitle(getString(R.string.delete_conversation_query))
                        .setPositiveButton(getString(R.string.delete)) { dialog, _ ->
                            analyticsLogger.log("${conversation.label}_to_-1")
                            conversation.moveTo(LABEL_NONE, mContext)
                            Toast.makeText(
                                mContext,
                                getString(R.string.conversation_deleted),
                                Toast.LENGTH_LONG
                            ).show()
                            dialog.dismiss()
                            if (itemViewHolder == null) {
                                (mContext as AppCompatActivity).finish()
                            }
                        }.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                            dialog.dismiss()
                        }.setOnDismissListener {
                            cancelCallBack?.invoke(itemViewHolder)
                        }.create().show()
                }
                R.id.action_move -> {
                    val choices = ArrayList<String>().apply {
                        val labelArr = mContext.resources.getStringArray(R.array.labels)
                        for (i in 0..3) {
                            if (i != conversation.label) {
                                add(labelArr[i])
                            }
                        }
                    }.toTypedArray()
                    var selection = if (LABEL_PERSONAL == conversation.label)
                        LABEL_IMPORTANT else LABEL_PERSONAL
                    AlertDialog.Builder(mContext)
                        .setTitle(getString(R.string.move_conversation_to))
                        .setSingleChoiceItems(choices, 0) { _, select ->
                            selection = select + if (select >= conversation.label) 1 else 0
                        }.setPositiveButton(getText(R.string.move)) { dialog, _ ->
                            analyticsLogger.log("${conversation.label}_to_$selection")
                            conversation.moveTo(selection, mContext)
                            Toast.makeText(
                                mContext,
                                getString(R.string.conversation_moved),
                                Toast.LENGTH_LONG
                            ).show()
                            dialog.dismiss()
                        }.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                            dialog.dismiss()
                        }.setOnDismissListener {
                            cancelCallBack?.invoke(itemViewHolder)
                        }.create().show()
                }
                R.id.action_search -> {
                    searchResult?.launch(
                        Intent(mContext, MessageSearchActivity::class.java).apply {
                            putExtra(EXTRA_NUMBER, conversation.number)
                        }
                    )
                    this as AppCompatActivity
                    overridePendingTransition(android.R.anim.fade_in, R.anim.hold)
                }
                R.id.action_mute -> {
                    conversation.apply {
                        isMuted = !isMuted
                        MainDaoProvider(mContext).getMainDaos()[label].insert(this)
                        GlobalScope.launch {
                            delay(300)
                            this as AppCompatActivity
                            runOnUiThread {
                                item?.title = if (isMuted)
                                    getString(R.string.unMute) else getString(R.string.mute)
                            }
                        }
                    }
                }
                R.id.action_call -> {
                    val intent = Intent(Intent.ACTION_DIAL)
                    intent.data = Uri.parse("tel:${conversation.number}")
                    startActivity(intent)
                }

                R.id.action_scheduled -> {
                    startActivity(
                        Intent(mContext, ScheduledActivity::class.java)
                            .putExtra(EXTRA_NUMBER, conversation.number)
                    )
                }
                R.id.action_contact -> {
                    val id = ContactsProvider(mContext).get(conversation.number)
                        ?.contactId?.toString()
                    val intent = if (id != null) {
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.withAppendedPath(Contacts.CONTENT_URI,id)
                        )
                    } else {
                        Intent(
                            ContactsContract.Intents.SHOW_OR_CREATE_CONTACT,
                            Uri.parse("tel:" + conversation.number)
                        ).putExtra(
                            ContactsContract.Intents.EXTRA_FORCE_CREATE,
                            true
                        )
                    }
                    startActivity(intent)
                }
                R.id.action_create_shortcut -> {
                    val shortcutManager = getSystemService(ShortcutManager::class.java)!!

                    if (shortcutManager.isRequestPinShortcutSupported) {
                        val pinShortcutInfo =
                            ShortcutInfo.Builder(mContext, conversation.number)
                                .setIcon(getSenderIcon())
                                .setShortLabel(
                                    contactsProvider.getNameOrNull(conversation.number)
                                        ?: conversation.number
                                )
                                .setIntent(
                                    Intent(mContext, ConversationActivity::class.java)
                                        .setAction("android.intent.action.VIEW")
                                        .putExtra(EXTRA_CONVERSATION_JSON, conversation.toString())
                                )
                                .setCategories(setOf("android.shortcut.conversation"))
                                .build()

                        shortcutManager.requestPinShortcut(pinShortcutInfo, null)
                    }
                }
                android.R.id.home -> {
                    this as AppCompatActivity
                    startActivityIfNeeded(
                        Intent(mContext, MainActivity::class.java)
                            .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT), 0
                    )
                    finish()
                }
            }
        }
        return false
    }
}