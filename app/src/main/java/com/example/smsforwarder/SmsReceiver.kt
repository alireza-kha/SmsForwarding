package com.example.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        if (!RuleStore.isMasterEnabled(context)) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // پیام‌های چندبخشی از یک فرستنده رو با هم ترکیب می‌کنیم
        val sender = messages[0].originatingAddress ?: return
        val fullBody = messages.joinToString(separator = "") { it.messageBody ?: "" }

        val rules = RuleStore.getAll(context)
        val matching = rules.filter { rule ->
            rule.enabled && (rule.fromNumber == "*" || numbersMatch(rule.fromNumber, sender))
        }

        if (matching.isEmpty()) return

        matching.forEach { rule ->
            forwardSms(context, rule.toNumber, sender, fullBody)
        }
    }

    /** مقایسه شماره‌ها با نادیده گرفتن فاصله، کد کشور و صفر ابتدایی */
    private fun numbersMatch(ruleNumber: String, incoming: String): Boolean {
        fun normalize(n: String) = n.replace(" ", "").replace("-", "").takeLast(10)
        return normalize(ruleNumber) == normalize(incoming)
    }

    private fun forwardSms(context: Context, toNumber: String, originalSender: String, body: String) {
        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
                ?: SmsManager.getDefault()
            val text = context.getString(R.string.forward_template, originalSender, body)
            val parts = smsManager.divideMessage(text)
            smsManager.sendMultipartTextMessage(toNumber, null, parts, null, null)
        } catch (e: Exception) {
            Log.e("SmsReceiver", "خطا در ارسال پیامک فوروارد شده", e)
        }
    }
}
