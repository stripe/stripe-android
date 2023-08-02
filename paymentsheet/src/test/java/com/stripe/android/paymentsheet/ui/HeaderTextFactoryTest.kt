package com.stripe.android.paymentsheet.ui

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import org.junit.Test
import com.stripe.android.R as StripeR

class HeaderTextFactoryTest {

    @Test
    fun `Shows the correct header in complete flow and showing wallets`() {
        val resource = HeaderTextFactory(isCompleteFlow = true).create(
            screen = PaymentSheetScreen.SelectSavedPaymentMethods,
            isWalletEnabled = true,
            types = emptyList(),
        )

        assertThat(resource).isNull()
    }

    @Test
    fun `Does not show a header on AddAnotherPaymentMethod screen`() {
        val resource = HeaderTextFactory(isCompleteFlow = true).create(
            screen = PaymentSheetScreen.AddAnotherPaymentMethod,
            isWalletEnabled = true,
            types = emptyList(),
        )

        assertThat(resource).isNull()
    }

    @Test
    fun `Shows the correct header if adding the first payment method in complete flow`() {
        val resource = HeaderTextFactory(isCompleteFlow = true).create(
            screen = PaymentSheetScreen.AddFirstPaymentMethod,
            isWalletEnabled = false,
            types = emptyList(),
        )

        assertThat(resource).isEqualTo(R.string.stripe_paymentsheet_add_payment_method_title)
    }

    @Test
    fun `Does not show a header if adding the first payment method and wallets are available`() {
        val resource = HeaderTextFactory(isCompleteFlow = true).create(
            screen = PaymentSheetScreen.AddFirstPaymentMethod,
            isWalletEnabled = true,
            types = emptyList(),
        )

        assertThat(resource).isNull()
    }

    @Test
    fun `Shows the correct header when displaying saved payment methods in custom flow`() {
        val resource = HeaderTextFactory(isCompleteFlow = false).create(
            screen = PaymentSheetScreen.SelectSavedPaymentMethods,
            isWalletEnabled = true,
            types = emptyList(),
        )

        assertThat(resource).isEqualTo(R.string.stripe_paymentsheet_select_payment_method)
    }

    @Test
    fun `Shows the correct header when only credit card form is shown in custom flow`() {
        val resource = HeaderTextFactory(isCompleteFlow = false).create(
            screen = PaymentSheetScreen.AddFirstPaymentMethod,
            isWalletEnabled = false,
            types = listOf("card"),
        )

        assertThat(resource).isEqualTo(StripeR.string.stripe_title_add_a_card)
    }

    @Test
    fun `Shows the correct header when multiple LPMs are shown in custom flow`() {
        val resource = HeaderTextFactory(isCompleteFlow = false).create(
            screen = PaymentSheetScreen.AddFirstPaymentMethod,
            isWalletEnabled = false,
            types = listOf("card", "not_card"),
        )

        assertThat(resource).isEqualTo(R.string.stripe_paymentsheet_choose_payment_method)
    }
}
