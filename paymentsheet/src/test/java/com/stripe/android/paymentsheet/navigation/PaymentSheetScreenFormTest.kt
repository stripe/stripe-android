package com.stripe.android.paymentsheet.navigation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock

internal class PaymentSheetScreenFormTest {
    @Test
    fun `title returns null`() = runTest {
        PaymentSheetScreen.VerticalModeForm(mock()).title(isCompleteFlow = true, isWalletEnabled = true).test {
            assertThat(awaitItem()).isNull()
        }
    }
}
