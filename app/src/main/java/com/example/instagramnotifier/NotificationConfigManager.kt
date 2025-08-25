package com.example.instagramnotifier

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

class NotificationConfigManager(private val context: Context) {

    companion object {
        private const val TAG = "ConfigManager"
        private const val PREFS_NAME = "NotificationConfigs"
        private const val KEY_CONFIGS = "configs"

        @Volatile
        private var INSTANCE: NotificationConfigManager? = null

        fun getInstance(context: Context): NotificationConfigManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationConfigManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _configs = mutableListOf<NotificationConfig>()

    val configs: List<NotificationConfig>
        get() = _configs.toList()

    init {
        loadConfigs()
        // 如果沒有配置，創建預設配置
        if (_configs.isEmpty()) {
            createDefaultConfigs()
        }
    }

    private fun loadConfigs() {
        try {
            val configsJson = sharedPrefs.getString(KEY_CONFIGS, null)
            if (configsJson != null) {
                val jsonArray = JSONArray(configsJson)
                _configs.clear()
                for (i in 0 until jsonArray.length()) {
                    try {
                        val configJson = jsonArray.getJSONObject(i)
                        val config = NotificationConfig.fromJson(configJson)
                        _configs.add(config)
                    } catch (e: Exception) {
                        Log.e(TAG, "載入配置 $i 時發生錯誤", e)
                    }
                }
                Log.d(TAG, "已載入 ${_configs.size} 個通知配置")
            }
        } catch (e: Exception) {
            Log.e(TAG, "載入配置時發生錯誤", e)
            _configs.clear()
        }
    }

    private fun saveConfigs() {
        try {
            val jsonArray = JSONArray()
            _configs.forEach { config ->
                jsonArray.put(config.toJson())
            }
            sharedPrefs.edit()
                .putString(KEY_CONFIGS, jsonArray.toString())
                .apply()
            Log.d(TAG, "已保存 ${_configs.size} 個通知配置")
        } catch (e: Exception) {
            Log.e(TAG, "保存配置時發生錯誤", e)
        }
    }

    private fun createDefaultConfigs() {
        // 遷移舊的設定
        migrateOldSettings()

        // 如果仍然沒有配置，創建空的預設配置
        if (_configs.isEmpty()) {
            Log.d(TAG, "創建預設配置")
            saveConfigs()
        }
    }

    private fun migrateOldSettings() {
        val oldPrefs = context.getSharedPreferences("NotifierPrefs", Context.MODE_PRIVATE)

        // 遷移 Instagram 配置
        val instagramWebhook = oldPrefs.getString("instagram_webhook_url", "")
        if (!instagramWebhook.isNullOrEmpty()) {
            val instagramConfig = NotificationConfig.getDefaultConfig(NotificationConfig.Platform.INSTAGRAM).copy(
                webhookUrl = instagramWebhook,
                botName = oldPrefs.getString("instagram_bot_name", "Instagram Bot") ?: "Instagram Bot",
                iconUrl = oldPrefs.getString("instagram_icon_url", "https://upload.wikimedia.org/wikipedia/commons/a/a5/Instagram_icon.png") ?: "",
                color = oldPrefs.getString("instagram_color", "E4405F") ?: "E4405F",
                includeTitle = oldPrefs.getBoolean("include_instagram_title", true),
                includeTimestamp = oldPrefs.getBoolean("include_timestamp", true)
            )
            _configs.add(instagramConfig)
            Log.d(TAG, "已遷移 Instagram 配置")
        }

        // 遷移 Twitter 配置
        val twitterWebhook = oldPrefs.getString("twitter_webhook_url", "")
        if (!twitterWebhook.isNullOrEmpty()) {
            val twitterConfig = NotificationConfig.getDefaultConfig(NotificationConfig.Platform.TWITTER).copy(
                webhookUrl = twitterWebhook,
                botName = oldPrefs.getString("twitter_bot_name", "X Bot") ?: "X Bot",
                iconUrl = oldPrefs.getString("twitter_icon_url", "https://upload.wikimedia.org/wikipedia/commons/c/ce/X_logo_2023.svg") ?: "",
                color = oldPrefs.getString("twitter_color", "1DA1F2") ?: "1DA1F2",
                includeTitle = oldPrefs.getBoolean("include_twitter_title", true),
                includeTimestamp = oldPrefs.getBoolean("include_timestamp", true)
            )
            _configs.add(twitterConfig)
            Log.d(TAG, "已遷移 Twitter 配置")
        }
    }

    fun addConfig(config: NotificationConfig) {
        _configs.add(config)
        saveConfigs()
        Log.d(TAG, "已添加配置: ${config.name}")
    }

    fun updateConfig(config: NotificationConfig) {
        val index = _configs.indexOfFirst { it.id == config.id }
        if (index != -1) {
            _configs[index] = config
            saveConfigs()
            Log.d(TAG, "已更新配置: ${config.name}")
        }
    }

    fun deleteConfig(configId: String) {
        val index = _configs.indexOfFirst { it.id == configId }
        if (index != -1) {
            val removedConfig = _configs.removeAt(index)
            saveConfigs()
            Log.d(TAG, "已刪除配置: ${removedConfig.name}")
        }
    }

    fun getConfig(configId: String): NotificationConfig? {
        return _configs.find { it.id == configId }
    }

    fun getConfigsByPlatform(platform: NotificationConfig.Platform): List<NotificationConfig> {
        return _configs.filter { it.platform == platform && it.isEnabled }
    }

    fun getEnabledConfigs(): List<NotificationConfig> {
        return _configs.filter { it.isEnabled }
    }

    fun toggleConfigEnabled(configId: String) {
        val index = _configs.indexOfFirst { it.id == configId }
        if (index != -1) {
            val config = _configs[index]
            _configs[index] = config.copy(isEnabled = !config.isEnabled)
            saveConfigs()
            Log.d(TAG, "已切換配置狀態: ${config.name} -> ${!config.isEnabled}")
        }
    }

    fun duplicateConfig(configId: String): NotificationConfig? {
        val originalConfig = getConfig(configId)
        if (originalConfig != null) {
            val duplicatedConfig = originalConfig.copy(
                id = java.util.UUID.randomUUID().toString(),
                name = "${originalConfig.name} (副本)"
            )
            addConfig(duplicatedConfig)
            return duplicatedConfig
        }
        return null
    }

    fun testConfig(config: NotificationConfig, context: Context) {
        val testTitle = "🧪 ${config.platform.displayName} 測試"
        val testMessage = "這是來自「${config.name}」配置的測試訊息"

        NotificationSender.sendToDiscordWithConfig(
            config,
            testTitle,
            testMessage,
            context
        )
    }
}