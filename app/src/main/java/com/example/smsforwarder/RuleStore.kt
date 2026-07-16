package com.example.smsforwarder

import android.content.Context
import org.json.JSONArray

/**
 * ذخیره و بازیابی رول‌ها. از SharedPreferences استفاده می‌کنه تا
 * رول‌ها بعد از بستن یا ری‌استارت شدن برنامه هم باقی بمونن.
 */
object RuleStore {

    private const val PREFS = "sms_forwarder_prefs"
    private const val KEY_RULES = "rules_json"
    private const val KEY_MASTER_ENABLED = "master_enabled"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun getAll(context: Context): MutableList<Rule> {
        val raw = prefs(context).getString(KEY_RULES, null) ?: return mutableListOf()
        val arr = JSONArray(raw)
        val list = mutableListOf<Rule>()
        for (i in 0 until arr.length()) {
            list.add(Rule.fromJson(arr.getJSONObject(i)))
        }
        return list
    }

    @Synchronized
    private fun saveAll(context: Context, rules: List<Rule>) {
        val arr = JSONArray()
        rules.forEach { arr.put(it.toJson()) }
        prefs(context).edit().putString(KEY_RULES, arr.toString()).apply()
    }

    @Synchronized
    fun addRule(context: Context, fromNumber: String, toNumber: String): Rule {
        val rules = getAll(context)
        val newId = (rules.maxOfOrNull { it.id } ?: 0L) + 1
        val rule = Rule(newId, fromNumber, toNumber, true)
        rules.add(rule)
        saveAll(context, rules)
        return rule
    }

    @Synchronized
    fun updateRule(context: Context, rule: Rule) {
        val rules = getAll(context)
        val idx = rules.indexOfFirst { it.id == rule.id }
        if (idx >= 0) {
            rules[idx] = rule
            saveAll(context, rules)
        }
    }

    @Synchronized
    fun deleteRule(context: Context, id: Long) {
        val rules = getAll(context)
        rules.removeAll { it.id == id }
        saveAll(context, rules)
    }

    @Synchronized
    fun setRuleEnabled(context: Context, id: Long, enabled: Boolean) {
        val rules = getAll(context)
        rules.find { it.id == id }?.let {
            it.enabled = enabled
            saveAll(context, rules)
        }
    }

    @Synchronized
    fun setAllEnabled(context: Context, enabled: Boolean) {
        val rules = getAll(context)
        rules.forEach { it.enabled = enabled }
        saveAll(context, rules)
    }

    fun isMasterEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MASTER_ENABLED, true)

    fun setMasterEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_MASTER_ENABLED, enabled).apply()
    }
}
