package com.stripe.android.paymentsheet.navigation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.R
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.FakeEditPaymentMethodInteractor
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class PaymentSheetScreenEditPaymentMethodTest {
    @Test
    fun `title returns update card`() = runTest {
        PaymentSheetScreen.EditPaymentMethod(
            interactor = FakeEditPaymentMethodInteractor(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        ).title(isCompleteFlow = true, isWalletEnabled = true).test {
            assertThat(awaitItem()).isEqualTo(R.string.stripe_title_update_card.resolvableString)
        }
    }
}
