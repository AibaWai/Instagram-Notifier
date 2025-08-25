package com.example.instagramnotifier

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class ConfigEditActivity : AppCompatActivity() {

    private lateinit var configManager: NotificationConfigManager
    private var currentConfig: NotificationConfig? = null
    private var isEditMode = false

    // UI 元件
    private lateinit var editTextConfigName: EditText
    private lateinit var spinnerPlatform: Spinner
    private lateinit var editTextWebhookUrl: EditText
    private lateinit var editTextKeywords: EditText
    private lateinit var spinnerFilterMode: Spinner
    private lateinit var editTextBotName: EditText
    private lateinit var editTextIconUrl: EditText
    private lateinit var editTextColor: EditText
    private lateinit var switchIncludeTitle: SwitchCompat
    private lateinit var switchIncludeTimestamp: SwitchCompat
    private lateinit var switchEnabled: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config_edit)

        configManager = NotificationConfigManager.getInstance(this)

        val configId = intent.getStringExtra("config_id")
        if (configId != null) {
            currentConfig = configManager.getConfig(configId)
            isEditMode = true
            supportActionBar?.title = "編輯配置"
        } else {
            isEditMode = false
            supportActionBar?.title = "新增配置"
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initViews()
        setupSpinners()
        loadConfigData()
    }

    private fun initViews() {
        editTextConfigName = findViewById(R.id.editTextConfigName)
        spinnerPlatform = findViewById(R.id.spinnerPlatform)
        editTextWebhookUrl = findViewById(R.id.editTextWebhookUrl)
        editTextKeywords = findViewById(R.id.editTextKeywords)
        spinnerFilterMode = findViewById(R.id.spinnerFilterMode)
        editTextBotName = findViewById(R.id.editTextBotName)
        editTextIconUrl = findViewById(R.id.editTextIconUrl)
        editTextColor = findViewById(R.id.editTextColor)
        switchIncludeTitle = findViewById(R.id.switchIncludeTitle)
        switchIncludeTimestamp = findViewById(R.id.switchIncludeTimestamp)
        switchEnabled = findViewById(R.id.switchEnabled)
    }

    private fun setupSpinners() {
        // 設置平台選擇器
        val platformAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            NotificationConfig.Platform.values().map { it.displayName }
        )
        platformAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPlatform.adapter = platformAdapter

        // 設置過濾模式選擇器
        val filterModeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            NotificationConfig.FilterMode.values().map { it.displayName }
        )
        filterModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFilterMode.adapter = filterModeAdapter

        // 設置平台變更監聽器，自動填入預設值
        spinnerPlatform.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (!isEditMode || currentConfig == null) {
                    val platform = NotificationConfig.Platform.values()[position]
                    val defaultConfig = NotificationConfig.getDefaultConfig(platform)

                    if (editTextBotName.text.isEmpty()) {
                        editTextBotName.setText(defaultConfig.botName)
                    }
                    if (editTextIconUrl.text.isEmpty()) {
                        editTextIconUrl.setText(defaultConfig.iconUrl)
                    }
                    if (editTextColor.text.isEmpty()) {
                        editTextColor.setText(defaultConfig.color)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadConfigData() {
        currentConfig?.let { config ->
            editTextConfigName.setText(config.name)
            spinnerPlatform.setSelection(NotificationConfig.Platform.values().indexOf(config.platform))
            editTextWebhookUrl.setText(config.webhookUrl)
            editTextKeywords.setText(config.keywords.joinToString(", "))
            spinnerFilterMode.setSelection(NotificationConfig.FilterMode.values().indexOf(config.filterMode))
            editTextBotName.setText(config.botName)
            editTextIconUrl.setText(config.iconUrl)
            editTextColor.setText(config.color)
            switchIncludeTitle.isChecked = config.includeTitle
            switchIncludeTimestamp.isChecked = config.includeTimestamp
            switchEnabled.isChecked = config.isEnabled
        } ?: run {
            // 新增模式，設置預設值
            val defaultConfig = NotificationConfig.getDefaultConfig(NotificationConfig.Platform.INSTAGRAM)
            editTextBotName.setText(defaultConfig.botName)
            editTextIconUrl.setText(defaultConfig.iconUrl)
            editTextColor.setText(defaultConfig.color)
            switchIncludeTitle.isChecked = true
            switchIncludeTimestamp.isChecked = true
            switchEnabled.isChecked = true
            spinnerFilterMode.setSelection(NotificationConfig.FilterMode.values().indexOf(NotificationConfig.FilterMode.NO_FILTER))
        }
    }

    private fun saveConfig(): Boolean {
        // 驗證必填欄位
        if (editTextConfigName.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "請輸入配置名稱", Toast.LENGTH_SHORT).show()
            editTextConfigName.requestFocus()
            return false
        }

        if (editTextWebhookUrl.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "請輸入 Discord Webhook URL", Toast.LENGTH_SHORT).show()
            editTextWebhookUrl.requestFocus()
            return false
        }

        // 驗證 Webhook URL 格式
        val webhookUrl = editTextWebhookUrl.text.toString().trim()
        if (!webhookUrl.startsWith("https://discord.com/api/webhooks/") &&
            !webhookUrl.startsWith("https://discordapp.com/api/webhooks/")) {
            Toast.makeText(this, "請輸入有效的 Discord Webhook URL", Toast.LENGTH_SHORT).show()
            editTextWebhookUrl.requestFocus()
            return false
        }

        // 解析關鍵字
        val keywordsText = editTextKeywords.text.toString().trim()
        val keywords = if (keywordsText.isNotEmpty()) {
            keywordsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            emptyList()
        }

        // 驗證顏色格式
        val colorText = editTextColor.text.toString().trim().removePrefix("#")
        if (colorText.isNotEmpty()) {
            try {
                Integer.parseInt(colorText, 16)
                if (colorText.length != 6) {
                    Toast.makeText(this, "顏色格式應為 6 位 16 進制數字，例如：E4405F", Toast.LENGTH_SHORT).show()
                    editTextColor.requestFocus()
                    return false
                }
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "顏色格式錯誤，請使用 16 進制格式，例如：E4405F", Toast.LENGTH_SHORT).show()
                editTextColor.requestFocus()
                return false
            }
        }

        // 建立配置對象
        val config = if (isEditMode && currentConfig != null) {
            currentConfig!!.copy(
                name = editTextConfigName.text.toString().trim(),
                platform = NotificationConfig.Platform.values()[spinnerPlatform.selectedItemPosition],
                webhookUrl = webhookUrl,
                keywords = keywords,
                filterMode = NotificationConfig.FilterMode.values()[spinnerFilterMode.selectedItemPosition],
                botName = editTextBotName.text.toString().trim(),
                iconUrl = editTextIconUrl.text.toString().trim(),
                color = colorText,
                includeTitle = switchIncludeTitle.isChecked,
                includeTimestamp = switchIncludeTimestamp.isChecked,
                isEnabled = switchEnabled.isChecked
            )
        } else {
            NotificationConfig(
                name = editTextConfigName.text.toString().trim(),
                platform = NotificationConfig.Platform.values()[spinnerPlatform.selectedItemPosition],
                webhookUrl = webhookUrl,
                keywords = keywords,
                filterMode = NotificationConfig.FilterMode.values()[spinnerFilterMode.selectedItemPosition],
                botName = editTextBotName.text.toString().trim(),
                iconUrl = editTextIconUrl.text.toString().trim(),
                color = colorText,
                includeTitle = switchIncludeTitle.isChecked,
                includeTimestamp = switchIncludeTimestamp.isChecked,
                isEnabled = switchEnabled.isChecked
            )
        }

        // 保存配置
        if (isEditMode) {
            configManager.updateConfig(config)
            Toast.makeText(this, "配置已更新", Toast.LENGTH_SHORT).show()
        } else {
            configManager.addConfig(config)
            Toast.makeText(this, "配置已新增", Toast.LENGTH_SHORT).show()
        }

        return true
    }

    private fun testConfig() {
        val platform = NotificationConfig.Platform.values()[spinnerPlatform.selectedItemPosition]
        val webhookUrl = editTextWebhookUrl.text.toString().trim()

        if (webhookUrl.isEmpty()) {
            Toast.makeText(this, "請先輸入 Webhook URL", Toast.LENGTH_SHORT).show()
            return
        }

        // 建立臨時配置用於測試
        val tempConfig = NotificationConfig(
            name = editTextConfigName.text.toString().trim().ifEmpty { "測試配置" },
            platform = platform,
            webhookUrl = webhookUrl,
            botName = editTextBotName.text.toString().trim(),
            iconUrl = editTextIconUrl.text.toString().trim(),
            color = editTextColor.text.toString().trim().removePrefix("#"),
            includeTitle = switchIncludeTitle.isChecked,
            includeTimestamp = switchIncludeTimestamp.isChecked
        )

        configManager.testConfig(tempConfig, this)
        Toast.makeText(this, "測試訊息已發送", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.config_edit_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                if (saveConfig()) {
                    setResult(RESULT_OK)
                    finish()
                }
                true
            }
            R.id.action_test -> {
                testConfig()
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}