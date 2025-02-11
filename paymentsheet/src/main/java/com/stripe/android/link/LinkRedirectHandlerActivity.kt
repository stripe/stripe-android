package com.stripe.android.link

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

internal class LinkRedirectHandlerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(LinkForegroundActivity.redirectIntent(this, intent.data))
        finish()
    }
}
