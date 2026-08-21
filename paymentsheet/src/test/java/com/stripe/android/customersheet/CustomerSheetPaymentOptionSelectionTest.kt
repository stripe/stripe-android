package com.stripe.android.customersheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.customersheet.CustomerSheet.Companion.toPaymentOptionSelection
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlin.test.Test

@OptIn(AppearanceAPIAdditionsPreview::class)
internal class CustomerSheetPaymentOptionSelectionTest {

    @Test
    fun `saved selection forwards appearance to payment option factory`() = runScenario(
        selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
    ) {
        assertThat(result).isInstanceOf(PaymentOptionSelection.PaymentMethod::class.java)
        assertThat(result?.paymentOption).isSameInstanceAs(paymentOptionFactory.paymentOption)
        assertCreateCall()
    }

    @Test
    fun `Google Pay selection forwards appearance to payment option factory`() = runScenario(
        selection = PaymentSelection.GooglePay,
    ) {
        assertThat(result).isInstanceOf(PaymentOptionSelection.GooglePay::class.java)
        assertThat(result?.paymentOption).isSameInstanceAs(paymentOptionFactory.paymentOption)
        assertCreateCall()
    }

    private fun runScenario(
        selection: PaymentSelection,
        test: Scenario.() -> Unit,
    ) {
        val appearance = PaymentSheet.Appearance(
            themeMode = PaymentSheet.ThemeMode.AlwaysDark,
        )
        val paymentOptionFactory = FakeCustomerSheetPaymentOptionFactory()
        val result = selection.toPaymentOptionSelection(
            paymentOptionFactory = paymentOptionFactory,
            appearance = appearance,
            canUseGooglePay = true,
        )

        Scenario(
            selection = selection,
            appearance = appearance,
            paymentOptionFactory = paymentOptionFactory,
            result = result,
        ).test()
    }

    private data class Scenario(
        val selection: PaymentSelection,
        val appearance: PaymentSheet.Appearance,
        val paymentOptionFactory: FakeCustomerSheetPaymentOptionFactory,
        val result: PaymentOptionSelection?,
    ) {
        fun assertCreateCall() {
            assertThat(paymentOptionFactory.createCalls).hasSize(1)
            assertThat(paymentOptionFactory.createCalls.single().selection).isSameInstanceAs(selection)
            assertThat(paymentOptionFactory.createCalls.single().appearance).isSameInstanceAs(appearance)
        }
    }
}
