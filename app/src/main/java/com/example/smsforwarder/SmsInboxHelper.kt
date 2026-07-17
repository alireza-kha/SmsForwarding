package com.example.smsforwarder

import android.content.Context
import android.provider.Telephony

data class InboxSender(
    val address: String,
    val lastMessageSnippet: String
)

object SmsInboxHelper {

    /** لیست شماره‌های یکتا از پیامک‌های دریافتی، جدیدترین اول */
    fun getRecentSenders(context: Context, limit: Int = 100): List<InboxSender> {
        val result = LinkedHashMap<String, String>()
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        try {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )?.use { cursor ->
                val addressIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                while (cursor.moveToNext() && result.size < limit) {
                    val address = cursor.getString(addressIdx) ?: continue
                    if (!result.containsKey(address)) {
                        val body = cursor.getString(bodyIdx) ?: ""
                        result[address] = body
                    }
                }
            }
        } catch (_: Exception) {
            // اگه مجوز نبود یا خطایی پیش اومد، لیست خالی برمی‌گرده
        }
        return result.map { InboxSender(it.key, it.value) }
    }
}
