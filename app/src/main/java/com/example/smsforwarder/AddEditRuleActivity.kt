package com.example.smsforwarder

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.smsforwarder.databinding.ActivityAddEditRuleBinding

class AddEditRuleActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RULE_ID = "extra_rule_id"
    }

    private lateinit var binding: ActivityAddEditRuleBinding
    private var editingRuleId: Long? = null

    /** برای دونستن اینکه دکمه‌ی مخاطبین کدوم فیلد رو باز کرده (from یا to) */
    private var contactPickTarget: EditTargetField = EditTargetField.FROM

    private enum class EditTargetField { FROM, TO }

    // --- Launcher برای انتخاب مخاطب از دفترچه ---
    private val pickContactLauncher = registerForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri != null) {
            val number = readPhoneNumberFromContactUri(uri)
            if (number != null) {
                applyPickedNumber(number)
            } else {
                Toast.makeText(this, R.string.error_to_required, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- Launcher برای درخواست مجوز مخاطبین ---
    private val requestContactsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchContactPicker()
        } else {
            Toast.makeText(this, R.string.permission_needed_contacts, Toast.LENGTH_SHORT).show()
        }
    }

    // --- Launcher برای درخواست مجوز خواندن پیامک ---
    private val requestSmsReadPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showInboxPicker()
        } else {
            Toast.makeText(this, R.string.permission_needed_sms_read, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditRuleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = getString(R.string.add_rule_title)

        val ruleId = intent.getLongExtra(EXTRA_RULE_ID, -1L)
        if (ruleId != -1L) {
            editingRuleId = ruleId
            val rule = RuleStore.getAll(this).find { it.id == ruleId }
            if (rule != null) {
                binding.editFrom.setText(if (rule.fromNumber == "*") "" else rule.fromNumber)
                binding.checkAllNumbers.isChecked = rule.fromNumber == "*"
                binding.editTo.setText(rule.toNumber)
            }
        }

        binding.checkAllNumbers.setOnCheckedChangeListener { _, checked ->
            binding.editFrom.isEnabled = !checked
        }

        binding.buttonSave.setOnClickListener { save() }

        binding.buttonPickContactFrom.setOnClickListener {
            contactPickTarget = EditTargetField.FROM
            requestContactsThenPick()
        }
        binding.buttonPickContactTo.setOnClickListener {
            contactPickTarget = EditTargetField.TO
            requestContactsThenPick()
        }
        binding.buttonPickSmsFrom.setOnClickListener {
            contactPickTarget = EditTargetField.FROM
            requestSmsReadThenPick()
        }
    }

    private fun applyPickedNumber(number: String) {
        val target = if (contactPickTarget == EditTargetField.FROM) binding.editFrom else binding.editTo
        target.setText(number)
        if (contactPickTarget == EditTargetField.FROM) {
            binding.checkAllNumbers.isChecked = false
        }
    }

    // ---------- مخاطبین ----------

    private fun requestContactsThenPick() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            launchContactPicker()
        } else {
            requestContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    private fun launchContactPicker() {
        pickContactLauncher.launch(null)
    }

    private fun readPhoneNumberFromContactUri(uri: android.net.Uri): String? {
        var number: String? = null
        val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val hasPhoneIdx = it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                val idIdx = it.getColumnIndex(ContactsContract.Contacts._ID)
                if (hasPhoneIdx >= 0 && idIdx >= 0 && it.getInt(hasPhoneIdx) > 0) {
                    val contactId = it.getString(idIdx)
                    val phoneCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(contactId),
                        null
                    )
                    phoneCursor?.use { pc ->
                        if (pc.moveToFirst()) {
                            val numIdx = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (numIdx >= 0) {
                                number = pc.getString(numIdx)
                            }
                        }
                    }
                }
            }
        }
        return number?.replace(" ", "")?.replace("-", "")
    }

    // ---------- پیامک‌های دریافتی ----------

    private fun requestSmsReadThenPick() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            showInboxPicker()
        } else {
            requestSmsReadPermissionLauncher.launch(Manifest.permission.READ_SMS)
        }
    }

    private fun showInboxPicker() {
        val senders = SmsInboxHelper.getRecentSenders(this)
        if (senders.isEmpty()) {
            Toast.makeText(this, R.string.inbox_picker_empty, Toast.LENGTH_SHORT).show()
            return
        }
        val labels = senders.map { sender ->
            val snippet = if (sender.lastMessageSnippet.length > 30)
                sender.lastMessageSnippet.take(30) + "…" else sender.lastMessageSnippet
            "${sender.address}\n$snippet"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(R.string.inbox_picker_title)
            .setItems(labels) { _, which ->
                applyPickedNumber(senders[which].address)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ---------- ذخیره ----------

    private fun save() {
        val allNumbers = binding.checkAllNumbers.isChecked
        val from = if (allNumbers) "*" else binding.editFrom.text.toString().trim()
        val to = binding.editTo.text.toString().trim()

        if (!allNumbers && from.isEmpty()) {
            Toast.makeText(this, R.string.error_from_required, Toast.LENGTH_SHORT).show()
            return
        }
        if (to.isEmpty()) {
            Toast.makeText(this, R.string.error_to_required, Toast.LENGTH_SHORT).show()
            return
        }

        val id = editingRuleId
        if (id != null) {
            RuleStore.updateRule(this, Rule(id, from, to, true))
        } else {
            RuleStore.addRule(this, from, to)
        }
        setResult(RESULT_OK)
        finish()
    }
}
