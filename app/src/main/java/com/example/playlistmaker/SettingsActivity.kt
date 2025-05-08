package com.example.playlistmaker

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar

class SettingsActivity : AppCompatActivity() {

    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var themeSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPrefs = getSharedPreferences("app_theme", MODE_PRIVATE)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        themeSwitch = findViewById(R.id.themeSwitch)
        initThemeSwitch()

        setupClickListeners()
    }

    private fun initThemeSwitch() {
        themeSwitch.setOnCheckedChangeListener(null)
        themeSwitch.isChecked = isDarkThemeEnabled()

        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != isDarkThemeEnabled()) {
                setDarkTheme(isChecked)

                AppCompatDelegate.setDefaultNightMode(
                    if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO
                )

                delegate.applyDayNight()
            }
        }
    }

    private fun setupClickListeners() {
        findViewById<TextView>(R.id.shareLayout).setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message, getString(R.string.practice_url)))
            }.also { startActivity(Intent.createChooser(it, null)) }
        }

        findViewById<TextView>(R.id.supportLayout).setOnClickListener {
            Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.support_email)))
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject))
                putExtra(Intent.EXTRA_TEXT, getString(R.string.email_body))
            }.takeIf { it.resolveActivity(packageManager) != null }?.let { startActivity(it) }
        }

        findViewById<TextView>(R.id.agreementLayout).setOnClickListener {
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(getString(R.string.agreement_url))
            }.takeIf { it.resolveActivity(packageManager) != null }?.let { startActivity(it) }
        }
    }

    private fun isDarkThemeEnabled(): Boolean {
        return sharedPrefs.getBoolean(DARK_THEME_KEY, false)
    }

    private fun setDarkTheme(enabled: Boolean) {
        sharedPrefs.edit()
            .putBoolean(DARK_THEME_KEY, enabled)
            .apply()
    }

    companion object {
        private const val DARK_THEME_KEY = "dark_theme"
    }
}
