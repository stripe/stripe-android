package com.stripe.android.paymentsheet.navigation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock

internal class PaymentSheetScreenVerticalModeTest {
    @Test
    fun `title returns null when isWalletEnabled`() = runTest {
        PaymentSheetScreen.VerticalMode(mock()).title(isCompleteFlow = true, isWalletEnabled = true).test {
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `title returns select payment method when isCompleteFlow=true`() = runTest {
        PaymentSheetScreen.VerticalMode(mock()).title(isCompleteFlow = true, isWalletEnabled = false).test {
            assertThat(awaitItem()).isEqualTo(resolvableString(R.string.stripe_paymentsheet_select_payment_method))
        }
    }

    @Test
    fun `title returns choose payment method when isCompleteFlow=false`() = runTest {
        PaymentSheetScreen.VerticalMode(mock()).title(isCompleteFlow = false, isWalletEnabled = false).test {
            assertThat(awaitItem()).isEqualTo(resolvableString(R.string.stripe_paymentsheet_choose_payment_method))
        }
    }
}
