package com.stripe.example.activity

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import com.stripe.android.PaymentConfiguration
import com.stripe.android.view.CardFormView
import com.stripe.example.Settings

class ComposeLifecycleOwnerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PaymentConfiguration.init(this, Settings(this).publishableKey)

        val dialog = Dialog(this)

        val cardFormView = CardFormView(this)
        dialog.setContentView(cardFormView)
        dialog.show()
    }
}
