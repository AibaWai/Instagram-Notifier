package com.example.instagramnotifier

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.*

object NotificationSender {

    private const val TAG = "NotificationSender"

    // 新的基於配置的發送方法
    fun sendToDiscordWithConfig(
        config: NotificationConfig,
        title: String,
        content: String,
        context: android.content.Context
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                sendToDiscordInternalWithConfig(config, title, content, context)
            } catch (e: Exception) {
                Log.e(TAG, "發送到Discord時發生錯誤", e)
            }
        }
    }

    @Suppress("unused")
    fun sendToDiscord(webhookUrl: String, title: String, content: String, username: String = "Instagram Bot") {
        // 使用協程在背景執行緒處理網路請求
        CoroutineScope(Dispatchers.IO).launch {
            try {
                sendToDiscordInternal(webhookUrl, title, content, username)
            } catch (e: Exception) {
                Log.e(TAG, "發送到Discord時發生錯誤", e)
            }
        }
    }

    fun sendToDiscordWithCustomSettings(
        webhookUrl: String,
        title: String,
        content: String,
        platform: String,
        context: android.content.Context
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                sendToDiscordInternalWithSettings(webhookUrl, title, content, platform, context)
            } catch (e: Exception) {
                Log.e(TAG, "發送到Discord時發生錯誤", e)
            }
        }
    }

    private suspend fun sendToDiscordInternalWithConfig(
        config: NotificationConfig,
        title: String,
        content: String,
        context: android.content.Context
    ) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL(config.webhookUrl)
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("User-Agent", "Instagram-Notifier/1.0")
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 15000

                // 構建基於配置的Discord消息
                val message = createDiscordMessageFromConfig(config, title, content)

                Log.d(TAG, "發送配置 JSON: ${message.toString(2)}")

                val writer = OutputStreamWriter(connection.outputStream, "UTF-8")
                writer.write(message.toString())
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_NO_CONTENT || responseCode == 200) {
                    Log.d(TAG, "✅ 配置「${config.name}」消息成功發送到Discord")
                } else {
                    Log.w(TAG, "❌ 配置「${config.name}」Discord回應代碼: $responseCode")
                    val errorStream = connection.errorStream
                    if (errorStream != null) {
                        val errorResponse = errorStream.bufferedReader().use { it.readText() }
                        Log.w(TAG, "Discord錯誤回應: $errorResponse")
                    }
                }

                connection.disconnect()

            } catch (e: Exception) {
                Log.e(TAG, "發送配置「${config.name}」到Discord時發生錯誤", e)
                throw e
            }
        }
    }

    private suspend fun sendToDiscordInternal(webhookUrl: String, title: String, content: String, username: String) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL(webhookUrl)
                val connection = url.openConnection() as HttpURLConnection

                // 設置請求方法和標頭
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("User-Agent", "Instagram-Notifier/1.0")
                connection.doOutput = true
                connection.connectTimeout = 10000 // 10秒連線超時
                connection.readTimeout = 15000    // 15秒讀取超時

                // 構建Discord消息JSON
                val message = createDiscordMessage(title, content, username)

                Log.d(TAG, "發送 JSON: ${message.toString(2)}")

                // 發送請求
                val writer = OutputStreamWriter(connection.outputStream, "UTF-8")
                writer.write(message.toString())
                writer.flush()
                writer.close()

                // 檢查回應
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_NO_CONTENT || responseCode == 200) {
                    Log.d(TAG, "✅ 消息成功發送到Discord")
                } else {
                    Log.w(TAG, "❌ Discord回應代碼: $responseCode")
                    // 讀取錯誤回應
                    val errorStream = connection.errorStream
                    if (errorStream != null) {
                        val errorResponse = errorStream.bufferedReader().use { it.readText() }
                        Log.w(TAG, "Discord錯誤回應: $errorResponse")
                    }
                }

                connection.disconnect()

            } catch (e: Exception) {
                Log.e(TAG, "發送到Discord時發生錯誤", e)
                throw e
            }
        }
    }

    private suspend fun sendToDiscordInternalWithSettings(
        webhookUrl: String,
        title: String,
        content: String,
        platform: String,
        context: android.content.Context
    ) {
        withContext(Dispatchers.IO) {
            try {
                val sharedPrefs = context.getSharedPreferences("NotifierPrefs", android.content.Context.MODE_PRIVATE)

                // 獲取自定義設定
                val customBotName = when (platform) {
                    "instagram" -> sharedPrefs.getString("instagram_bot_name", "Instagram Bot") ?: "Instagram Bot"
                    "twitter" -> sharedPrefs.getString("twitter_bot_name", "X Bot") ?: "X Bot"
                    else -> "Bot"
                }

                val customIconUrl = when (platform) {
                    "instagram" -> sharedPrefs.getString("instagram_icon_url", "https://upload.wikimedia.org/wikipedia/commons/a/a5/Instagram_icon.png")
                    "twitter" -> sharedPrefs.getString("twitter_icon_url", "https://upload.wikimedia.org/wikipedia/commons/c/ce/X_logo_2023.svg")
                    else -> "https://upload.wikimedia.org/wikipedia/commons/a/a5/Instagram_icon.png"
                } ?: ""

                val includeTitle = when (platform) {
                    "instagram" -> sharedPrefs.getBoolean("include_instagram_title", true)
                    "twitter" -> sharedPrefs.getBoolean("include_twitter_title", true)
                    else -> true
                }

                val includeTimestamp = sharedPrefs.getBoolean("include_timestamp", true)

                val customColor = when (platform) {
                    "instagram" -> {
                        val colorHex = sharedPrefs.getString("instagram_color", "E4405F") ?: "E4405F"
                        try {
                            Integer.parseInt(colorHex, 16)
                        } catch (e: NumberFormatException) {
                            0xE4405F
                        }
                    }
                    "twitter" -> {
                        val colorHex = sharedPrefs.getString("twitter_color", "1DA1F2") ?: "1DA1F2"
                        try {
                            Integer.parseInt(colorHex, 16)
                        } catch (e: NumberFormatException) {
                            0x1DA1F2
                        }
                    }
                    else -> 0xE4405F
                }

                val url = URL(webhookUrl)
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("User-Agent", "Instagram-Notifier/1.0")
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 15000

                // 構建自定義Discord消息
                val message = createCustomDiscordMessage(
                    if (includeTitle) title else "",
                    content,
                    customBotName,
                    customIconUrl,
                    customColor,
                    includeTimestamp,
                    platform
                )

                Log.d(TAG, "發送自定義 JSON: ${message.toString(2)}")

                val writer = OutputStreamWriter(connection.outputStream, "UTF-8")
                writer.write(message.toString())
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_NO_CONTENT || responseCode == 200) {
                    Log.d(TAG, "✅ 自定義消息成功發送到Discord")
                } else {
                    Log.w(TAG, "❌ Discord回應代碼: $responseCode")
                    // 讀取錯誤回應
                    val errorStream = connection.errorStream
                    if (errorStream != null) {
                        val errorResponse = errorStream.bufferedReader().use { it.readText() }
                        Log.w(TAG, "Discord錯誤回應: $errorResponse")
                    }
                }

                connection.disconnect()

            } catch (e: Exception) {
                Log.e(TAG, "發送到Discord時發生錯誤", e)
                throw e
            }
        }
    }

    private fun createDiscordMessageFromConfig(config: NotificationConfig, title: String, content: String): JSONObject {
        // 使用當地時區的時間 - 兼容 API 21+
        val calendar = Calendar.getInstance()
        val datePart = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        val timePart = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(calendar.time)
        val timestamp = "${datePart}T${timePart}${getTimezoneOffset()}"

        return JSONObject().apply {
            // Bot 名稱
            if (config.botName.isNotBlank()) {
                put("username", config.botName.take(80)) // Discord 限制 80 字符
            }

            // Bot 圖示
            if (config.iconUrl.isNotBlank() && config.iconUrl.startsWith("http")) {
                put("avatar_url", config.iconUrl)
            }

            // 限制內容長度
            val safeContent = content.take(2000) // Discord 限制 2000 字符
            val safeTitle = title.take(256) // Discord embed title 限制 256 字符

            // 使用embed格式以支援更多自定義選項
            val embed = JSONObject().apply {
                if (config.includeTitle && safeTitle.isNotBlank()) {
                    put("title", safeTitle)
                }
                if (safeContent.isNotBlank()) {
                    put("description", safeContent)
                }

                // 確保顏色值有效
                val validColor = try {
                    if (config.color.isNotBlank()) {
                        val colorHex = config.color.removePrefix("#")
                        val colorValue = Integer.parseInt(colorHex, 16)
                        if (colorValue in 0x000000..0xFFFFFF) colorValue else getDefaultColor(config.platform)
                    } else {
                        getDefaultColor(config.platform)
                    }
                } catch (e: NumberFormatException) {
                    getDefaultColor(config.platform)
                }
                put("color", validColor)

                if (config.includeTimestamp) {
                    put("timestamp", timestamp)
                }

                val footer = JSONObject().apply {
                    val footerText = "${config.platform.displayName} 通知轉發器 - ${config.name}"
                    put("text", footerText)
                }
                put("footer", footer)
            }

            put("embeds", JSONArray().put(embed))
        }
    }

    private fun getDefaultColor(platform: NotificationConfig.Platform): Int {
        return when (platform) {
            NotificationConfig.Platform.INSTAGRAM -> 0xE4405F
            NotificationConfig.Platform.TWITTER -> 0x1DA1F2
        }
    }

    private fun createDiscordMessage(title: String, content: String, username: String): JSONObject {
        // 使用當地時區的時間 - 兼容 API 21+
        val calendar = Calendar.getInstance()
        val datePart = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        val timePart = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(calendar.time)
        val timestamp = "${datePart}T${timePart}${getTimezoneOffset()}"

        return JSONObject().apply {
            // 基本訊息設定
            if (username.isNotBlank()) {
                put("username", username.take(80)) // Discord 限制 80 字符
            }

            // 根據username設置不同的頭像
            when {
                username.contains("Instagram", ignoreCase = true) -> {
                    put("avatar_url", "https://upload.wikimedia.org/wikipedia/commons/a/a5/Instagram_icon.png")
                }
                username.contains("Twitter", ignoreCase = true) || username.contains("X", ignoreCase = true) -> {
                    put("avatar_url", "https://upload.wikimedia.org/wikipedia/commons/c/ce/X_logo_2023.svg")
                }
            }

            // 限制內容長度
            val safeContent = content.take(2000) // Discord 限制 2000 字符
            val safeTitle = title.take(256) // Discord embed title 限制 256 字符

            // 使用embed格式處理消息
            val embed = JSONObject().apply {
                if (safeTitle.isNotBlank()) {
                    put("title", safeTitle)
                }
                if (safeContent.isNotBlank()) {
                    put("description", safeContent)
                }

                // 根據來源設置不同顏色
                when {
                    username.contains("Instagram", ignoreCase = true) -> {
                        put("color", 0xE4405F) // Instagram品牌色
                    }
                    username.contains("Twitter", ignoreCase = true) || username.contains("X", ignoreCase = true) -> {
                        put("color", 0x1DA1F2) // Twitter品牌色
                    }
                    else -> {
                        put("color", 0x5865F2) // Discord 藍色
                    }
                }

                put("timestamp", timestamp)

                val footer = JSONObject().apply {
                    if (username.contains("Instagram", ignoreCase = true)) {
                        put("text", "Instagram 通知轉發器")
                    } else if (username.contains("Twitter", ignoreCase = true) || username.contains("X", ignoreCase = true)) {
                        put("text", "X (Twitter) 通知轉發器")
                    } else {
                        put("text", "通知轉發器")
                    }
                }
                put("footer", footer)
            }

            put("embeds", JSONArray().put(embed))
        }
    }

    private fun createCustomDiscordMessage(
        title: String,
        content: String,
        customBotName: String,
        customIconUrl: String,
        customColor: Int,
        includeTimestamp: Boolean,
        platform: String
    ): JSONObject {
        // 使用當地時區的時間 - 兼容 API 21+
        val calendar = Calendar.getInstance()
        val datePart = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        val timePart = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(calendar.time)
        val timestamp = "${datePart}T${timePart}${getTimezoneOffset()}"

        return JSONObject().apply {
            // 基本設定
            if (customBotName.isNotBlank()) {
                put("username", customBotName.take(80)) // Discord 限制 80 字符
            }
            if (customIconUrl.isNotBlank() && customIconUrl.startsWith("http")) {
                put("avatar_url", customIconUrl)
            }

            // 限制內容長度
            val safeContent = content.take(2000) // Discord 限制 2000 字符
            val safeTitle = title.take(256) // Discord embed title 限制 256 字符

            // 使用embed格式以支援更多自定義選項
            val embed = JSONObject().apply {
                if (safeTitle.isNotBlank()) {
                    put("title", safeTitle)
                }
                if (safeContent.isNotBlank()) {
                    put("description", safeContent)
                }

                // 確保顏色值有效
                val validColor = if (customColor in 0x000000..0xFFFFFF) customColor else 0x5865F2
                put("color", validColor)

                if (includeTimestamp) {
                    put("timestamp", timestamp)
                }

                val footer = JSONObject().apply {
                    val footerText = when (platform) {
                        "instagram" -> "Instagram 通知轉發器"
                        "twitter" -> "X (Twitter) 通知轉發器"
                        else -> "通知轉發器"
                    }
                    put("text", footerText)
                }
                put("footer", footer)
            }

            put("embeds", JSONArray().put(embed))
        }
    }

    /**
     * 獲取時區偏移量，兼容 API 21+
     */
    private fun getTimezoneOffset(): String {
        val calendar = Calendar.getInstance()
        val offsetMillis = calendar.timeZone.getOffset(calendar.timeInMillis)
        val offsetHours = offsetMillis / (1000 * 60 * 60)
        val offsetMinutes = (offsetMillis % (1000 * 60 * 60)) / (1000 * 60)

        return "%+03d:%02d".format(Locale.US, offsetHours, kotlin.math.abs(offsetMinutes))
    }
}