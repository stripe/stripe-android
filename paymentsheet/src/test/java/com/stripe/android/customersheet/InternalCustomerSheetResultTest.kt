package com.stripe.android.customersheet

import android.graphics.Color
import android.graphics.drawable.ShapeDrawable
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_METHOD
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class InternalCustomerSheetResultTest {
    @Test
    fun `Selected passes saved payment method and appearance to factory`() = runScenario {
        val selection = PaymentSelection.Saved(CARD_PAYMENT_METHOD)
        val result = InternalCustomerSheetResult.Selected(
            paymentSelection = selection,
            appearance = CUSTOM_APPEARANCE,
        ).toPublicResult(factory) as CustomerSheetResult.Selected

        assertThat(result.selection).isSameInstanceAs(factory.result)
        assertCreateCall(selection)
    }

    @Test
    fun `Canceled passes saved payment method and appearance to factory`() = runScenario {
        val selection = PaymentSelection.Saved(CARD_PAYMENT_METHOD)
        val result = InternalCustomerSheetResult.Canceled(
            paymentSelection = selection,
            appearance = CUSTOM_APPEARANCE,
        ).toPublicResult(factory) as CustomerSheetResult.Canceled

        assertThat(result.selection).isSameInstanceAs(factory.result)
        assertCreateCall(selection)
    }

    @Test
    fun `Selected passes Google Pay and appearance to factory`() = runScenario {
        val result = InternalCustomerSheetResult.Selected(
            paymentSelection = PaymentSelection.GooglePay,
            appearance = CUSTOM_APPEARANCE,
        ).toPublicResult(factory) as CustomerSheetResult.Selected

        assertThat(result.selection).isSameInstanceAs(factory.result)
        assertCreateCall(PaymentSelection.GooglePay)
    }

    @Test
    fun `Canceled passes Google Pay and appearance to factory`() = runScenario {
        val result = InternalCustomerSheetResult.Canceled(
            paymentSelection = PaymentSelection.GooglePay,
            appearance = CUSTOM_APPEARANCE,
        ).toPublicResult(factory) as CustomerSheetResult.Canceled

        assertThat(result.selection).isSameInstanceAs(factory.result)
        assertCreateCall(PaymentSelection.GooglePay)
    }

    @Test
    fun `Selected passes null selection and appearance to factory`() = runScenario(factoryResult = null) {
        val result = InternalCustomerSheetResult.Selected(
            paymentSelection = null,
            appearance = CUSTOM_APPEARANCE,
        ).toPublicResult(factory) as CustomerSheetResult.Selected

        assertThat(result.selection).isNull()
        assertCreateCall(null)
    }

    @Test
    fun `Canceled passes null selection and appearance to factory`() = runScenario(factoryResult = null) {
        val result = InternalCustomerSheetResult.Canceled(
            paymentSelection = null,
            appearance = CUSTOM_APPEARANCE,
        ).toPublicResult(factory) as CustomerSheetResult.Canceled

        assertThat(result.selection).isNull()
        assertCreateCall(null)
    }

    @Test
    fun `Error preserves exception without creating a payment option selection`() = runScenario {
        val exception = IllegalStateException("Unable to load customer")

        val result = InternalCustomerSheetResult.Error(exception)
            .toPublicResult(factory) as CustomerSheetResult.Failed

        assertThat(result.exception).isSameInstanceAs(exception)
        factory.createCalls.expectNoEvents()
    }

    private fun runScenario(
        factoryResult: PaymentOptionSelection? = PAYMENT_OPTION_SELECTION,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val factory = FakePaymentOptionSelectionFactory(result = factoryResult)

        Scenario(factory).block()

        factory.ensureAllEventsConsumed()
    }

    private class Scenario(val factory: FakePaymentOptionSelectionFactory) {
        suspend fun assertCreateCall(selection: PaymentSelection?) {
            val call = factory.createCalls.awaitItem()
            assertThat(call.selection).isEqualTo(selection)
            assertThat(call.canUseGooglePay).isTrue()
            assertThat(call.appearance).isEqualTo(CUSTOM_APPEARANCE)
        }
    }

    private companion object {
        val CUSTOM_APPEARANCE = PaymentSheet.Appearance(
            colorsLight = PaymentSheet.Colors.Builder.light().component(Color.BLACK).build(),
        )
        val PAYMENT_OPTION_SELECTION = PaymentOptionSelection.PaymentMethod(
            paymentMethod = CARD_PAYMENT_METHOD,
            paymentOption = PaymentOption(
                drawableResourceId = 0,
                label = "Converted payment option",
                paymentMethodType = "card",
                billingDetails = null,
                _shippingDetails = null,
                _labels = PaymentOption.Labels(label = "Converted payment option"),
                imageLoader = { ShapeDrawable() },
            ),
        )
    }
}
