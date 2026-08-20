package com.stripe.android.paymentsheet.navigation

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.verticalmode.FakeSavedPaymentMethodConfirmInteractor
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class PaymentSheetScreenSavedPaymentMethodConfirmTest {
    @Test
    fun `close closes interactor`() = runTest {
        val interactor = FakeSavedPaymentMethodConfirmInteractor()
        val screen = PaymentSheetScreen.SavedPaymentMethodConfirm(
            interactor = interactor,
            isLiveMode = true,
        )

        screen.close()

        assertThat(interactor.closeCalls.awaitItem()).isEqualTo(Unit)
        interactor.validate()
    }
}
