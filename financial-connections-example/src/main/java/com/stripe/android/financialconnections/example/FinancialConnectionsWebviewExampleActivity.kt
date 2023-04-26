package com.stripe.android.financialconnections.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


class FinancialConnectionsWebviewExampleActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinancialConnectionsWebViewScreen {
                openCustomTab(
                    this,
                    "https://night-discreet-femur.glitch.me/"
                )
            }
        }
    }

    @Composable
    private fun FinancialConnectionsWebViewScreen(onButtonClick: () -> Unit) {
        Column(modifier = Modifier.padding(horizontal = 10.dp)) {
            Button(
                onClick = { onButtonClick() },
            ) {
                Text("Connect Accounts!")
            }

            Divider(modifier = Modifier.padding(vertical = 5.dp))
        }
    }

    private fun openCustomTab(context: Context, url: String) {
        val uri = Uri.parse(url)
        val builder = CustomTabsIntent.Builder()
        builder.setShowTitle(true)
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(context, uri)
    }

}

class FinancialConnectionsWebviewExampleRedirectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent?.data
        if (data != null) {
            val intent =
                Intent(this, FinancialConnectionsWebviewExampleActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            startActivity(intent)
        }
        finish()
    }
}
