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
        cardMultilineWidget = CardMultilineWidget(this, true)
        noZipCardMulitlineWidget = CardMultilineWidget(this, false)
        maskedCardView = MaskedCardView(this)
        val linearLayout = LinearLayout(this)
        linearLayout.addView(cardInputWidget)
        linearLayout.addView(cardMultilineWidget)
        linearLayout.addView(noZipCardMulitlineWidget)
        linearLayout.addView(maskedCardView)
        setContentView(linearLayout)
    }

    companion object {
        internal const val EXAMPLE_JSON_SOURCE_CARD_DATA = "{\"exp_month\":12,\"exp_year\":2050," +
            "\"address_line1_check\":\"unchecked\",\"address_zip_check\":" +
            "\"unchecked\",\"brand\":\"Visa\",\"country\":\"US\",\"cvc_check\"" +
            ":\"unchecked\",\"funding\":\"credit\",\"last4\":\"4242\",\"three_d_secure\"" +
            ":\"optional\"}"

        const val EXAMPLE_JSON_CARD_SOURCE: String = "{\n" +
            "\"id\": \"src_19t3xKBZqEXluyI4uz2dxAfQ\",\n" +
            "\"object\": \"source\",\n" +
            "\"amount\": 1000,\n" +
            "\"client_secret\": \"src_client_secret_of43INi1HteJwXVe3djAUosN\",\n" +
            "\"created\": 1488499654,\n" +
            "\"currency\": \"usd\",\n" +
            "\"flow\": \"receiver\",\n" +
            "\"livemode\": false,\n" +
            "\"metadata\": {\n" +
            "},\n" +
            "\"owner\": {\n" +
            "\"address\": null,\n" +
            "\"email\": \"jenny.rosen@example.com\",\n" +
            "\"name\": \"Jenny Rosen\",\n" +
            "\"phone\": \"4158675309\",\n" +
            "\"verified_address\": null,\n" +
            "\"verified_email\": null,\n" +
            "\"verified_name\": null,\n" +
            "\"verified_phone\": null\n" +
            "},\n" +
            "\"receiver\": {\n" +
            "\"address\": \"test_1MBhWS3uv4ynCfQXF3xQjJkzFPukr4K56N\",\n" +
            "\"amount_charged\": 0,\n" +
            "\"amount_received\": 0,\n" +
            "\"amount_returned\": 0\n" +
            "},\n" +
            "\"status\": \"pending\",\n" +
            "\"type\": \"card\",\n" +
            "\"usage\": \"single_use\",\n" +
            "\"card\": " + EXAMPLE_JSON_SOURCE_CARD_DATA + "\n" +
            "}"
    }
}
