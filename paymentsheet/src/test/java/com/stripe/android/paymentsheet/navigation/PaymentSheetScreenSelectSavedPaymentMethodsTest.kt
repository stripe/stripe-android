package com.stripe.android.paymentsheet.navigation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock

internal class PaymentSheetScreenSelectSavedPaymentMethodsTest {
    @Test
    fun `title returns null when isCompleteFlow and isWalletEnabled`() = runTest {
        PaymentSheetScreen.SelectSavedPaymentMethods(mock()).title(isCompleteFlow = true, isWalletEnabled = true).test {
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `title returns select payment method when isCompleteFlow false`() = runTest {
        PaymentSheetScreen.SelectSavedPaymentMethods(mock())
            .title(isCompleteFlow = false, isWalletEnabled = true).test {
                assertThat(awaitItem()).isEqualTo(
                    R.string.stripe_paymentsheet_select_your_payment_method.resolvableString
                )
            }
    }

    @Test
    fun `title returns select payment method when isWalletEnabled is false`() = runTest {
        PaymentSheetScreen.SelectSavedPaymentMethods(mock())
            .title(isCompleteFlow = true, isWalletEnabled = false).test {
                assertThat(awaitItem()).isEqualTo(
                    R.string.stripe_paymentsheet_select_your_payment_method.resolvableString
                )
            }
    }
}
