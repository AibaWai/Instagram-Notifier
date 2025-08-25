package com.example.instagramnotifier

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast

class NotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationListener"
    }

    private lateinit var configManager: NotificationConfigManager

    override fun onCreate() {
        super.onCreate()
        configManager = NotificationConfigManager.getInstance(this)
        Log.d(TAG, "通知監聽服務已創建")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        sbn?.let { notification ->
            Log.d(TAG, "收到通知來自: ${notification.packageName}")

            // 根據包名判斷平台
            val platform = when (notification.packageName) {
                NotificationConfig.Platform.INSTAGRAM.packageName -> {
                    Log.d(TAG, "✅ 收到 Instagram 通知")
                    NotificationConfig.Platform.INSTAGRAM
                }
                NotificationConfig.Platform.TWITTER.packageName -> {
                    Log.d(TAG, "✅ 收到 X (Twitter) 通知")
                    NotificationConfig.Platform.TWITTER
                }
                else -> {
                    Log.d(TAG, "其他應用通知: ${notification.packageName}")
                    return
                }
            }

            showDebugToast("收到 ${platform.displayName} 通知")
            processNotification(notification, platform)
        }
    }

    private fun showDebugToast(message: String) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "顯示 Toast 失敗", e)
        }
    }

    private fun processNotification(sbn: StatusBarNotification, platform: NotificationConfig.Platform) {
        try {
            val notification = sbn.notification
            val extras = notification.extras

            Log.d(TAG, "=== ${platform.displayName} 通知詳情 ===")

            // 提取通知內容
            val title = extras.getCharSequence("android.title")?.toString() ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val bigText = extras.getCharSequence("android.bigText")?.toString() ?: text
            val subText = extras.getCharSequence("android.subText")?.toString() ?: ""

            Log.d(TAG, "標題: '$title'")
            Log.d(TAG, "文字: '$text'")
            Log.d(TAG, "大文字: '$bigText'")
            Log.d(TAG, "子文字: '$subText'")

            // 提取用戶名和內容
            val (username, content) = when (platform) {
                NotificationConfig.Platform.INSTAGRAM -> extractInstagramContent(title, text, bigText, subText)
                NotificationConfig.Platform.TWITTER -> extractTwitterContent(title, text, bigText, subText)
            }

            Log.d(TAG, "解析用戶名: '$username'")
            Log.d(TAG, "處理後內容: '$content'")

            if (content.isNotEmpty()) {
                // 獲取該平台的所有已啟用配置
                val configs = configManager.getConfigsByPlatform(platform)
                Log.d(TAG, "找到 ${configs.size} 個 ${platform.displayName} 配置")

                if (configs.isEmpty()) {
                    Log.w(TAG, "沒有找到 ${platform.displayName} 的已啟用配置")
                    showDebugToast("沒有 ${platform.displayName} 配置")
                    return
                }

                // 為每個匹配的配置發送通知
                configs.forEach { config ->
                    processConfigNotification(config, username, content)
                }
            } else {
                Log.w(TAG, "${platform.displayName} 通知內容為空，跳過發送")
            }

        } catch (e: Exception) {
            Log.e(TAG, "處理${platform.displayName}通知時發生錯誤", e)
            showDebugToast("${platform.displayName} 通知處理錯誤: ${e.message}")
        }
    }

    private fun processConfigNotification(config: NotificationConfig, username: String, content: String) {
        try {
            Log.d(TAG, "檢查配置: ${config.name}")
            Log.d(TAG, "過濾模式: ${config.filterMode.displayName}")
            Log.d(TAG, "關鍵字: ${config.keywords}")

            // 組合完整內容用於過濾（包含用戶名和內容）
            val fullContent = if (username.isNotEmpty()) {
                "$username $content"
            } else {
                content
            }

            // 檢查過濾條件
            if (!config.matchesFilter(fullContent)) {
                Log.d(TAG, "配置「${config.name}」: 內容不匹配過濾條件")
                return
            }

            Log.d(TAG, "配置「${config.name}」: 過濾條件匹配，準備發送")

            // 檢查 webhook URL
            if (config.webhookUrl.isEmpty()) {
                Log.w(TAG, "配置「${config.name}」: Webhook URL 為空")
                return
            }

            // 構建標題
            val finalTitle = if (config.includeTitle) {
                if (username.isNotEmpty()) {
                    "👤 $username"
                } else {
                    "📸 ${config.platform.displayName}"
                }
            } else {
                ""
            }

            // 發送到 Discord
            sendToDiscord(config, finalTitle, content)

        } catch (e: Exception) {
            Log.e(TAG, "處理配置「${config.name}」時發生錯誤", e)
        }
    }

    private fun extractInstagramContent(title: String, text: String, bigText: String, subText: String): Pair<String, String> {
        val username = extractInstagramUsername(title, text, bigText, subText)

        val content = buildString {
            var mainContent = bigText.ifEmpty { text }

            // 清理內容，移除用戶名重複
            if (username.isNotEmpty()) {
                mainContent = mainContent
                    .replace("$username ", "")
                    .replace("$username\n", "")
                    .replace("$username 張貼了", "張貼了")
                    .replace("$username 的直播視訊開始了", "開始了直播")
                    .replace("$username posted", "posted")
                    .replace("$username is live", "is live")
            }

            append(mainContent.trim())

            if (subText.isNotEmpty() && subText != title && subText != text && subText != bigText) {
                if (isNotEmpty() && !endsWith("\n")) append("\n")
                append(subText)
            }
        }.trim()

        return Pair(username, content)
    }

    private fun extractTwitterContent(title: String, text: String, bigText: String, subText: String): Pair<String, String> {
        val username = extractTwitterUsername(title, text, bigText, subText)

        val content = buildString {
            var mainContent = bigText.ifEmpty { text }

            // 清理內容，移除用戶名重複
            if (username.isNotEmpty()) {
                mainContent = mainContent
                    .replace("@$username ", "")
                    .replace("@$username\n", "")
                    .replace("@$username:", "")
                    .replace("$username ", "")
                    .replace("$username\n", "")
                    .replace("$username:", "")
            }

            append(mainContent.trim())

            if (subText.isNotEmpty() && subText != title && subText != text && subText != bigText) {
                if (isNotEmpty() && !endsWith("\n")) append("\n")
                append(subText)
            }
        }.trim()

        return Pair(username, content)
    }

    private fun extractInstagramUsername(title: String, text: String, bigText: String, subText: String): String {
        val patterns = listOf(
            Regex("^([a-zA-Z0-9_.]+)\\s*張貼了"),
            Regex("^([a-zA-Z0-9_.]+)\\s*的直播視訊開始了"),
            Regex("^([a-zA-Z0-9_.]+)\\s*posted"),
            Regex("^([a-zA-Z0-9_.]+)\\s*is live"),
            Regex("^([a-zA-Z0-9_.]+)\\s+"),
            Regex("^([a-zA-Z0-9_.]{1,30})(?:\\s|$)")
        )

        val textSources = listOf(bigText, text, title, subText)

        for (source in textSources) {
            if (source.isNotEmpty()) {
                for (pattern in patterns) {
                    val match = pattern.find(source.trim())
                    if (match != null && match.groupValues.size > 1) {
                        val username = match.groupValues[1]
                        if (isValidInstagramUsername(username)) {
                            return username
                        }
                    }
                }
            }
        }

        return ""
    }

    private fun extractTwitterUsername(title: String, text: String, bigText: String, subText: String): String {
        Log.d(TAG, "=== Twitter 用戶名提取 ===")
        Log.d(TAG, "Title: '$title'")
        Log.d(TAG, "Text: '$text'")
        Log.d(TAG, "BigText: '$bigText'")

        val patterns = listOf(
            Regex("@([a-zA-Z0-9_]+)\\s*(?:發布了|發佈了|posted|tweeted|說|replied|回覆)"),
            Regex("@([a-zA-Z0-9_]+)\\s*[:：]"),
            Regex("^@([a-zA-Z0-9_]+)(?:\\s|$)"),
            Regex("^([a-zA-Z0-9_]+)\\s*(?:發布了|發佈了|posted|tweeted)"),
            Regex("@([a-zA-Z0-9_]+)"),
            Regex("來自\\s*@?([a-zA-Z0-9_]+)"),
            Regex("From\\s*@?([a-zA-Z0-9_]+)"),
            Regex("[^a-zA-Z0-9_]*@([a-zA-Z0-9_]+)"),
            Regex("([a-zA-Z0-9_]{3,15})")
        )

        val textSources = listOf(title, bigText, text, subText)

        for (source in textSources) {
            if (source.isNotEmpty()) {
                Log.d(TAG, "正在分析: '$source'")

                for ((index, pattern) in patterns.withIndex()) {
                    val matches = pattern.findAll(source)
                    for (match in matches) {
                        if (match.groupValues.size > 1) {
                            val username = match.groupValues[1]
                            Log.d(TAG, "模式 $index 找到潛在用戶名: '$username'")

                            if (isValidTwitterUsername(username)) {
                                Log.d(TAG, "✅ 確認用戶名: '$username'")
                                return username
                            } else {
                                Log.d(TAG, "❌ 無效用戶名: '$username'")
                            }
                        }
                    }
                }
            }
        }

        Log.d(TAG, "❌ 無法提取用戶名")
        return ""
    }

    private fun isValidInstagramUsername(username: String): Boolean {
        return username.matches(Regex("^[a-zA-Z0-9_.]{1,30}$")) &&
                !isCommonWord(username)
    }

    private fun isValidTwitterUsername(username: String): Boolean {
        val isValidFormat = username.matches(Regex("^[a-zA-Z0-9_]{1,15}$"))
        val isNotCommonWord = !isCommonWord(username)
        val isNotTooShort = username.length >= 2

        Log.d(TAG, "用戶名驗證 '$username': 格式=$isValidFormat, 非常見詞=$isNotCommonWord, 長度OK=$isNotTooShort")

        return isValidFormat && isNotCommonWord && isNotTooShort
    }

    private fun isCommonWord(word: String): Boolean {
        val commonWords = setOf(
            "twitter", "post", "tweet", "發布", "分享", "回覆", "轉推",
            "new", "latest", "update", "notification", "通知", "instagram",
            "live", "直播", "視訊", "相片", "photo", "video"
        )
        return commonWords.contains(word.lowercase())
    }

    private fun sendToDiscord(config: NotificationConfig, notificationTitle: String, content: String) {
        Log.d(TAG, "發送通知到配置: ${config.name}")

        Thread {
            try {
                NotificationSender.sendToDiscordWithConfig(
                    config,
                    notificationTitle,
                    content,
                    this@NotificationListener
                )

                Log.d(TAG, "✅ 配置「${config.name}」通知已成功發送到Discord")

                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    showDebugToast("✅ ${config.name} 已發送")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 配置「${config.name}」發送失敗", e)

                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    showDebugToast("❌ ${config.name} 發送失敗")
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