package com.stripe.android.paymentsheet.navigation

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class PaymentSheetScreenLoadingTest {
    @Test
    fun `title returns null`() = runTest {
        assertThat(
            PaymentSheetScreen.Loading.title(isCompleteFlow = false, isWalletEnabled = false)
        ).isNull()
    }
}
