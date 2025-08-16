package com.example.instagramnotifier

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationListener"
        private const val INSTAGRAM_PACKAGE = "com.instagram.android"
        private const val TWITTER_PACKAGE = "com.twitter.android"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        sbn?.let { notification ->
            when (notification.packageName) {
                INSTAGRAM_PACKAGE -> {
                    Log.d(TAG, "收到 Instagram 通知")
                    processInstagramNotification(notification)
                }
                TWITTER_PACKAGE -> {
                    Log.d(TAG, "收到 X (Twitter) 通知")
                    processTwitterNotification(notification)
                }
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
            sendToDiscord("instagram", title, fullMessage)

        } catch (e: Exception) {
            Log.e(TAG, "處理Instagram通知時發生錯誤", e)
        }
    }

    private fun processTwitterNotification(sbn: StatusBarNotification) {
        try {
            val notification = sbn.notification
            val extras = notification.extras

            // 提取通知內容
            val title = extras.getCharSequence("android.title")?.toString() ?: "X (Twitter)"
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val bigText = extras.getCharSequence("android.bigText")?.toString() ?: text
            val subText = extras.getCharSequence("android.subText")?.toString() ?: ""

            // 嘗試解析用戶名
            val username = extractTwitterUsername(title, text, bigText)

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

            Log.d(TAG, "X (Twitter) 通知: 用戶=$username, 標題=$title, 內容=$fullMessage")

            // 發送到Discord
            sendToDiscord("twitter", title, fullMessage)

        } catch (e: Exception) {
            Log.e(TAG, "處理X (Twitter)通知時發生錯誤", e)
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

        if (webhookUrl.isNullOrEmpty()) {
            Log.w(TAG, "$platform Discord Webhook URL 未設置")
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

                // 使用新的自定義發送功能
                NotificationSender.sendToDiscordWithCustomSettings(
                    webhookUrl,
                    platformTitle,
                    content,
                    platform,
                    this@NotificationListener
                )
                Log.d(TAG, "$platform 通知已轉發到Discord: $notificationTitle")
            } catch (e: Exception) {
                Log.e(TAG, "發送 $platform 通知到Discord失敗", e)
            }
        }.start()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // 可以在這裡處理通知被移除的情況
    }
}