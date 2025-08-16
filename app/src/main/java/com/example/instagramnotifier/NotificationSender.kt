package com.example.instagramnotifier

import android.util.Log
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.*

object NotificationSender {

    private const val TAG = "NotificationSender"

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

    private suspend fun sendToDiscordInternal(webhookUrl: String, title: String, content: String, username: String) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL(webhookUrl)
                val connection = url.openConnection() as HttpURLConnection

                // 設置請求方法和標頭
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("User-Agent", "Instagram-Notifier/1.0")
                connection.doOutput = true
                connection.connectTimeout = 10000 // 10秒連線超時
                connection.readTimeout = 15000    // 15秒讀取超時

                // 構建Discord消息JSON
                val message = createDiscordMessage(title, content, username)

                // 發送請求
                val writer = OutputStreamWriter(connection.outputStream, "UTF-8")
                writer.write(message.toString())
                writer.flush()
                writer.close()

                // 檢查回應
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_NO_CONTENT || responseCode == 200) {
                    Log.d(TAG, "消息成功發送到Discord")
                } else {
                    Log.w(TAG, "Discord回應代碼: $responseCode")
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
                connection.setRequestProperty("Content-Type", "application/json")
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

                val writer = OutputStreamWriter(connection.outputStream, "UTF-8")
                writer.write(message.toString())
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_NO_CONTENT || responseCode == 200) {
                    Log.d(TAG, "消息成功發送到Discord")
                } else {
                    Log.w(TAG, "Discord回應代碼: $responseCode")
                }

                connection.disconnect()

            } catch (e: Exception) {
                Log.e(TAG, "發送到Discord時發生錯誤", e)
                throw e
            }
        }
    }

    private fun createDiscordMessage(title: String, content: String, username: String): JSONObject {
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())

        return JSONObject().apply {
            put("username", username)

            // 根據username設置不同的頭像
            when {
                username.contains("Instagram", ignoreCase = true) -> {
                    put("avatar_url", "https://upload.wikimedia.org/wikipedia/commons/a/a5/Instagram_icon.png")
                }
                username.contains("Twitter", ignoreCase = true) || username.contains("X", ignoreCase = true) -> {
                    put("avatar_url", "https://upload.wikimedia.org/wikipedia/commons/c/ce/X_logo_2023.svg")
                }
                else -> {
                    put("avatar_url", "https://upload.wikimedia.org/wikipedia/commons/a/a5/Instagram_icon.png")
                }
            }

            // 如果內容很短，使用簡單消息
            if (content.length <= 100) {
                put("content", "🔔 **$title**\n$content")
            } else {
                // 使用embed格式處理長消息
                val embed = JSONObject().apply {
                    put("title", title)
                    put("description", content)

                    // 根據來源設置不同顏色
                    when {
                        username.contains("Instagram", ignoreCase = true) -> {
                            put("color", 0xE4405F) // Instagram品牌色
                        }
                        username.contains("Twitter", ignoreCase = true) || username.contains("X", ignoreCase = true) -> {
                            put("color", 0x1DA1F2) // Twitter品牌色
                        }
                        else -> {
                            put("color", 0xE4405F)
                        }
                    }

                    put("timestamp", timestamp)

                    val footer = JSONObject().apply {
                        if (username.contains("Instagram", ignoreCase = true)) {
                            put("text", "Instagram 通知轉發器")
                            put("icon_url", "https://upload.wikimedia.org/wikipedia/commons/a/a5/Instagram_icon.png")
                        } else if (username.contains("Twitter", ignoreCase = true) || username.contains("X", ignoreCase = true)) {
                            put("text", "X (Twitter) 通知轉發器")
                            put("icon_url", "https://upload.wikimedia.org/wikipedia/commons/c/ce/X_logo_2023.svg")
                        } else {
                            put("text", "通知轉發器")
                        }
                    }
                    put("footer", footer)
                }

                put("embeds", arrayOf(embed))
            }
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
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())

        return JSONObject().apply {
            put("username", customBotName)
            if (customIconUrl.isNotEmpty()) {
                put("avatar_url", customIconUrl)
            }

            // 使用embed格式以支援更多自定義選項
            val embed = JSONObject().apply {
                if (title.isNotEmpty()) {
                    put("title", title)
                }
                put("description", content)
                put("color", customColor)

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
                    if (customIconUrl.isNotEmpty()) {
                        put("icon_url", customIconUrl)
                    }
                }
                put("footer", footer)
            }

            put("embeds", arrayOf(embed))
        }
    }
}