package com.stripe.android.paymentsheet.navigation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.verticalmode.FakeManageScreenInteractor
import com.stripe.android.paymentsheet.verticalmode.ManageScreenInteractor
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class PaymentSheetScreenManageSavedPaymentMethodsTest {
    @Test
    fun `title returns manage payment methods when isEditing=true`() = runTest {
        val interactor = FakeManageScreenInteractor(createState(isEditing = true))
        PaymentSheetScreen.ManageSavedPaymentMethods(interactor)
            .title(isCompleteFlow = true, isWalletEnabled = true).test {
                assertThat(awaitItem()).isEqualTo(R.string.stripe_paymentsheet_manage_payment_methods.resolvableString)
            }
    }

    @Test
    fun `title returns select your payment method when isEditing=false`() = runTest {
        val interactor = FakeManageScreenInteractor(createState(isEditing = false))
        PaymentSheetScreen.ManageSavedPaymentMethods(interactor)
            .title(isCompleteFlow = true, isWalletEnabled = true).test {
                assertThat(awaitItem()).isEqualTo(
                    R.string.stripe_paymentsheet_select_your_payment_method.resolvableString
                )
            }
    }

    private fun createState(isEditing: Boolean): ManageScreenInteractor.State {
        return ManageScreenInteractor.State(
            paymentMethods = emptyList(),
            currentSelection = null,
            isEditing = isEditing,
            canEdit = true,
        )
    }
}
