package com.example.instagramnotifier

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ConfigListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var configAdapter: NotificationConfigAdapter
    private lateinit var configManager: NotificationConfigManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config_list)

        supportActionBar?.title = "通知配置管理"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        configManager = NotificationConfigManager.getInstance(this)

        initViews()
        setupRecyclerView()
        loadConfigs()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewConfigs)
    }

    private fun setupRecyclerView() {
        configAdapter = NotificationConfigAdapter(
            onEditClick = { config ->
                editConfig(config)
            },
            onDeleteClick = { config ->
                showDeleteConfirmDialog(config)
            },
            onToggleClick = { config ->
                configManager.toggleConfigEnabled(config.id)
                loadConfigs()
                Toast.makeText(this,
                    if (config.isEnabled) getString(R.string.config_disabled, config.name)
                    else getString(R.string.config_enabled, config.name),
                    Toast.LENGTH_SHORT
                ).show()
            },
            onTestClick = { config ->
                testConfig(config)
            },
            onDuplicateClick = { config ->
                duplicateConfig(config)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = configAdapter
    }

    private fun loadConfigs() {
        configAdapter.submitList(configManager.configs.toList())
    }

    private fun editConfig(config: NotificationConfig) {
        val intent = Intent(this, ConfigEditActivity::class.java)
        intent.putExtra("config_id", config.id)
        startActivityForResult(intent, REQUEST_EDIT_CONFIG)
    }

    private fun addConfig() {
        val intent = Intent(this, ConfigEditActivity::class.java)
        startActivityForResult(intent, REQUEST_ADD_CONFIG)
    }

    private fun showDeleteConfirmDialog(config: NotificationConfig) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_config_title))
            .setMessage(getString(R.string.delete_config_message, config.name))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                configManager.deleteConfig(config.id)
                loadConfigs()
                Toast.makeText(this, getString(R.string.config_deleted, config.name), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun testConfig(config: NotificationConfig) {
        if (config.webhookUrl.isEmpty()) {
            Toast.makeText(this, getString(R.string.webhook_url_required), Toast.LENGTH_SHORT).show()
            return
        }

        configManager.testConfig(config, this)
        Toast.makeText(this, getString(R.string.test_message_sent), Toast.LENGTH_SHORT).show()
    }

    private fun duplicateConfig(config: NotificationConfig) {
        val duplicatedConfig = configManager.duplicateConfig(config.id)
        if (duplicatedConfig != null) {
            loadConfigs()
            Toast.makeText(this, getString(R.string.config_duplicated), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.config_list_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_config -> {
                addConfig()
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == REQUEST_ADD_CONFIG || requestCode == REQUEST_EDIT_CONFIG) && resultCode == RESULT_OK) {
            loadConfigs()
        }
    }

    companion object {
        private const val REQUEST_ADD_CONFIG = 1001
        private const val REQUEST_EDIT_CONFIG = 1002
    }
}