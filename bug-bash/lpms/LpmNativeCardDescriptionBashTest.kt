package com.stripe.android.paymentsheet

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class LpmNativeCardDescriptionBashTest {
    private val context: Context = getApplicationContext()

    @Test
    fun `captured Visa exposes its supported brand name`() {
        val method = PaymentMethodJsonParser().parse(
            JSONObject(
                """{
                    "id":"pm_bash_visa","type":"card",
                    "card":{"brand":"visa","display_brand":"visa","last4":"4242"}
                }"""
            )
        )
        assertThat(method.card?.brand).isEqualTo(CardBrand.Visa)

        val description = DisplayableSavedPaymentMethod.create(
            displayName = "unused".resolvableString,
            paymentMethod = method,
        ).getDescription().resolve(context)

        assertThat(description).isEqualTo("Visa ending in 4242")
    }

    @Test
    fun `captured Amex exposes its supported brand name`() {
        val method = PaymentMethodJsonParser().parse(
            JSONObject(
                """{
                    "id":"pm_bash_amex","type":"card",
                    "card":{"brand":"amex","display_brand":"american_express","last4":"8431"}
                }"""
            )
        )
        assertThat(method.card?.brand).isEqualTo(CardBrand.AmericanExpress)
        assertThat(method.card?.displayBrand).isEqualTo("american_express")

        val description = DisplayableSavedPaymentMethod.create(
            displayName = "unused".resolvableString,
            paymentMethod = method,
        ).getDescription().resolve(context)

        assertThat(description).isEqualTo("American Express ending in 8431")
    }
}
