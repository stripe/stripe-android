package com.stripe.android.paymentsheet.navigation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock

internal class PaymentSheetScreenManageOneSavedPaymentMethodTest {
    @Test
    fun `title returns remove payment method`() = runTest {
        PaymentSheetScreen.ManageOneSavedPaymentMethod(mock()).title(isCompleteFlow = true, isWalletEnabled = true).test {
            assertThat(awaitItem()).isEqualTo(resolvableString(R.string.stripe_paymentsheet_remove_pm_title))
        }
    }
}
