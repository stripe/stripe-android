package com.stripe.android.model.parsers

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.StripeIntent
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentIntentJsonParserTest {
    @Test
    fun parse_withExpandedPaymentMethod_shouldCreateExpectedObject() {
        val paymentIntent = PaymentIntentJsonParser().parse(
            PaymentIntentFixtures.EXPANDED_PAYMENT_METHOD_JSON
        )
        assertThat(paymentIntent?.paymentMethodId)
            .isEqualTo("pm_1GSTxOCRMbs6FrXfYCosDqyr")
        assertThat(paymentIntent?.paymentMethod?.id)
            .isEqualTo("pm_1GSTxOCRMbs6FrXfYCosDqyr")
    }

    @Test
    fun parse_withShipping_shouldCreateExpectedObject() {
        val paymentIntent = PaymentIntentJsonParser().parse(
            PaymentIntentFixtures.PI_WITH_SHIPPING_JSON
        )

        assertThat(paymentIntent?.shipping)
            .isEqualTo(
                PaymentIntent.Shipping(
                    address = Address(
                        line1 = "123 Market St",
                        line2 = "#345",
                        city = "San Francisco",
                        state = "CA",
                        postalCode = "94107",
                        country = "US"
                    ),
                    carrier = "UPS",
                    name = "Jenny Rosen",
                    phone = "1-800-555-1234",
                    trackingNumber = "12345"
                )
            )
    }

    @Test
    fun parse_withOxxo_shouldCreateExpectedNextActionData() {
        val paymentIntent = PaymentIntentJsonParser().parse(
            PaymentIntentFixtures.OXXO_REQUIRES_ACTION
        )
        assertThat(paymentIntent?.nextActionData)
            .isEqualTo(
                StripeIntent.NextActionData.DisplayOxxoDetails(
                    expiresAfter = 1587704399,
                    number = "12345678901234657890123456789012"
                )
            )
    }

    @Test
    fun parse_withAlipayAction_shoulddCreateExpectedNextActionData() {
        val paymentIntent = PaymentIntentJsonParser().parse(
            PaymentIntentFixtures.ALIPAY_REQUIRES_ACTION
        )
        assertThat(paymentIntent?.nextActionData)
            .isEqualTo(
                StripeIntent.NextActionData.RedirectToUrl(
                    Uri.parse("https://hooks.stripe.com/redirect/authenticate/src_1GiUlyHSL10J9wqvLZKrtWo3?client_secret=src_client_secret_JjkxntbeO885UyGjnwqjVDwI"),
                    "example://return_url",
                    StripeIntent.NextActionData.RedirectToUrl.MobileData.Alipay("alipay_sdk_data")
                )
            )
    }
}
