package com.stripe.android.financialconnections.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.browser.customtabs.CustomTabsIntent
import android.net.Uri

class YourChromeCustomTabActivity : AppCompatActivity() {

    private var customTabOpened = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ... Other code to initialize your activity

        // Open the Chrome Custom Tab for the first time
        if (!customTabOpened) {
            openChromeCustomTab()
            customTabOpened = true
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            val uri = intent.data
            if (uri?.path == "/customtab_return") {
                if (!customTabOpened) {
                    openChromeCustomTab()
                    customTabOpened = true
                }
            }
        }
    }

    private fun openChromeCustomTab() {
        val customTabsIntent = CustomTabsIntent.Builder()
            .build()


        customTabsIntent.launchUrl(this, Uri.parse("https://www.yahoo.com"))
    }
}

class ReturnToCustomTabActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent?.data
        if (data != null) {
            val intent = Intent(Intent.ACTION_VIEW, data).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        }
        finish()
    }
}