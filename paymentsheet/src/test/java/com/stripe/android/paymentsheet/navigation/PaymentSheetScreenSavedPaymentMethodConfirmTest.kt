package com.stripe.android.paymentsheet.navigation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.verticalmode.FakeSavedPaymentMethodConfirmInteractor
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class PaymentSheetScreenSavedPaymentMethodConfirmTest {
    @Test
    fun `close closes interactor`() = runTest {
        val interactor = FakeSavedPaymentMethodConfirmInteractor()
        val screen = PaymentSheetScreen.SavedPaymentMethodConfirm(
            interactor = interactor,
        )

        screen.close()

        assertThat(interactor.closeCalls.awaitItem()).isEqualTo(Unit)
        interactor.validate()
    }

    @Test
    fun `title returns added card`() = runTest {
        val interactor = FakeSavedPaymentMethodConfirmInteractor()
        val screen = PaymentSheetScreen.SavedPaymentMethodConfirm(
            interactor = interactor,
        )

        screen.title(isCompleteFlow = true, isWalletEnabled = false).test {
            assertThat(awaitItem()).isEqualTo(R.string.stripe_paymentsheet_added_card.resolvableString)
        }
        interactor.validate()
    }
}
