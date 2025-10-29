@file:OptIn(PaymentMethodMessagingElementPreview::class)

package com.stripe.android.paymentmethodmessaging.element

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.stripe.android.model.PaymentMethodMessage
import com.stripe.android.model.PaymentMethodMessageImage
import com.stripe.android.model.PaymentMethodMessageLearnMore
import com.stripe.android.model.PaymentMethodMessageMultiPartner
import com.stripe.android.model.PaymentMethodMessageSinglePartner
import com.stripe.android.paymentmethodmessaging.R
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test

class SinglePartnerContentScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule()

    @OptIn(ExperimentalCoroutinesApi::class)
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun singlePartnerLight() {
        paparazziRule.snapshot {
            val content = PaymentMethodMessagingContent.get(
                getSinglePartner(
                    message = "Buy stuff in increments with {partner}"
                )
            )
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
            )
            content.Content(appearance = darkAppearance.build())
        }
    }

    @Test
    fun singlePartnerFlat() {
        paparazziRule.snapshot {
            val content = PaymentMethodMessagingContent.get(
                getSinglePartner(
                    message = "Buy stuff in increments with {partner}"
                )
            )
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
            )
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
            )
            content.Content(crazyAppearance.build())
        }
    }

    @Test
    fun multiPartnerLight() {
        paparazziRule.snapshot {
            val content = PaymentMethodMessagingContent.get(
                getMultiPartner("Buy stuff in increments of money")
            )
            content.Content(PaymentMethodMessagingElement.Appearance().build())
        }
    }

    @Test
    fun multiPartnerDark() {
        paparazziRule.snapshot {
            val content = PaymentMethodMessagingContent.get(
                getMultiPartner("Buy stuff in increments of money")
            )
            content.Content(darkAppearance.build())
        }
    }

    @Test
    fun multiPartnerFlat() {
        paparazziRule.snapshot {
            val content = PaymentMethodMessagingContent.get(
                getMultiPartner("Buy stuff in increments of money")
            )
            content.Content(flatAppearance.build())
        }
    }

    @Test
    fun multiPartnerLongMessage() {
        paparazziRule.snapshot {
            val content = PaymentMethodMessagingContent.get(
                getMultiPartner("Buyyyyyyyyyyyyyy stufffffffffffffffff innnnnn incrementssssss of moneyyyyyyyyy")
            )
            content.Content(PaymentMethodMessagingElement.Appearance().build())
        }
    }

    @Test
    fun multiPartnerCrazy() {
        paparazziRule.snapshot {
            val content = PaymentMethodMessagingContent.get(
                getMultiPartner("Buy stuff in increments of money")
            )
            content.Content(crazyAppearance.build())
        }
    }

    private fun getSinglePartner(message: String): PaymentMethodMessage {
        return PaymentMethodMessage(
            paymentMethods = listOf(),
            singlePartner = PaymentMethodMessageSinglePartner(
                inlinePartnerPromotion = message,
                lightImage = klarnaLightImage,
                darkImage = klarnaDarkImage,
                flatImage = klarnaFlatImage,
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
                lightImages = listOf(klarnaLightImage, affirmLightImage, afterpayLightImage),
                darkImages = listOf(klarnaDarkImage, affirmDarkImage, afterpayDarkImage),
                flatImages = listOf(klarnaFlatImage, affirmFlatImage, afterpayFlatImage),
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
        private const val KLARNA_LIGHT_URL =
            "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-logo.png"
        private const val KLARNA_DARK_URL =
            "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-logo-dark.png"
        private const val KLARNA_FLAT_URL =
            "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-logo-flat.png"

        private const val AFFIRM_LIGHT_URL =
            "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/affirm-logo.png"
        private const val AFFIRM_DARK_URL =
            "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/affirm-logo-dark.png"
        private const val AFFIRM_FLAT_URL =
            "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/affirm-logo-flat.png"

        private const val AFTERPAY_LIGHT_URL =
            "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/cashapp-afterpay-logo.png"
        private const val AFTERPAY_DARK_URL =
            "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/cashapp-afterpay-logo-dark.png"
        private const val AFTERPAY_FLAT_URL =
            "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/cashapp-afterpay-logo-flat.png"

        val klarnaLightImage = PaymentMethodMessageImage(
            role = "logo",
            url = KLARNA_LIGHT_URL,
            paymentMethodType = "klarna",
            text = "klarna"
        )

        val klarnaDarkImage = PaymentMethodMessageImage(
            role = "logo",
            url = KLARNA_DARK_URL,
            paymentMethodType = "klarna",
            text = "klarna"
        )

        val klarnaFlatImage = PaymentMethodMessageImage(
            role = "logo",
            url = KLARNA_FLAT_URL,
            paymentMethodType = "klarna",
            text = "klarna"
        )

        val affirmLightImage = PaymentMethodMessageImage(
            role = "logo",
            url = AFFIRM_LIGHT_URL,
            paymentMethodType = "affirm",
            text = "affirm"
        )

        val affirmDarkImage = PaymentMethodMessageImage(
            role = "logo",
            url = AFFIRM_DARK_URL,
            paymentMethodType = "affirm",
            text = "affirm"
        )

        val affirmFlatImage = PaymentMethodMessageImage(
            role = "logo",
            url = AFFIRM_FLAT_URL,
            paymentMethodType = "affirm",
            text = "affirm"
        )

        val afterpayLightImage = PaymentMethodMessageImage(
            role = "logo",
            url = AFTERPAY_LIGHT_URL,
            paymentMethodType = "afterpay_clearpay",
            text = "Cash App Afterpay"
        )

        val afterpayDarkImage = PaymentMethodMessageImage(
            role = "logo",
            url = AFTERPAY_DARK_URL,
            paymentMethodType = "afterpay_clearpay",
            text = "Cash App Afterpay"
        )

        val afterpayFlatImage = PaymentMethodMessageImage(
            role = "logo",
            url = AFTERPAY_FLAT_URL,
            paymentMethodType = "afterpay_clearpay",
            text = "Cash App Afterpay"
        )
    }
}
