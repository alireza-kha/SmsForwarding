package com.example.smsforwarder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smsforwarder.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: RuleAdapter

    private val requiredPermissions = mutableListOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.SEND_SMS
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private val permissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            startForwarderService()
        }
    }

    private val addEditLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) {
        refreshList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = RuleAdapter(
            onToggle = { rule, enabled ->
                RuleStore.setRuleEnabled(this, rule.id, enabled)
            },
            onDelete = { rule ->
                AlertDialog.Builder(this)
                    .setTitle(R.string.delete_rule_title)
                    .setMessage(getString(R.string.delete_rule_message, rule.fromNumber, rule.toNumber))
                    .setPositiveButton(R.string.delete) { _, _ ->
                        RuleStore.deleteRule(this, rule.id)
                        refreshList()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            },
            onEdit = { rule ->
                val intent = Intent(this, AddEditRuleActivity::class.java)
                intent.putExtra(AddEditRuleActivity.EXTRA_RULE_ID, rule.id)
                addEditLauncher.launch(intent)
            }
        )

        binding.recyclerRules.layoutManager = LinearLayoutManager(this)
        binding.recyclerRules.adapter = adapter

        binding.fabAddRule.setOnClickListener {
            addEditLauncher.launch(Intent(this, AddEditRuleActivity::class.java))
        }

        binding.switchMaster.setOnCheckedChangeListener { _, checked ->
            RuleStore.setMasterEnabled(this, checked)
        }

        binding.buttonEnableAll.setOnClickListener {
            RuleStore.setAllEnabled(this, true)
            refreshList()
        }
        binding.buttonDisableAll.setOnClickListener {
            RuleStore.setAllEnabled(this, false)
            refreshList()
        }

        checkAndRequestPermissions()
        requestIgnoreBatteryOptimizations()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        binding.switchMaster.setOnCheckedChangeListener(null)
        binding.switchMaster.isChecked = RuleStore.isMasterEnabled(this)
        binding.switchMaster.setOnCheckedChangeListener { _, checked ->
            RuleStore.setMasterEnabled(this, checked)
        }
        val rules = RuleStore.getAll(this)
        adapter.submitList(rules)
        binding.textEmpty.visibility =
            if (rules.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun checkAndRequestPermissions() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            startForwarderService()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun startForwarderService() {
        startForegroundService(this, Intent(this, ForwarderService::class.java))
    }

    private fun requestIgnoreBatteryOptimizations() {
        val powerManager = getSystemService(PowerManager::class.java)
        val packageName = packageName
        if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
            AlertDialog.Builder(this)
                .setTitle(R.string.battery_dialog_title)
                .setMessage(R.string.battery_dialog_message)
                .setPositiveButton(R.string.ok) { _, _ ->
                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                    } catch (_: Exception) {
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }
}
