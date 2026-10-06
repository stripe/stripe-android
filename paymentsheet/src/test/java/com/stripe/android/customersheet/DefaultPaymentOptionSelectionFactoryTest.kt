package com.stripe.android.customersheet

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_METHOD
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.DefaultPaymentOptionFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.FakeStripeImageLoader
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DefaultPaymentOptionSelectionFactoryTest {
    @Test
    fun `creates saved payment method selection`() = runScenario {
        val selection = factory.create(
            selection = PaymentSelection.Saved(CARD_PAYMENT_METHOD),
            canUseGooglePay = false,
            appearance = PaymentSheet.Appearance(),
        ) as PaymentOptionSelection.PaymentMethod

        assertThat(selection.paymentMethod).isEqualTo(CARD_PAYMENT_METHOD)
        assertThat(selection.paymentOption.paymentMethodType).isEqualTo("card")
        assertThat(selection.paymentOption.label).isEqualTo("\u2066···· 4242\u2069")
    }

    @Test
    fun `creates Google Pay selection when enabled`() = runScenario {
        val selection = factory.create(
            selection = PaymentSelection.GooglePay,
            canUseGooglePay = true,
            appearance = PaymentSheet.Appearance(),
        ) as PaymentOptionSelection.GooglePay

        assertThat(selection.paymentOption.paymentMethodType).isEqualTo("google_pay")
        assertThat(selection.paymentOption.label).isEqualTo("Google Pay")
    }

    @Test
    fun `returns null for Google Pay when disabled`() = runScenario {
        val selection = factory.create(
            selection = PaymentSelection.GooglePay,
            canUseGooglePay = false,
            appearance = PaymentSheet.Appearance(),
        )

        assertThat(selection).isNull()
    }

    @Test
    fun `returns null for null selection`() = runScenario {
        val selection = factory.create(
            selection = null,
            canUseGooglePay = true,
            appearance = PaymentSheet.Appearance(),
        )

        assertThat(selection).isNull()
    }

    private fun runScenario(block: Scenario.() -> Unit) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val imageLoader = FakeStripeImageLoader()
        val factory = DefaultPaymentOptionSelectionFactory(
            paymentOptionFactory = DefaultPaymentOptionFactory(
                iconLoader = PaymentSelection.IconLoader(
                    resources = context.resources,
                    imageLoader = imageLoader,
                ),
                cardArtDrawableLoader = { null },
                context = context,
            ),
        )

        Scenario(factory).block()

        imageLoader.ensureAllEventsConsumed()
    }

    private class Scenario(val factory: DefaultPaymentOptionSelectionFactory)
}
