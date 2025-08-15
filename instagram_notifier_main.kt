package com.example.instagramnotifier

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    private lateinit var webhookUrlEditText: EditText
    private lateinit var statusTextView: TextView
    private lateinit var enableButton: Button
    private lateinit var testButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        setupClickListeners()
        updateStatus()
    }
    
    private fun initViews() {
        webhookUrlEditText = findViewById(R.id.editTextWebhookUrl)
        statusTextView = findViewById(R.id.textViewStatus)
        enableButton = findViewById(R.id.buttonEnable)
        testButton = findViewById(R.id.buttonTest)
        
        // 載入已保存的webhook URL
        val sharedPrefs = getSharedPreferences("NotifierPrefs", MODE_PRIVATE)
        val savedUrl = sharedPrefs.getString("webhook_url", "")
        webhookUrlEditText.setText(savedUrl)
    }
    
    private fun setupClickListeners() {
        enableButton.setOnClickListener {
            if (isNotificationServiceEnabled()) {
                // 已啟用，跳到設定頁面
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            } else {
                // 未啟用，請求權限
                requestNotificationPermission()
            }
        }
        
        testButton.setOnClickListener {
            saveWebhookUrl()
            sendTestNotification()
        }
    }
    
    private fun saveWebhookUrl() {
        val url = webhookUrlEditText.text.toString().trim()
        val sharedPrefs = getSharedPreferences("NotifierPrefs", MODE_PRIVATE)
        sharedPrefs.edit().putString("webhook_url", url).apply()
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
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
        Toast.makeText(this, "請開啟 Instagram通知轉發器 的通知存取權限", Toast.LENGTH_LONG).show()
    }
    
    private fun updateStatus() {
        if (isNotificationServiceEnabled()) {
            statusTextView.text = "✅ 通知監聽已啟用\n正在監聽Instagram通知..."
            enableButton.text = "管理權限"
        } else {
            statusTextView.text = "❌ 需要通知監聽權限\n點擊下方按鈕開啟權限"
            enableButton.text = "啟用通知監聽"
        }
    }
    
    private fun sendTestNotification() {
        val url = webhookUrlEditText.text.toString().trim()
        if (url.isEmpty()) {
            Toast.makeText(this, "請輸入Discord Webhook URL", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 發送測試訊息
        NotificationSender.sendToDiscord(
            url,
            "📱 測試訊息",
            "Instagram通知轉發器已成功設置！",
            "測試"
        )
        Toast.makeText(this, "測試訊息已發送", Toast.LENGTH_SHORT).show()
    }
    
    override fun onResume() {
        super.onResume()
        updateStatus()
    }
}