package com.example.instagramnotifier

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class NotificationConfigAdapter(
    private val onEditClick: (NotificationConfig) -> Unit,
    private val onDeleteClick: (NotificationConfig) -> Unit,
    private val onToggleClick: (NotificationConfig) -> Unit,
    private val onTestClick: (NotificationConfig) -> Unit,
    private val onDuplicateClick: (NotificationConfig) -> Unit
) : ListAdapter<NotificationConfig, NotificationConfigAdapter.ConfigViewHolder>(ConfigDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConfigViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification_config, parent, false)
        return ConfigViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConfigViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ConfigViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val platformIcon: ImageView = itemView.findViewById(R.id.imageViewPlatformIcon)
        private val configName: TextView = itemView.findViewById(R.id.textViewConfigName)
        private val configDetails: TextView = itemView.findViewById(R.id.textViewConfigDetails)
        private val configKeywords: TextView = itemView.findViewById(R.id.textViewConfigKeywords)
        private val enableSwitch: SwitchCompat = itemView.findViewById(R.id.switchConfigEnabled)
        private val editButton: ImageButton = itemView.findViewById(R.id.buttonEditConfig)
        private val testButton: ImageButton = itemView.findViewById(R.id.buttonTestConfig)
        private val duplicateButton: ImageButton = itemView.findViewById(R.id.buttonDuplicateConfig)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.buttonDeleteConfig)

        fun bind(config: NotificationConfig) {
            val context = itemView.context

            // 設置平台圖示
            when (config.platform) {
                NotificationConfig.Platform.INSTAGRAM -> {
                    platformIcon.setImageResource(R.drawable.ic_instagram)
                    platformIcon.setColorFilter(ContextCompat.getColor(context, R.color.instagram_primary))
                }
                NotificationConfig.Platform.TWITTER -> {
                    platformIcon.setImageResource(R.drawable.ic_twitter)
                    platformIcon.setColorFilter(ContextCompat.getColor(context, R.color.twitter_primary))
                }
            }

            // 設置基本資訊
            configName.text = config.name.ifEmpty {
                context.getString(R.string.default_config_name, config.platform.displayName)
            }

            // 設置詳細資訊
            val details = buildString {
                append(context.getString(R.string.config_platform_label, config.platform.displayName))
                if (config.botName.isNotEmpty()) {
                    append("\n")
                    append(context.getString(R.string.config_bot_label, config.botName))
                }
                append("\n")
                append(context.getString(R.string.config_filter_label, config.filterMode.displayName))
                if (config.webhookUrl.isEmpty()) {
                    append("\n")
                    append(context.getString(R.string.config_webhook_missing))
                }
            }
            configDetails.text = details

            // 設置關鍵字顯示
            if (config.keywords.isNotEmpty()) {
                configKeywords.visibility = View.VISIBLE
                val keywordString = config.keywords.joinToString(", ")
                configKeywords.text = context.getString(R.string.config_keywords_label, keywordString)
            } else {
                configKeywords.visibility = View.GONE
            }

            // 設置啟用狀態
            enableSwitch.setOnCheckedChangeListener(null) // 清除舊的監聽器
            enableSwitch.isChecked = config.isEnabled
            enableSwitch.setOnCheckedChangeListener { _, _ ->
                onToggleClick(config)
            }

            // 設置按鈕點擊事件
            editButton.setOnClickListener { onEditClick(config) }
            testButton.setOnClickListener { onTestClick(config) }
            duplicateButton.setOnClickListener { onDuplicateClick(config) }
            deleteButton.setOnClickListener { onDeleteClick(config) }

            // 設置整個項目的透明度
            itemView.alpha = if (config.isEnabled) 1.0f else 0.6f

            // 禁用測試按鈕如果沒有設置 webhook
            testButton.isEnabled = config.webhookUrl.isNotEmpty()
            testButton.alpha = if (config.webhookUrl.isNotEmpty()) 1.0f else 0.5f

            // 設置按鈕的色調
            editButton.setColorFilter(ContextCompat.getColor(context, R.color.black))
            testButton.setColorFilter(ContextCompat.getColor(context, if (config.webhookUrl.isNotEmpty()) R.color.black else android.R.color.darker_gray))
            duplicateButton.setColorFilter(ContextCompat.getColor(context, R.color.black))
            // deleteButton 的顏色已經在 XML 中設置為紅色
        }
    }

    class ConfigDiffCallback : DiffUtil.ItemCallback<NotificationConfig>() {
        override fun areItemsTheSame(oldItem: NotificationConfig, newItem: NotificationConfig): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NotificationConfig, newItem: NotificationConfig): Boolean {
            return oldItem == newItem
        }
    }
}