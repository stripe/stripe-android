package com.stripe.example.activity

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.CardBrand
import com.stripe.android.view.CardInputWidget
import com.stripe.example.Settings

class TestCBCActivity : Activity() {

    lateinit var cardFormView: CardInputWidget
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PaymentConfiguration.init(this, Settings(this).publishableKey)
        val frame = LinearLayout(this)
        frame.orientation = LinearLayout.VERTICAL

        cardFormView = CardInputWidget(this)
        cardFormView.setPreferredNetworks(listOf(CardBrand.CartesBancaires))
        cardFormView.onBehalfOf = "test"
        frame.addView(cardFormView)
        setContentView(frame)
    }
}
