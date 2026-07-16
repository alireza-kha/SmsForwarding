package com.example.smsforwarder

import org.json.JSONObject

/**
 * یک رول فوروارد پیامک.
 * fromNumber = "*" یعنی همه‌ی شماره‌ها (فوروارد همه پیامک‌ها)
 */
data class Rule(
    val id: Long,
    var fromNumber: String,
    var toNumber: String,
    var enabled: Boolean
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("from", fromNumber)
        put("to", toNumber)
        put("enabled", enabled)
    }

    companion object {
        fun fromJson(o: JSONObject): Rule = Rule(
            id = o.getLong("id"),
            fromNumber = o.getString("from"),
            toNumber = o.getString("to"),
            enabled = o.getBoolean("enabled")
        )
    }
}
