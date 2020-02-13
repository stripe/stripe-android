package com.stripe.android.test

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.R
import com.stripe.android.view.CardInputWidget

internal class CardInputWidgetTestActivity : AppCompatActivity() {
    val cardInputWidget: CardInputWidget by lazy {
        findViewById<CardInputWidget>(R.id.card_input_widget)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.card_input_widget_activity)
    }
}
