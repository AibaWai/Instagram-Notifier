package com.example.instagramnotifier

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var instagramWebhookEditText: EditText
    private lateinit var twitterWebhookEditText: EditText
    private lateinit var statusTextView: TextView
    private lateinit var enableButton: Button
    private lateinit var testInstagramButton: Button
    private lateinit var testTwitterButton: Button
    private lateinit var settingsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupClickListeners()
        updateStatus()
    }

    private fun initViews() {
        // 確保這些 ID 與 XML 中的 ID 一致
        instagramWebhookEditText = findViewById(R.id.editTextInstagramWebhookUrl)
        twitterWebhookEditText = findViewById(R.id.editTextTwitterWebhookUrl)
        statusTextView = findViewById(R.id.textViewStatus)
        enableButton = findViewById(R.id.buttonEnable)
        testInstagramButton = findViewById(R.id.buttonTestInstagram)
        testTwitterButton = findViewById(R.id.buttonTestTwitter)
        settingsButton = findViewById(R.id.buttonSettings)

        // 載入已保存的webhook URL
        val sharedPrefs = getSharedPreferences("NotifierPrefs", MODE_PRIVATE)
        val savedInstagramUrl = sharedPrefs.getString("instagram_webhook_url", "")
        val savedTwitterUrl = sharedPrefs.getString("twitter_webhook_url", "")
        instagramWebhookEditText.setText(savedInstagramUrl)
        twitterWebhookEditText.setText(savedTwitterUrl)
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

        testInstagramButton.setOnClickListener {
            saveWebhookUrls()
            sendTestNotification("instagram")
        }

        testTwitterButton.setOnClickListener {
            saveWebhookUrls()
            sendTestNotification("twitter")
        }

        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun saveWebhookUrls() {
        val instagramUrl = instagramWebhookEditText.text.toString().trim()
        val twitterUrl = twitterWebhookEditText.text.toString().trim()
        val sharedPrefs = getSharedPreferences("NotifierPrefs", MODE_PRIVATE)
        sharedPrefs.edit()
            .putString("instagram_webhook_url", instagramUrl)
            .putString("twitter_webhook_url", twitterUrl)
            .apply()
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
        if (isNotificationServiceEnabled()) {
            statusTextView.text = getString(R.string.status_enabled)
            enableButton.text = getString(R.string.button_manage_permissions)
        } else {
            statusTextView.text = getString(R.string.status_disabled)
            enableButton.text = getString(R.string.button_enable_notifications)
        }
    }

    private fun sendTestNotification(platform: String) {
        when (platform) {
            "instagram" -> {
                val url = instagramWebhookEditText.text.toString().trim()
                if (url.isEmpty()) {
                    Toast.makeText(this, getString(R.string.error_instagram_webhook_empty), Toast.LENGTH_SHORT).show()
                    return
                }
                NotificationSender.sendToDiscordWithCustomSettings(
                    url,
                    getString(R.string.test_instagram_title),
                    getString(R.string.test_instagram_message),
                    "instagram",
                    this
                )
                Toast.makeText(this, getString(R.string.success_instagram_test_sent), Toast.LENGTH_SHORT).show()
            }
            "twitter" -> {
                val url = twitterWebhookEditText.text.toString().trim()
                if (url.isEmpty()) {
                    Toast.makeText(this, getString(R.string.error_twitter_webhook_empty), Toast.LENGTH_SHORT).show()
                    return
                }
                NotificationSender.sendToDiscordWithCustomSettings(
                    url,
                    getString(R.string.test_twitter_title),
                    getString(R.string.test_twitter_message),
                    "twitter",
                    this
                )
                Toast.makeText(this, getString(R.string.success_twitter_test_sent), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }
}