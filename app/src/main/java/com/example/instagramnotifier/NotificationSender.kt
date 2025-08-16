package com.example.instagramnotifier

import android.util.Log
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

object NotificationSender {

    private const val TAG = "NotificationSender"

    fun sendToDiscord(webhookUrl: String, title: String, content: String, username: String = "Instagram Bot") {
        try {
            val url = URL(webhookUrl)
            val connection = url.openConnection() as HttpURLConnection

            // 設置請求方法和標頭
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("User-Agent", "Instagram-Notifier/1.0")
            connection.doOutput = true

            // 構建Discord消息JSON
            val message = createDiscordMessage(title, content, username)

            // 發送請求
            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(message.toString())
            writer.flush()
            writer.close()

            // 檢查回應
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                Log.d(TAG, "消息成功發送到Discord")
            } else {
                Log.w(TAG, "Discord回應代碼: $responseCode")
            }

            connection.disconnect()

        } catch (e: Exception) {
            Log.e(TAG, "發送到Discord時發生錯誤", e)
        }
    }

    private fun createDiscordMessage(title: String, content: String, username: String): JSONObject {
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())

        return JSONObject().apply {
            put("username", username)
            put("avatar_url", "https://upload.wikimedia.org/wikipedia/commons/a/a5/Instagram_icon.png")

            // 如果內容很短，使用簡單消息
            if (content.length <= 100) {
                put("content", "🔔 **$title**\n$content")
            } else {
                // 使用embed格式處理長消息
                val embed = JSONObject().apply {
                    put("title", title)
                    put("description", content)
                    put("color", 0xE4405F) // Instagram品牌色
                    put("timestamp", timestamp)

                    val footer = JSONObject().apply {
                        put("text", "Instagram 通知轉發器")
                        put("icon_url", "https://upload.wikimedia.org/wikipedia/commons/a/a5/Instagram_icon.png")
                    }
                    put("footer", footer)
                }

                put("embeds", arrayOf(embed))
            }
        }
    }
}