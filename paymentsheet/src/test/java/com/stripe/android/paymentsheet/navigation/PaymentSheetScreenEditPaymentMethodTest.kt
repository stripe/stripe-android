package com.stripe.android.paymentsheet.navigation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.R
import com.stripe.android.core.strings.resolvableString
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock

internal class PaymentSheetScreenEditPaymentMethodTest {
    @Test
    fun `title returns update card`() = runTest {
        PaymentSheetScreen.EditPaymentMethod(mock()).title(isCompleteFlow = true, isWalletEnabled = true).test {
            assertThat(awaitItem()).isEqualTo(resolvableString(R.string.stripe_title_update_card))
        }
    }
}
