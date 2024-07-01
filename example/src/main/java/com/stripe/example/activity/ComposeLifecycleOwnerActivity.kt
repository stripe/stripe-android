package com.stripe.example.activity

import android.app.Activity
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.CardBrand
import com.stripe.android.view.CardFormView
import com.stripe.example.Settings

class ComposeLifecycleOwnerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PaymentConfiguration.init(this, Settings(this).publishableKey)

        val frameLayout = FrameLayout(this)
        val cardFormView = CardFormView(this)
        frameLayout.addView(cardFormView)
        setContentView(frameLayout)
    }
}
