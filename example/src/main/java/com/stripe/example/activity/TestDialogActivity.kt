package com.stripe.example.activity

import android.app.Activity
import android.os.Bundle
import android.widget.FrameLayout
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.CardBrand
import com.stripe.android.view.CardFormView
import com.stripe.example.Settings

class TestDialogActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PaymentConfiguration.init(this, Settings(this).publishableKey)

        //val dialog = Dialog(this)
        val frame = FrameLayout(this)

        val cardFormView = CardFormView(this)
        cardFormView.setPreferredNetworks(listOf(CardBrand.CartesBancaires))
        frame.addView(cardFormView)
        setContentView(frame)
    }
}