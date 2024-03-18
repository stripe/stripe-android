package com.stripe.android.lpmfoundations.paymentmethod

import androidx.test.core.app.ApplicationProvider
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
        localeRule.setTemporarily(Locale.UK)
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_WITH_SHIPPING.copy(
                paymentMethodTypes = listOf("card", "afterpay_clearpay"),
            )
        )
        val supportedPaymentMethod = paymentMethodMetadata.supportedPaymentMethodForCode(
            code = "afterpay_clearpay",
            context = ApplicationProvider.getApplicationContext(),
        )!!
        assertThat(supportedPaymentMethod.displayNameResource)
            .isEqualTo(R.string.stripe_paymentsheet_payment_method_clearpay)
    }

    @Test
    fun `Test label for afterpay show correctly when afterpay string`() {
        localeRule.setTemporarily(Locale.US)
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_WITH_SHIPPING.copy(
                paymentMethodTypes = listOf("card", "afterpay_clearpay"),
            )
        )
        val supportedPaymentMethod = paymentMethodMetadata.supportedPaymentMethodForCode(
            code = "afterpay_clearpay",
            context = ApplicationProvider.getApplicationContext(),
        )!!
        assertThat(supportedPaymentMethod.displayNameResource)
            .isEqualTo(R.string.stripe_paymentsheet_payment_method_afterpay)
    }
}
