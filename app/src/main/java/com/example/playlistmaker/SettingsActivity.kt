package com.example.playlistmaker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.widget.Switch
import android.widget.TextView
import android.net.Uri


class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        findViewById<TextView>(R.id.shareLayout).setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    getString(R.string.share_message, getString(R.string.practice_url))
                )
            }
            startActivity(Intent.createChooser(shareIntent, null))
        }


        findViewById<TextView>(R.id.supportLayout).setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.support_email)))
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject))
                putExtra(Intent.EXTRA_TEXT, getString(R.string.email_body))
            }
            if (emailIntent.resolveActivity(packageManager) != null) {
                startActivity(emailIntent)
            }
        }


        findViewById<TextView>(R.id.agreementLayout).setOnClickListener {
            val agreementIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(getString(R.string.agreement_url))
            }
            if (agreementIntent.resolveActivity(packageManager) != null) {
                startActivity(agreementIntent)
            }
        }
    }
}