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
        PaymentSheetScreen.EditPaymentMethod(
            interactor = mock(),
            isLiveMode = true
        ).title(isCompleteFlow = true, isWalletEnabled = true).test {
            assertThat(awaitItem()).isEqualTo(R.string.stripe_title_update_card.resolvableString)
        }
    }
}
