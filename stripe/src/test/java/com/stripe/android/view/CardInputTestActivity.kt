package com.stripe.android.view

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.R

/**
 * Activity used to test UI components. We add the layout programmatically to avoid needing test
 * resource files.
 */
internal class CardInputTestActivity : AppCompatActivity() {

    val cardInputWidget: CardInputWidget by lazy {
        CardInputWidget(this)
    }
    val cardMultilineWidget: CardMultilineWidget by lazy {
        CardMultilineWidget(this, shouldShowPostalCode = true)
    }
    val noZipCardMulitlineWidget: CardMultilineWidget by lazy {
        CardMultilineWidget(this, shouldShowPostalCode = false)
    }
    val maskedCardView: MaskedCardView by lazy {
        MaskedCardView(this)
    }
    val cardNumberEditText: CardNumberEditText by lazy {
        cardInputWidget.findViewById<CardNumberEditText>(R.id.et_card_number)
    }

    val expiryDateEditText: ExpiryDateEditText by lazy {
        cardInputWidget.findViewById<ExpiryDateEditText>(R.id.et_expiry_date)
    }

    val cvcEditText: StripeEditText by lazy {
        cardInputWidget.findViewById<StripeEditText>(R.id.et_cvc)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(R.style.StripeDefaultTheme)

        val linearLayout = LinearLayout(this)
        linearLayout.addView(cardInputWidget)
        linearLayout.addView(cardMultilineWidget)
        linearLayout.addView(noZipCardMulitlineWidget)
        linearLayout.addView(maskedCardView)
        setContentView(linearLayout)
    }
}
