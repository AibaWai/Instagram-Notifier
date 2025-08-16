package com.example.instagramnotifier

import android.os.Bundle
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class SettingsActivity : AppCompatActivity() {

    private lateinit var instagramBotNameEditText: EditText
    private lateinit var twitterBotNameEditText: EditText
    private lateinit var instagramIconEditText: EditText
    private lateinit var twitterIconEditText: EditText
    private lateinit var includeInstagramTitleSwitch: SwitchCompat
    private lateinit var includeTwitterTitleSwitch: SwitchCompat
    private lateinit var includeTimestampSwitch: SwitchCompat
    private lateinit var customInstagramColorEditText: EditText
    private lateinit var customTwitterColorEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var resetButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // 設置返回按鈕
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings_title)

        initViews()
        loadSettings()
        setupClickListeners()
        setupBackPressedCallback()
    }

    private fun initViews() {
        instagramBotNameEditText = findViewById(R.id.editTextInstagramBotName)
        twitterBotNameEditText = findViewById(R.id.editTextTwitterBotName)
        instagramIconEditText = findViewById(R.id.editTextInstagramIcon)
        twitterIconEditText = findViewById(R.id.editTextTwitterIcon)
        includeInstagramTitleSwitch = findViewById(R.id.switchIncludeInstagramTitle)
        includeTwitterTitleSwitch = findViewById(R.id.switchIncludeTwitterTitle)
        includeTimestampSwitch = findViewById(R.id.switchIncludeTimestamp)
        customInstagramColorEditText = findViewById(R.id.editTextInstagramColor)
        customTwitterColorEditText = findViewById(R.id.editTextTwitterColor)
        saveButton = findViewById(R.id.buttonSaveSettings)
        resetButton = findViewById(R.id.buttonResetSettings)
    }

    private fun loadSettings() {
        val sharedPrefs = getSharedPreferences("NotifierPrefs", MODE_PRIVATE)

        // Bot 名稱
        instagramBotNameEditText.setText(sharedPrefs.getString("instagram_bot_name", getString(R.string.default_instagram_bot_name)))
        twitterBotNameEditText.setText(sharedPrefs.getString("twitter_bot_name", getString(R.string.default_twitter_bot_name)))

        // Bot 圖示 URL
        instagramIconEditText.setText(sharedPrefs.getString("instagram_icon_url", getString(R.string.default_instagram_icon_url)))
        twitterIconEditText.setText(sharedPrefs.getString("twitter_icon_url", getString(R.string.default_twitter_icon_url)))

        // 顯示設定
        includeInstagramTitleSwitch.isChecked = sharedPrefs.getBoolean("include_instagram_title", true)
        includeTwitterTitleSwitch.isChecked = sharedPrefs.getBoolean("include_twitter_title", true)
        includeTimestampSwitch.isChecked = sharedPrefs.getBoolean("include_timestamp", true)

        // 自定義顏色 (16進制，如: E4405F)
        customInstagramColorEditText.setText(sharedPrefs.getString("instagram_color", getString(R.string.default_instagram_color)))
        customTwitterColorEditText.setText(sharedPrefs.getString("twitter_color", getString(R.string.default_twitter_color)))
    }

    private fun setupClickListeners() {
        saveButton.setOnClickListener {
            saveSettings()
        }

        resetButton.setOnClickListener {
            resetToDefaults()
        }
    }

    private fun setupBackPressedCallback() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun saveSettings() {
        val sharedPrefs = getSharedPreferences("NotifierPrefs", MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        // 保存 Bot 名稱
        editor.putString("instagram_bot_name", instagramBotNameEditText.text.toString().trim())
        editor.putString("twitter_bot_name", twitterBotNameEditText.text.toString().trim())

        // 保存圖示 URL
        editor.putString("instagram_icon_url", instagramIconEditText.text.toString().trim())
        editor.putString("twitter_icon_url", twitterIconEditText.text.toString().trim())

        // 保存顯示設定
        editor.putBoolean("include_instagram_title", includeInstagramTitleSwitch.isChecked)
        editor.putBoolean("include_twitter_title", includeTwitterTitleSwitch.isChecked)
        editor.putBoolean("include_timestamp", includeTimestampSwitch.isChecked)

        // 保存顏色設定
        var instagramColor = customInstagramColorEditText.text.toString().trim()
        var twitterColor = customTwitterColorEditText.text.toString().trim()

        // 確保顏色格式正確
        if (!instagramColor.startsWith("#") && instagramColor.isNotEmpty()) {
            instagramColor = instagramColor.removePrefix("#")
        }
        if (!twitterColor.startsWith("#") && twitterColor.isNotEmpty()) {
            twitterColor = twitterColor.removePrefix("#")
        }

        editor.putString("instagram_color", instagramColor)
        editor.putString("twitter_color", twitterColor)

        editor.apply()

        Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun resetToDefaults() {
        instagramBotNameEditText.setText(getString(R.string.default_instagram_bot_name))
        twitterBotNameEditText.setText(getString(R.string.default_twitter_bot_name))
        instagramIconEditText.setText(getString(R.string.default_instagram_icon_url))
        twitterIconEditText.setText(getString(R.string.default_twitter_icon_url))
        includeInstagramTitleSwitch.isChecked = true
        includeTwitterTitleSwitch.isChecked = true
        includeTimestampSwitch.isChecked = true
        customInstagramColorEditText.setText(getString(R.string.default_instagram_color))
        customTwitterColorEditText.setText(getString(R.string.default_twitter_color))

        Toast.makeText(this, getString(R.string.settings_reset), Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}