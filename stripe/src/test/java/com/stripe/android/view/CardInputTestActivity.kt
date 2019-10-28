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

    lateinit var cardInputWidget: CardInputWidget
    lateinit var cardMultilineWidget: CardMultilineWidget
    lateinit var noZipCardMulitlineWidget: CardMultilineWidget
    lateinit var maskedCardView: MaskedCardView

    val cardNumberEditText: CardNumberEditText
        get() = cardInputWidget.findViewById(R.id.et_card_number)

    val expiryDateEditText: ExpiryDateEditText
        get() = cardInputWidget.findViewById(R.id.et_expiry_date)

    val cvcEditText: StripeEditText
        get() = cardInputWidget.findViewById(R.id.et_cvc_number)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(R.style.StripeDefaultTheme)
        cardInputWidget = CardInputWidget(this)
        cardMultilineWidget = CardMultilineWidget(this, shouldShowPostalCode = true)
        noZipCardMulitlineWidget = CardMultilineWidget(this, shouldShowPostalCode = false)
        maskedCardView = MaskedCardView(this)

        val linearLayout = LinearLayout(this)
        linearLayout.addView(cardInputWidget)
        linearLayout.addView(cardMultilineWidget)
        linearLayout.addView(noZipCardMulitlineWidget)
        linearLayout.addView(maskedCardView)
        setContentView(linearLayout)
    }
}
