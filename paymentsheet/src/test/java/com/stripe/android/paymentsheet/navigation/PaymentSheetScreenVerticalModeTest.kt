package com.stripe.android.paymentsheet.navigation

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.verticalmode.FakePaymentMethodVerticalLayoutInteractor
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class PaymentSheetScreenVerticalModeTest {
    private val interactor = FakePaymentMethodVerticalLayoutInteractor.create()

    @Test
    fun `title returns null when isWalletEnabled`() = runTest {
        assertThat(
            PaymentSheetScreen.VerticalMode(interactor).title(isCompleteFlow = true, isWalletEnabled = true)
        ).isNull()
    }

    @Test
    fun `title returns select payment method when isCompleteFlow=true`() = runTest {
        assertThat(
            PaymentSheetScreen.VerticalMode(interactor).title(isCompleteFlow = true, isWalletEnabled = false)
        ).isEqualTo(R.string.stripe_paymentsheet_select_payment_method.resolvableString)
    }

    @Test
    fun `title returns choose payment method when isCompleteFlow=false`() = runTest {
        assertThat(
            PaymentSheetScreen.VerticalMode(interactor).title(isCompleteFlow = false, isWalletEnabled = false)
        ).isEqualTo(R.string.stripe_paymentsheet_choose_payment_method.resolvableString)
    }
}
