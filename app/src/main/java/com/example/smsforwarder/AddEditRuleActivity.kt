package com.example.smsforwarder

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smsforwarder.databinding.ActivityAddEditRuleBinding

class AddEditRuleActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RULE_ID = "extra_rule_id"
    }

    private lateinit var binding: ActivityAddEditRuleBinding
    private var editingRuleId: Long? = null

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
    }

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
