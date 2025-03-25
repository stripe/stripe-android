package com.stripe.android.lpmfoundations.paymentmethod

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.ui.core.R
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import com.stripe.android.paymentsheet.R as PaymentSheetR

@RunWith(RobolectricTestRunner::class)
internal class AfterpayClearpayDefinitionTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val res = context.resources

    @Test
    fun `Test label for afterpay show correctly when clearpay string`() {
        testDisplayNameReflectsLocale(
            currency = "GBP",
            titleResourceId = R.string.stripe_paymentsheet_payment_method_clearpay,
            subtitleResourceId = PaymentSheetR.string.stripe_clearpay_subtitle,
        )
    }

    @Test
    fun `Test label for afterpay show correctly when afterpay string`() {
        testDisplayNameReflectsLocale(
            currency = "EUR",
            titleResourceId = R.string.stripe_paymentsheet_payment_method_afterpay,
            subtitleResourceId = PaymentSheetR.string.stripe_afterpay_subtitle,
        )
    }

    @Test
    fun `Test label for afterpay show correctly when cashapp afterpay string`() {
        testDisplayNameReflectsLocale(
            currency = "USD",
            titleResourceId = R.string.stripe_paymentsheet_payment_method_afterpay,
            subtitleResourceId = PaymentSheetR.string.stripe_cashapp_afterpay_subtitle,
        )
    }

    private fun testDisplayNameReflectsLocale(currency: String, titleResourceId: Int, subtitleResourceId: Int) {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_WITH_SHIPPING.copy(
                paymentMethodTypes = listOf("card", "afterpay_clearpay"),
                currency = currency,
            )
        )
        val supportedPaymentMethod = paymentMethodMetadata.supportedPaymentMethodForCode(
            code = "afterpay_clearpay",
        )!!
        assertThat(supportedPaymentMethod.displayName.resolve(context))
            .isEqualTo(res.getString(titleResourceId))
        assertThat(supportedPaymentMethod.subtitle?.resolve(context))
            .isEqualTo(res.getString(subtitleResourceId))
    }
}
