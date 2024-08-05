package com.stripe.example.activity

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.CardBrand
import com.stripe.android.view.CardMultilineWidget
import com.stripe.example.Settings

class CardMultilineWidgetActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PaymentConfiguration.init(this, Settings(this).publishableKey)
        val frame = LinearLayout(this)
        frame.orientation = LinearLayout.VERTICAL

        val cardMultilineWidget = CardMultilineWidget(this)
        frame.addView(cardMultilineWidget)
        val button = Button(this).apply {
            text = "Set Cartes Bancaires as preferred Network"
            setOnClickListener {
                cardMultilineWidget.setPreferredNetworks(listOf(CardBrand.CartesBancaires))
            }
        }
        frame.addView(button)
        setContentView(frame)
    }
}