package com.stripe.android.iap

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

internal class CheckoutRedirectHandlerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(CheckoutForegroundActivity.redirectIntent(this, intent.data))
        finish()
    }
}
