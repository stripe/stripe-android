package com.stripe.android.customersheet

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_METHOD
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.FakeStripeImageLoader
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class InternalCustomerSheetResultTest {
    @Test
    fun `Selected converts saved payment method to public result`() = runScenario {
        val result = InternalCustomerSheetResult.Selected(
            paymentSelection = PaymentSelection.Saved(CARD_PAYMENT_METHOD),
            appearance = CUSTOM_APPEARANCE,
        ).toPublicResult(paymentOptionFactory) as CustomerSheetResult.Selected

        val selection = result.selection as PaymentOptionSelection.PaymentMethod
        assertThat(selection.paymentMethod).isEqualTo(CARD_PAYMENT_METHOD)
        assertThat(selection.paymentOption.paymentMethodType).isEqualTo("card")
        assertThat(selection.paymentOption.label).isEqualTo("\u2066···· 4242\u2069")
    }

    @Test
    fun `Canceled converts saved payment method to public result`() = runScenario {
        val result = InternalCustomerSheetResult.Canceled(
            paymentSelection = PaymentSelection.Saved(CARD_PAYMENT_METHOD),
            appearance = CUSTOM_APPEARANCE,
        ).toPublicResult(paymentOptionFactory) as CustomerSheetResult.Canceled

        val selection = result.selection as PaymentOptionSelection.PaymentMethod
        assertThat(selection.paymentMethod).isEqualTo(CARD_PAYMENT_METHOD)
        assertThat(selection.paymentOption.paymentMethodType).isEqualTo("card")
        assertThat(selection.paymentOption.label).isEqualTo("\u2066···· 4242\u2069")
    }

    @Test
    fun `Selected converts Google Pay to public result`() = runScenario {
        val result = InternalCustomerSheetResult.Selected(
            paymentSelection = PaymentSelection.GooglePay,
            appearance = CUSTOM_APPEARANCE,
        ).toPublicResult(paymentOptionFactory) as CustomerSheetResult.Selected

        val selection = result.selection as PaymentOptionSelection.GooglePay
        assertThat(selection.paymentOption.paymentMethodType).isEqualTo("google_pay")
        assertThat(selection.paymentOption.label).isEqualTo("Google Pay")
    }

    @Test
    fun `Canceled converts Google Pay to public result`() = runScenario {
        val result = InternalCustomerSheetResult.Canceled(
            paymentSelection = PaymentSelection.GooglePay,
            appearance = CUSTOM_APPEARANCE,
        ).toPublicResult(paymentOptionFactory) as CustomerSheetResult.Canceled

        val selection = result.selection as PaymentOptionSelection.GooglePay
        assertThat(selection.paymentOption.paymentMethodType).isEqualTo("google_pay")
        assertThat(selection.paymentOption.label).isEqualTo("Google Pay")
    }

    @Test
    fun `Selected preserves null selection in public result`() = runScenario {
        val result = InternalCustomerSheetResult.Selected(
            paymentSelection = null,
            appearance = CUSTOM_APPEARANCE,
        ).toPublicResult(paymentOptionFactory) as CustomerSheetResult.Selected

        assertThat(result.selection).isNull()
    }

    @Test
    fun `Canceled preserves null selection in public result`() = runScenario {
        val result = InternalCustomerSheetResult.Canceled(
            paymentSelection = null,
            appearance = CUSTOM_APPEARANCE,
        ).toPublicResult(paymentOptionFactory) as CustomerSheetResult.Canceled

        assertThat(result.selection).isNull()
    }

    private fun runScenario(block: Scenario.() -> Unit) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val imageLoader = FakeStripeImageLoader()
        val paymentOptionFactory = PaymentOptionFactory(
            iconLoader = PaymentSelection.IconLoader(
                resources = context.resources,
                imageLoader = imageLoader,
            ),
            cardArtDrawableLoader = { null },
            context = context,
        )

        Scenario(paymentOptionFactory).block()

        imageLoader.ensureAllEventsConsumed()
    }

    private class Scenario(val paymentOptionFactory: PaymentOptionFactory)

    private companion object {
        val CUSTOM_APPEARANCE = PaymentSheet.Appearance(
            colorsLight = PaymentSheet.Colors.Builder.light().component(Color.BLACK).build(),
        )
    }
}
