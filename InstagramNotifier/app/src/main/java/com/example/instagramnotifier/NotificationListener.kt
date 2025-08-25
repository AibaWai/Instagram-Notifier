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
            val title = extras.getCharSequence("android.title")?.toString() ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val bigText = extras.getCharSequence("android.bigText")?.toString() ?: text
            val subText = extras.getCharSequence("android.subText")?.toString() ?: ""

            Log.d(TAG, "標題: $title")
            Log.d(TAG, "文字: $text")
            Log.d(TAG, "大文字: $bigText")
            Log.d(TAG, "子文字: $subText")

            // 提取用戶名
            val username = extractInstagramUsername(title, text, bigText, subText)
            Log.d(TAG, "解析用戶名: $username")

            // 組合內容 - 不包含用戶名，因為用戶名會作為標題
            val content = buildString {
                // 移除用戶名相關的部分，只保留實際內容
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

            Log.d(TAG, "處理後內容: $content")

            if (content.isNotEmpty()) {
                // 使用用戶名作為標題，如果沒有用戶名則使用預設標題
                val finalTitle = if (username.isNotEmpty()) {
                    "👤 $username"
                } else {
                    "📸 Instagram"
                }

                // 發送到Discord
                sendToDiscord("instagram", finalTitle, content)
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
            val title = extras.getCharSequence("android.title")?.toString() ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val bigText = extras.getCharSequence("android.bigText")?.toString() ?: text
            val subText = extras.getCharSequence("android.subText")?.toString() ?: ""

            Log.d(TAG, "標題: '$title'")
            Log.d(TAG, "文字: '$text'")
            Log.d(TAG, "大文字: '$bigText'")
            Log.d(TAG, "子文字: '$subText'")

            // 提取用戶名 - 包含 subText
            val username = extractTwitterUsername(title, text, bigText, subText)
            Log.d(TAG, "最終解析用戶名: '$username'")

            // 組合內容
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

            Log.d(TAG, "處理後內容: '$content'")

            if (content.isNotEmpty()) {
                // 使用用戶名作為標題
                val finalTitle = if (username.isNotEmpty()) {
                    "👤 @$username"
                } else {
                    "🐦 X (Twitter)"
                }

                // 發送到Discord
                sendToDiscord("twitter", finalTitle, content)
            } else {
                Log.w(TAG, "X (Twitter) 通知內容為空，跳過發送")
            }

        } catch (e: Exception) {
            Log.e(TAG, "處理X (Twitter)通知時發生錯誤", e)
            showDebugToast("X (Twitter) 通知處理錯誤: ${e.message}")
        }
    }

    private fun extractInstagramUsername(title: String, text: String, bigText: String, subText: String): String {
        // Instagram 用戶名提取模式
        val patterns = listOf(
            // "username 張貼了"
            Regex("^([a-zA-Z0-9_.]+)\\s*張貼了"),
            // "username 的直播視訊開始了"
            Regex("^([a-zA-Z0-9_.]+)\\s*的直播視訊開始了"),
            // "username posted"
            Regex("^([a-zA-Z0-9_.]+)\\s*posted"),
            // "username is live"
            Regex("^([a-zA-Z0-9_.]+)\\s*is live"),
            // "username "開頭的
            Regex("^([a-zA-Z0-9_.]+)\\s+"),
            // 任何看起來像Instagram用戶名的
            Regex("^([a-zA-Z0-9_.]{1,30})(?:\\s|$)")
        )

        // 按優先順序嘗試各種文字來源
        val textSources = listOf(bigText, text, title, subText)

        for (source in textSources) {
            if (source.isNotEmpty()) {
                for (pattern in patterns) {
                    val match = pattern.find(source.trim())
                    if (match != null && match.groupValues.size > 1) {
                        val username = match.groupValues[1]
                        // 驗證用戶名格式
                        if (isValidInstagramUsername(username)) {
                            return username
                        }
                    }
                }
            }
        }

        return ""
    }

    private fun extractTwitterUsername(title: String, text: String, bigText: String, subText: String = ""): String {
        // 詳細記錄所有內容以幫助除錯
        Log.d(TAG, "=== Twitter 用戶名提取 ===")
        Log.d(TAG, "Title: '$title'")
        Log.d(TAG, "Text: '$text'")
        Log.d(TAG, "BigText: '$bigText'")

        // Twitter 用戶名提取模式 - 更全面的模式
        val patterns = listOf(
            // "@username 發布了新推文" 等各種中文格式
            Regex("@([a-zA-Z0-9_]+)\\s*(?:發布了|發佈了|posted|tweeted|說|replied|回覆)"),
            // "@username:" 格式
            Regex("@([a-zA-Z0-9_]+)\\s*[:：]"),
            // "@username" 在開頭，後面跟空格或換行
            Regex("^@([a-zA-Z0-9_]+)(?:\\s|$)"),
            // "username 發布了"（沒有@）
            Regex("^([a-zA-Z0-9_]+)\\s*(?:發布了|發佈了|posted|tweeted)"),
            // 從內容中提取 @username（任何位置）
            Regex("@([a-zA-Z0-9_]+)"),
            // 其他可能的格式
            Regex("來自\\s*@?([a-zA-Z0-9_]+)"),
            Regex("From\\s*@?([a-zA-Z0-9_]+)"),
            // 處理 emoji 或特殊字符後的用戶名
            Regex("[^a-zA-Z0-9_]*@([a-zA-Z0-9_]+)"),
            // 如果是純 emoji，嘗試從其他地方提取
            Regex("([a-zA-Z0-9_]{3,15})") // 最後嘗試：合理長度的用戶名
        )

        // 按優先順序嘗試各種文字來源
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

                            // 驗證用戶名格式
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
        // Instagram 用戶名規則：1-30個字符，只能包含字母、數字、下劃線和點
        return username.matches(Regex("^[a-zA-Z0-9_.]{1,30}$")) &&
                !isCommonWord(username)
    }

    private fun isValidTwitterUsername(username: String): Boolean {
        // Twitter 用戶名規則：1-15個字符，只能包含字母、數字和下劃線
        val isValidFormat = username.matches(Regex("^[a-zA-Z0-9_]{1,15}$"))
        val isNotCommonWord = !isCommonWord(username)
        val isNotTooShort = username.length >= 2 // 避免單字符誤判

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
                Log.d(TAG, "開始發送到 Discord...")

                // 使用新的自定義發送功能
                NotificationSender.sendToDiscordWithCustomSettings(
                    webhookUrl,
                    notificationTitle, // 這裡已經包含用戶名
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