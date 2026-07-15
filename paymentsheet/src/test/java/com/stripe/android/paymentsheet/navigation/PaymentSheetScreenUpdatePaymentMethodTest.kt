package com.stripe.android.paymentsheet.navigation

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.ui.FakeUpdatePaymentMethodInteractor
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class PaymentSheetScreenUpdatePaymentMethodTest {
    @Test
    fun `screen close delegates to interactor close`() = runTest {
        val interactor = FakeUpdatePaymentMethodInteractor()

        PaymentSheetScreen.UpdatePaymentMethod(interactor).close()

        assertThat(interactor.closeCalls.awaitItem()).isEqualTo(Unit)
    }
}
