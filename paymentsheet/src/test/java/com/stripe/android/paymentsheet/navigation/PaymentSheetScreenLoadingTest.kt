package com.stripe.android.paymentsheet.navigation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class PaymentSheetScreenLoadingTest {
    @Test
    fun `title returns null`() = runTest {
        PaymentSheetScreen.Loading.title(isCompleteFlow = false, isWalletEnabled = false).test {
            assertThat(awaitItem()).isNull()
        }
    }
}
