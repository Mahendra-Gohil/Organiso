package com.bruhascended.core.model

import java.util.*
import kotlin.math.abs

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

fun Boolean.toFloat() = if (this) 1f else 0f

fun Array<Float>.firstMax(): Int {
    var max = 0f
    forEach {
        max = kotlin.math.max(max, it)
    }
    return indexOfFirst { it == max }
}

// removes all instances of regex from text
private fun removeRegex (text: String, regex: Regex) : Pair<String, Float> {
    val many = regex.findAll(text)
    var newText = text
    for (one in many)
        newText = newText.replace(one.groupValues.first().toString(), " ")
    return newText.trim() to (!many.none()).toFloat()
}

fun removeDecimals (message: String): Pair<String, Float> {
    // identifies decimals, time, date and big numbers separated by ','
    val decimal = Regex("\\d*[.:,/\\\\]+\\d+")
    return removeRegex(message, decimal)
}

// removes dates from sms text
fun removeDates (message: String): Pair<String, Float> {
    val date = Regex("(?:\\d{1,2}[-/th|st|nd|rd\\s]*)?" +
            "(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)?[a-z\\s,.]*" +
            "(?:\\d{1,2}[-/th|st|nd|rd)\\s,]*)+(?:\\d{2,4})+", RegexOption.IGNORE_CASE)
    return removeRegex(message, date)
}

// removes large numbers from sms text
fun removeNumbers (message: String): Pair<String, Float> {
    val number = Regex("(?<!\\d)\\d{4,25}(?!\\d)")
    return removeRegex(message, number)
}

// removes lines
fun removeLines(message: String): String {
    return message.replace('\n', ' ').replace('\r', ' ')
}

// trims all urls in sms text down to their domain names
fun trimUrls(message: String): Pair<String, Float> {
    val urlRe = Regex("(https?:\\/\\/(?:www\\.|(?!www))[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\." +
            "[^\\s]{2,}|www\\.[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\.[^\\s]{2,}|https?:\\/\\/" +
            "(?:www\\.|(?!www))[a-zA-Z0-9]+\\.[^\\s]{2,}|www\\.[a-zA-Z0-9]+\\.[^\\s]{2,})", RegexOption.IGNORE_CASE)
    val urls = urlRe.findAll(message)
    var newMessage = message
    for (url in urls) {
        val trimmedUrl =
            url.toString().split("//").last().split("/")[0].split('?')[0]
                .replace("www.", "").split('.')[0]
        newMessage = newMessage.replace(url.toString(), trimmedUrl)
    }
    return newMessage.trim() to (!urls.none()).toFloat()
}

// stem words to root meaning
fun stem(message: String): String {
    var newMessage = Regex("[\\-.']+").replace(message, "")  // no space
    newMessage = Regex("[/,:;_!<>()&^*#@+]+").replace(newMessage, " ") // added space
    return  newMessage.toLowerCase(Locale.ROOT)
}

// changes long time to [0,1] ([day, night])
fun time (date: Long): Float {
    val cl = Calendar.getInstance()
    cl.timeInMillis = date
    val seriesTime = cl.get(Calendar.MINUTE) + cl.get(Calendar.HOUR_OF_DAY)*60
    return abs(abs(seriesTime-60*4)-(60*12)) / 720f
}

fun removeHiddenNumbers (message: String): Pair<String, Float> {
    // identifies decimals, time, date and big numbers separated by ','
    val decimal = Regex("\\d*[Xx*]+\\d+")
    return removeRegex(message, decimal)
}


// removes large numbers from sms text
fun getOtp(message: String): String? {
    val maxSepAllowed = 150
    val sepRegex = Regex("(?<=\\d)[\\s\\-](?=\\d)")
    val content = removeHiddenNumbers(removeDecimals(message).first).first.toLowerCase(Locale.ROOT).replace(sepRegex, "")
    val otpRegex = Regex("\\b\\d{6}\\b")
    val otps = otpRegex.findAll(content).toList()

    if (otps.isEmpty()) {
        return null
    }
    var otp = otps.first().groups.first()!!.value
    var otpIndex = content.indexOf(otp)
    if (otpIndex - content.indexOf(".") == 1) {
        if (otps.size == 1) {
            return null
        } else {
            otp = otps[1].groups.first()!!.value
            otpIndex = content.indexOf(otp)
        }
    }

    val keys = arrayOf(
        "otp", "code", "key", "pin", "one time password", "verify"
    )

    val numberPairs = arrayOf(
        "verify", "verification", "registration"
    )

    content.apply {
        keys.forEach {
            val ind = Regex("\\b$it\\b", RegexOption.IGNORE_CASE)
                .find(this)?.range?.start ?: -1
            if (ind != -1 && abs(otpIndex-ind) < maxSepAllowed) {
                return otp
            }
        }
        val numberIndex = indexOf("number")
        if (numberIndex != -1) {
            numberPairs.forEach {
                val ind = indexOf(it)
                if (ind != 1 &&
                    (abs(otpIndex-ind) < maxSepAllowed ||
                        abs(otpIndex-numberIndex) < maxSepAllowed)
                ) {
                    return otp
                }
            }
        }
    }
    return null
}