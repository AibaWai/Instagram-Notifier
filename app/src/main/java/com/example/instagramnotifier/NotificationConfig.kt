package com.example.instagramnotifier

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class NotificationConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val platform: Platform = Platform.INSTAGRAM,
    val webhookUrl: String = "",
    val keywords: List<String> = emptyList(),
    val filterMode: FilterMode = FilterMode.INCLUDE_ANY,
    val isEnabled: Boolean = true,
    val botName: String = "",
    val iconUrl: String = "",
    val color: String = "",
    val includeTitle: Boolean = true,
    val includeTimestamp: Boolean = true
) {
    enum class Platform(val displayName: String, val packageName: String) {
        INSTAGRAM("Instagram", "com.instagram.android"),
        TWITTER("X (Twitter)", "com.twitter.android")
    }

    enum class FilterMode(val displayName: String) {
        INCLUDE_ANY("包含任一關鍵字"),
        INCLUDE_ALL("包含所有關鍵字"),
        EXCLUDE_ANY("排除任一關鍵字"),
        EXCLUDE_ALL("排除所有關鍵字"),
        NO_FILTER("無過濾")
    }

    fun matchesFilter(content: String): Boolean {
        if (keywords.isEmpty() || filterMode == FilterMode.NO_FILTER) {
            return true
        }

        val contentLower = content.lowercase()
        val keywordsLower = keywords.map { it.lowercase().trim() }.filter { it.isNotEmpty() }

        if (keywordsLower.isEmpty()) {
            return true
        }

        return when (filterMode) {
            FilterMode.INCLUDE_ANY -> keywordsLower.any { keyword ->
                contentLower.contains(keyword)
            }
            FilterMode.INCLUDE_ALL -> keywordsLower.all { keyword ->
                contentLower.contains(keyword)
            }
            FilterMode.EXCLUDE_ANY -> keywordsLower.none { keyword ->
                contentLower.contains(keyword)
            }
            FilterMode.EXCLUDE_ALL -> !keywordsLower.all { keyword ->
                contentLower.contains(keyword)
            }
            FilterMode.NO_FILTER -> true
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("platform", platform.name)
            put("webhookUrl", webhookUrl)
            put("keywords", JSONArray(keywords))
            put("filterMode", filterMode.name)
            put("isEnabled", isEnabled)
            put("botName", botName)
            put("iconUrl", iconUrl)
            put("color", color)
            put("includeTitle", includeTitle)
            put("includeTimestamp", includeTimestamp)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): NotificationConfig {
            return NotificationConfig(
                id = json.optString("id", UUID.randomUUID().toString()),
                name = json.optString("name", ""),
                platform = try {
                    Platform.valueOf(json.optString("platform", Platform.INSTAGRAM.name))
                } catch (e: IllegalArgumentException) {
                    Platform.INSTAGRAM
                },
                webhookUrl = json.optString("webhookUrl", ""),
                keywords = try {
                    val keywordArray = json.optJSONArray("keywords")
                    if (keywordArray != null) {
                        (0 until keywordArray.length()).map {
                            keywordArray.getString(it)
                        }
                    } else {
                        emptyList()
                    }
                } catch (e: Exception) {
                    emptyList()
                },
                filterMode = try {
                    FilterMode.valueOf(json.optString("filterMode", FilterMode.NO_FILTER.name))
                } catch (e: IllegalArgumentException) {
                    FilterMode.NO_FILTER
                },
                isEnabled = json.optBoolean("isEnabled", true),
                botName = json.optString("botName", ""),
                iconUrl = json.optString("iconUrl", ""),
                color = json.optString("color", ""),
                includeTitle = json.optBoolean("includeTitle", true),
                includeTimestamp = json.optBoolean("includeTimestamp", true)
            )
        }

        fun getDefaultConfig(platform: Platform): NotificationConfig {
            return when (platform) {
                Platform.INSTAGRAM -> NotificationConfig(
                    name = "Instagram 預設",
                    platform = platform,
                    botName = "Instagram Bot",
                    iconUrl = "https://upload.wikimedia.org/wikipedia/commons/a/a5/Instagram_icon.png",
                    color = "E4405F"
                )
                Platform.TWITTER -> NotificationConfig(
                    name = "X (Twitter) 預設",
                    platform = platform,
                    botName = "X Bot",
                    iconUrl = "https://upload.wikimedia.org/wikipedia/commons/c/ce/X_logo_2023.svg",
                    color = "1DA1F2"
                )
            }
        }
    }
}