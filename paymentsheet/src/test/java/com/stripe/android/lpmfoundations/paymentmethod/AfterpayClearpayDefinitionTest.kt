package com.stripe.android.lpmfoundations.paymentmethod

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.testing.LocaleTestRule
import com.stripe.android.ui.core.R
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class AfterpayClearpayDefinitionTest {
    @get:Rule
    val localeRule = LocaleTestRule()

    @Test
    fun `Test label for afterpay show correctly when clearpay string`() {
        testDisplayNameReflectsLocale(Locale.UK, R.string.stripe_paymentsheet_payment_method_clearpay)
    }

    @Test
    fun `Test label for afterpay show correctly when afterpay string`() {
        testDisplayNameReflectsLocale(Locale.US, R.string.stripe_paymentsheet_payment_method_afterpay)
    }

    private fun testDisplayNameReflectsLocale(locale: Locale, resourceId: Int) {
        localeRule.setTemporarily(locale)
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_WITH_SHIPPING.copy(
                paymentMethodTypes = listOf("card", "afterpay_clearpay"),
            )
        )
        val supportedPaymentMethod = paymentMethodMetadata.supportedPaymentMethodForCode(
            code = "afterpay_clearpay",
        )!!
        assertThat(supportedPaymentMethod.displayNameResource)
            .isEqualTo(resourceId)
    }
}
