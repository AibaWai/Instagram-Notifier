package com.example.instagramnotifier

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast

class NotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationListener"
        private const val INSTAGRAM_PACKAGE = "com.instagram.android"
        private const val TWITTER_PACKAGE = "com.twitter.android"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        sbn?.let { notification ->
            // 記錄所有收到的通知，用於除錯
            Log.d(TAG, "收到通知來自: ${notification.packageName}")

            when (notification.packageName) {
                INSTAGRAM_PACKAGE -> {
                    Log.d(TAG, "✅ 收到 Instagram 通知")
                    showDebugToast("收到 Instagram 通知")
                    processInstagramNotification(notification)
                }
                TWITTER_PACKAGE -> {
                    Log.d(TAG, "✅ 收到 X (Twitter) 通知")
                    showDebugToast("收到 X (Twitter) 通知")
                    processTwitterNotification(notification)
                }
                else -> {
                    // 記錄其他應用的通知以幫助除錯
                    Log.d(TAG, "其他應用通知: ${notification.packageName}")
                }
            }
        }
    }

    private fun showDebugToast(message: String) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "顯示 Toast 失敗", e)
        }
    }

    private fun processInstagramNotification(sbn: StatusBarNotification) {
        try {
            val notification = sbn.notification
            val extras = notification.extras

            // 詳細記錄通知內容
            Log.d(TAG, "=== Instagram 通知詳情 ===")

            // 提取通知內容
            val title = extras.getCharSequence("android.title")?.toString() ?: "Instagram"
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val bigText = extras.getCharSequence("android.bigText")?.toString() ?: text
            val subText = extras.getCharSequence("android.subText")?.toString() ?: ""

            Log.d(TAG, "標題: $title")
            Log.d(TAG, "文字: $text")
            Log.d(TAG, "大文字: $bigText")
            Log.d(TAG, "子文字: $subText")

            // 組合完整訊息
            val fullMessage = buildString {
                if (title.isNotEmpty()) append("**$title**\n")
                if (bigText.isNotEmpty()) append("$bigText\n")
                if (subText.isNotEmpty()) append("_${subText}_\n")
            }.trim()

            Log.d(TAG, "完整訊息: $fullMessage")

            if (fullMessage.isNotEmpty()) {
                // 發送到Discord
                sendToDiscord("instagram", title, fullMessage)
            } else {
                Log.w(TAG, "Instagram 通知內容為空，跳過發送")
            }

        } catch (e: Exception) {
            Log.e(TAG, "處理Instagram通知時發生錯誤", e)
            showDebugToast("Instagram 通知處理錯誤: ${e.message}")
        }
    }

    private fun processTwitterNotification(sbn: StatusBarNotification) {
        try {
            val notification = sbn.notification
            val extras = notification.extras

            // 詳細記錄通知內容
            Log.d(TAG, "=== X (Twitter) 通知詳情 ===")

            // 提取通知內容
            val title = extras.getCharSequence("android.title")?.toString() ?: "X (Twitter)"
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val bigText = extras.getCharSequence("android.bigText")?.toString() ?: text
            val subText = extras.getCharSequence("android.subText")?.toString() ?: ""

            Log.d(TAG, "標題: $title")
            Log.d(TAG, "文字: $text")
            Log.d(TAG, "大文字: $bigText")
            Log.d(TAG, "子文字: $subText")

            // 嘗試解析用戶名
            val username = extractTwitterUsername(title, text, bigText)
            Log.d(TAG, "解析用戶名: $username")

            // 原封不動保留完整訊息內容，但加上用戶信息
            val fullMessage = buildString {
                // 如果成功解析到用戶名，先顯示用戶信息
                if (username.isNotEmpty()) {
                    append("👤 **來自: @$username**\n\n")
                }

                if (bigText.isNotEmpty()) {
                    append(bigText)
                } else if (text.isNotEmpty()) {
                    append(text)
                }
                if (subText.isNotEmpty() && subText != title && subText != text && subText != bigText) {
                    if (isNotEmpty() && !endsWith("\n")) append("\n")
                    append(subText)
                }
            }.trim()

            Log.d(TAG, "完整訊息: $fullMessage")

            if (fullMessage.isNotEmpty()) {
                // 發送到Discord
                sendToDiscord("twitter", title, fullMessage)
            } else {
                Log.w(TAG, "X (Twitter) 通知內容為空，跳過發送")
            }

        } catch (e: Exception) {
            Log.e(TAG, "處理X (Twitter)通知時發生錯誤", e)
            showDebugToast("X (Twitter) 通知處理錯誤: ${e.message}")
        }
    }

    private fun extractTwitterUsername(title: String, text: String, bigText: String): String {
        // 常見的 Twitter 通知格式模式
        val patterns = listOf(
            // "@username 發布了新推文"
            Regex("@(\\w+)\\s*發布了"),
            Regex("@(\\w+)\\s*posted"),
            Regex("@(\\w+)\\s*tweeted"),
            // "@username:"
            Regex("@(\\w+)\\s*[:：]"),
            // "username 發布了"
            Regex("^(\\w+)\\s*發布了"),
            Regex("^(\\w+)\\s*posted"),
            // 從標題中提取 @username
            Regex("@(\\w+)"),
            // 其他可能的格式
            Regex("來自\\s*@?(\\w+)"),
            Regex("From\\s*@?(\\w+)")
        )

        // 按優先順序嘗試各種文字來源
        val textSources = listOf(title, text, bigText)

        for (source in textSources) {
            if (source.isNotEmpty()) {
                for (pattern in patterns) {
                    val match = pattern.find(source)
                    if (match != null && match.groupValues.size > 1) {
                        val username = match.groupValues[1]
                        // 過濾掉一些常見的非用戶名詞彙
                        if (username.length > 1 && !isCommonWord(username)) {
                            return username
                        }
                    }
                }
            }
        }

        return ""
    }

    private fun isCommonWord(word: String): Boolean {
        val commonWords = setOf(
            "twitter", "post", "tweet", "發布", "分享", "回覆", "轉推",
            "new", "latest", "update", "notification", "通知"
        )
        return commonWords.contains(word.lowercase())
    }

    private fun sendToDiscord(platform: String, notificationTitle: String, content: String) {
        val sharedPrefs = getSharedPreferences("NotifierPrefs", MODE_PRIVATE)

        val webhookUrl = when (platform) {
            "instagram" -> sharedPrefs.getString("instagram_webhook_url", "")
            "twitter" -> sharedPrefs.getString("twitter_webhook_url", "")
            else -> ""
        }

        Log.d(TAG, "嘗試發送 $platform 通知到 Discord")
        Log.d(TAG, "Webhook URL 是否已設置: ${!webhookUrl.isNullOrEmpty()}")

        if (webhookUrl.isNullOrEmpty()) {
            Log.w(TAG, "$platform Discord Webhook URL 未設置")
            showDebugToast("$platform Webhook URL 未設置")
            return
        }

        // 使用背景執行緒發送請求
        Thread {
            try {
                val platformTitle = when (platform) {
                    "instagram" -> "📸 Instagram 通知"
                    "twitter" -> "🐦 X (Twitter) 通知"
                    else -> "🔔 通知"
                }

                Log.d(TAG, "開始發送到 Discord...")

                // 使用新的自定義發送功能
                NotificationSender.sendToDiscordWithCustomSettings(
                    webhookUrl,
                    platformTitle,
                    content,
                    platform,
                    this@NotificationListener
                )

                Log.d(TAG, "✅ $platform 通知已成功轉發到Discord: $notificationTitle")

                // 在主執行緒顯示成功訊息
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    showDebugToast("✅ $platform 通知已發送")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 發送 $platform 通知到Discord失敗", e)

                // 在主執行緒顯示錯誤訊息
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    showDebugToast("❌ $platform 發送失敗: ${e.message}")
                }
            }
        }.start()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        Log.d(TAG, "通知被移除: ${sbn?.packageName}")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "✅ 通知監聽服務已連接")
        showDebugToast("通知監聽服務已啟動")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "❌ 通知監聽服務已斷開")
        showDebugToast("通知監聽服務已停止")
    }
}