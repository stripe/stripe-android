@file:OptIn(PaymentMethodMessagingElementPreview::class)

package com.stripe.android.paymentmethodmessaging.element

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.stripe.android.model.PaymentMethodMessage
import com.stripe.android.model.PaymentMethodMessageImage
import com.stripe.android.model.PaymentMethodMessageLearnMore
import com.stripe.android.model.PaymentMethodMessageMultiPartner
import com.stripe.android.model.PaymentMethodMessageSinglePartner
import com.stripe.android.paymentmethodmessaging.R
import com.stripe.android.screenshottesting.PaparazziRule
import org.junit.Rule
import org.junit.Test

class PaymentMethodMessagingContentScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule()

    @Test
    fun singlePartnerLight() {
        paparazziRule.snapshot {
            val content = PaymentMethodMessagingContent.get(
                getSinglePartner(
                    message = "Buy stuff in increments with {partner}"
                )
            ) { _, _, _ -> }
            content.Content(PaymentMethodMessagingElement.Appearance().build())
        }
    }

    @Test
    fun singlePartnerDark() {
        paparazziRule.snapshot {
            val content = PaymentMethodMessagingContent.get(
                getSinglePartner(
                    message = "Buy stuff in increments with {partner}"
                )
            ) { _, _, _ -> }
            Box(Modifier.background(Color.Black)) {
                content.Content(appearance = darkAppearance.build())
            }
        }
    }

    @Test
    fun singlePartnerFlat() {
        paparazziRule.snapshot {
            val content = PaymentMethodMessagingContent.get(
                getSinglePartner(
                    message = "Buy stuff in increments with {partner}"
                )
            ) { _, _, _ -> }
            content.Content(flatAppearance.build())
        }
    }

    @Test
    fun singlePartnerLongMessage() {
        paparazziRule.snapshot {
            val content = PaymentMethodMessagingContent.get(
                getSinglePartner(
                    message = "This is a lonnnnnnngggggggg messsssaaaaaaaaaaaaage forrrrrrrrrrrrrrrrrrrrrrrr {partner}"
                )
            ) { _, _, _ -> }
            content.Content(PaymentMethodMessagingElement.Appearance().build())
        }
    }

    @Test
    fun singlePartnerCrazy() {
        paparazziRule.snapshot {
            val content = PaymentMethodMessagingContent.get(
                getSinglePartner(
                    message = "Buy stuff in increments with {partner}"
                )
            ) { _, _, _ -> }
            content.Content(crazyAppearance.build())
        }
    }

    @Test
    fun multiPartnerLight() {
        paparazziRule.snapshot {
            val content = PaymentMethodMessagingContent.get(
                getMultiPartner("Buy stuff in increments of money")
            ) { _, _, _ -> }
            content.Content(PaymentMethodMessagingElement.Appearance().build())
        }
    }

    @Test
    fun multiPartnerDark() {
        paparazziRule.snapshot {
            val content = PaymentMethodMessagingContent.get(
                getMultiPartner("Buy stuff in increments of money")
            ) { _, _, _ -> }
            Box(Modifier.background(Color.Black)) {
                content.Content(darkAppearance.build())
            }
        }
    }

    @Test
    fun multiPartnerFlat() {
        paparazziRule.snapshot {
            val content = PaymentMethodMessagingContent.get(
                getMultiPartner("Buy stuff in increments of money")
            ) { _, _, _ -> }
            content.Content(flatAppearance.build())
        }
    }

    @Test
    fun multiPartnerLongMessage() {
        paparazziRule.snapshot {
            val content = PaymentMethodMessagingContent.get(
                getMultiPartner("Buyyyyyyyyyyyyyy stufffffffffffffffff innnnnn incrementssssss of moneyyyyyyyyy")
            ) { _, _, _ -> }
            content.Content(PaymentMethodMessagingElement.Appearance().build())
        }
    }

    @Test
    fun multiPartnerCrazy() {
        paparazziRule.snapshot {
            val content = PaymentMethodMessagingContent.get(
                getMultiPartner("Buy stuff in increments of money")
            ) { _, _, _ -> }
            content.Content(crazyAppearance.build())
        }
    }

    private fun getSinglePartner(message: String): PaymentMethodMessage {
        return PaymentMethodMessage(
            paymentMethods = listOf(),
            singlePartner = PaymentMethodMessageSinglePartner(
                inlinePartnerPromotion = message,
                lightImage = PaymentMethodMessageImage("", "", "", ""),
                darkImage = PaymentMethodMessageImage("", "", "", ""),
                flatImage = PaymentMethodMessageImage("", "", "", ""),
                learnMore = PaymentMethodMessageLearnMore(
                    url = "",
                    message = ""
                )
            ),
            multiPartner = null
        )
    }

    private fun getMultiPartner(message: String): PaymentMethodMessage {
        return PaymentMethodMessage(
            paymentMethods = listOf(),
            singlePartner = null,
            multiPartner = PaymentMethodMessageMultiPartner(
                promotion = message,
                lightImages = listOf(),
                darkImages = listOf(),
                flatImages = listOf(),
                learnMore = PaymentMethodMessageLearnMore(
                    url = "",
                    message = ""
                )
            )
        )
    }

    companion object {
        val darkAppearance = PaymentMethodMessagingElement.Appearance()
            .theme(PaymentMethodMessagingElement.Appearance.Theme.DARK)
            .colors(
                PaymentMethodMessagingElement.Appearance.Colors()
                    .textColor(Color.White.toArgb())
                    .infoIconColor(Color.White.toArgb())
            )

        val flatAppearance = PaymentMethodMessagingElement.Appearance()
            .theme(PaymentMethodMessagingElement.Appearance.Theme.FLAT)

        val crazyAppearance = PaymentMethodMessagingElement.Appearance()
            .font(
                PaymentMethodMessagingElement.Appearance.Font()
                    .fontFamily(R.font.cursive)
                    .fontSizeSp(32f)
                    .fontWeight(600)
                    .letterSpacingSp(12f)
            )
            .colors(
                PaymentMethodMessagingElement.Appearance.Colors()
                    .infoIconColor(Color.Cyan.toArgb())
                    .textColor(Color.Green.toArgb())
            )
    }
}
