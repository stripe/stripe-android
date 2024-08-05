package com.stripe.example.activity

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.CardBrand
import com.stripe.android.view.CardFormView
import com.stripe.example.Settings

class CardFormViewActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PaymentConfiguration.init(this, Settings(this).publishableKey)
        val frame = LinearLayout(this)
        frame.orientation = LinearLayout.VERTICAL

        val cardFormView = CardFormView(this)
        frame.addView(cardFormView)
        val button = Button(this).apply {
            text = "Set Cartes Bancaires as preferred Network"
            setOnClickListener {
                cardFormView.setPreferredNetworks(listOf(CardBrand.CartesBancaires))
            }
        }
        frame.addView(button)
        setContentView(frame)
    }
}