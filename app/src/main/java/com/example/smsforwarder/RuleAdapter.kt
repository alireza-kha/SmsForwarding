package com.example.smsforwarder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smsforwarder.databinding.ItemRuleBinding

class RuleAdapter(
    private val onToggle: (Rule, Boolean) -> Unit,
    private val onDelete: (Rule) -> Unit,
    private val onEdit: (Rule) -> Unit
) : RecyclerView.Adapter<RuleAdapter.RuleViewHolder>() {

    private val items = mutableListOf<Rule>()

    fun submitList(newItems: List<Rule>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RuleViewHolder {
        val binding = ItemRuleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RuleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RuleViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class RuleViewHolder(private val binding: ItemRuleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(rule: Rule) {
            val fromLabel = if (rule.fromNumber == "*")
                binding.root.context.getString(R.string.all_numbers) else rule.fromNumber

            binding.textFrom.text = binding.root.context.getString(R.string.rule_from_label, fromLabel)
            binding.textTo.text = binding.root.context.getString(R.string.rule_to_label, rule.toNumber)

            binding.switchEnabled.setOnCheckedChangeListener(null)
            binding.switchEnabled.isChecked = rule.enabled
            binding.switchEnabled.setOnCheckedChangeListener { _, checked ->
                onToggle(rule, checked)
            }

            binding.root.setOnClickListener { onEdit(rule) }
            binding.buttonDelete.setOnClickListener { onDelete(rule) }
        }
    }
}
