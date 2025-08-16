package com.example.instagramnotifier

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "InstagramNotifier"
        private const val INSTAGRAM_PACKAGE = "com.instagram.android"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        sbn?.let { notification ->
            // 只處理Instagram的通知
            if (notification.packageName == INSTAGRAM_PACKAGE) {
                processInstagramNotification(notification)
            }
        }
    }

    private fun processInstagramNotification(sbn: StatusBarNotification) {
        try {
            val notification = sbn.notification
            val extras = notification.extras

            // 提取通知內容
            val title = extras.getCharSequence("android.title")?.toString() ?: "Instagram"
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val bigText = extras.getCharSequence("android.bigText")?.toString() ?: text
            val subText = extras.getCharSequence("android.subText")?.toString() ?: ""

            // 組合完整訊息
            val fullMessage = buildString {
                if (title.isNotEmpty()) append("**$title**\n")
                if (bigText.isNotEmpty()) append("$bigText\n")
                if (subText.isNotEmpty()) append("_${subText}_\n")
            }.trim()

            Log.d(TAG, "Instagram通知: $fullMessage")

            // 發送到Discord
            sendToDiscord(title, fullMessage)

        } catch (e: Exception) {
            Log.e(TAG, "處理Instagram通知時發生錯誤", e)
        }
    }

    private fun sendToDiscord(title: String, content: String) {
        val sharedPrefs = getSharedPreferences("NotifierPrefs", MODE_PRIVATE)
        val webhookUrl = sharedPrefs.getString("webhook_url", "")

        if (webhookUrl.isNullOrEmpty()) {
            Log.w(TAG, "Discord Webhook URL 未設置")
            return
        }

        // 使用背景執行緒發送請求
        Thread {
            try {
                NotificationSender.sendToDiscord(
                    webhookUrl,
                    "📸 Instagram 通知",
                    content,
                    title
                )
                Log.d(TAG, "Instagram通知已轉發到Discord")
            } catch (e: Exception) {
                Log.e(TAG, "發送到Discord失敗", e)
            }
        }.start()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // 可以在這裡處理通知被移除的情況
    }
}