package com.stripe.example.activity

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.CardBrand
import com.stripe.android.view.CardFormView
import com.stripe.android.view.CardInputWidget
import com.stripe.android.view.CardMultilineWidget
import com.stripe.example.Settings

class ComposeLifecycleOwnerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PaymentConfiguration.init(this, Settings(this).publishableKey)

        val linearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val cardFormView = CardFormView(this)
        val cardInputWidget = CardInputWidget(this)
        val cardMultilineWidget = CardMultilineWidget(this)
        cardFormView.setPreferredNetworks(listOf(CardBrand.CartesBancaires))
        linearLayout.addView(cardFormView)
        linearLayout.addView(cardInputWidget)
        linearLayout.addView(cardMultilineWidget)
        setContentView(linearLayout)
    }
}