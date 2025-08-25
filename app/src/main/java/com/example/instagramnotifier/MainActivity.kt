package com.example.instagramnotifier

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusTextView: TextView
    private lateinit var enableButton: Button
    private lateinit var manageConfigsButton: Button
    private lateinit var settingsButton: Button
    private lateinit var configManager: NotificationConfigManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_new)

        configManager = NotificationConfigManager.getInstance(this)

        initViews()
        setupClickListeners()
        updateStatus()
    }

    private fun initViews() {
        statusTextView = findViewById(R.id.textViewStatus)
        enableButton = findViewById(R.id.buttonEnable)
        manageConfigsButton = findViewById(R.id.buttonManageConfigs)
        settingsButton = findViewById(R.id.buttonSettings)
    }

    private fun setupClickListeners() {
        enableButton.setOnClickListener {
            if (isNotificationServiceEnabled()) {
                // 已啟用，跳到設定頁面
                requestNotificationPermission()
            } else {
                // 未啟用，請求權限
                requestNotificationPermission()
            }
        }

        manageConfigsButton.setOnClickListener {
            val intent = Intent(this, ConfigListActivity::class.java)
            startActivity(intent)
        }

        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val packageName = packageName
        val flat = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        return flat?.contains(packageName) == true
    }

    private fun requestNotificationPermission() {
        try {
            // 兼容 API 級別 21+
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            } else {
                Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            }
            startActivity(intent)
            Toast.makeText(this, getString(R.string.permission_request_message), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            // 如果無法打開設定頁面，提供手動指引
            Toast.makeText(this, getString(R.string.permission_manual_guide), Toast.LENGTH_LONG).show()
        }
    }

    private fun updateStatus() {
        val enabledConfigs = configManager.getEnabledConfigs()
        val totalConfigs = configManager.configs.size

        val statusText = buildString {
            if (isNotificationServiceEnabled()) {
                append("✅ 通知監聽已啟用\n")
                if (enabledConfigs.isNotEmpty()) {
                    append("已啟用 ${enabledConfigs.size}/${totalConfigs} 個配置\n")

                    // 按平台分組顯示
                    val instagramConfigs = enabledConfigs.filter { it.platform == NotificationConfig.Platform.INSTAGRAM }
                    val twitterConfigs = enabledConfigs.filter { it.platform == NotificationConfig.Platform.TWITTER }

                    if (instagramConfigs.isNotEmpty()) {
                        append("📸 Instagram: ${instagramConfigs.size} 個配置\n")
                    }
                    if (twitterConfigs.isNotEmpty()) {
                        append("🐦 X (Twitter): ${twitterConfigs.size} 個配置\n")
                    }

                    append("正在監聽通知...")
                } else {
                    if (totalConfigs > 0) {
                        append("⚠️ 所有配置都已禁用\n點擊「管理配置」啟用")
                    } else {
                        append("⚠️ 尚未設置任何配置\n點擊「管理配置」開始設置")
                    }
                }
            } else {
                append("❌ 需要通知監聽權限\n點擊下方按鈕開啟權限")
            }
        }

        statusTextView.text = statusText

        if (isNotificationServiceEnabled()) {
            enableButton.text = getString(R.string.button_manage_permissions)
        } else {
            enableButton.text = getString(R.string.button_enable_notifications)
        }

        // 更新管理配置按鈕文字
        manageConfigsButton.text = if (totalConfigs == 0) {
            getString(R.string.add_config_button)
        } else {
            getString(R.string.manage_configs_button, totalConfigs)
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }
}