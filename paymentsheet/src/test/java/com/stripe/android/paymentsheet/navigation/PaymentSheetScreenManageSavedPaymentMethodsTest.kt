package com.stripe.android.paymentsheet.navigation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.verticalmode.FakeManageScreenInteractor
import com.stripe.android.paymentsheet.verticalmode.ManageScreenInteractor
import com.stripe.android.testing.PaymentMethodFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class PaymentSheetScreenManageSavedPaymentMethodsTest {
    @Test
    fun `title returns manage payment methods when isEditing=true`() = runTest {
        val state = createState(
            isEditing = true,
            paymentMethods = listOf(
                PaymentMethodFactory.card(random = true),
                PaymentMethodFactory.card(random = true),
                PaymentMethodFactory.usBankAccount(),
                PaymentMethodFactory.sepaDebit(),
            )
        )
        val interactor = FakeManageScreenInteractor(state)
        PaymentSheetScreen.ManageSavedPaymentMethods(interactor)
            .title(isCompleteFlow = true, isWalletEnabled = true).test {
                assertThat(awaitItem()).isEqualTo(R.string.stripe_paymentsheet_manage_payment_methods.resolvableString)
            }
    }

    @Test
    fun `title returns select payment method when isEditing=false`() = runTest {
        val state = createState(
            isEditing = false,
            paymentMethods = listOf(
                PaymentMethodFactory.card(random = true),
                PaymentMethodFactory.card(random = true),
                PaymentMethodFactory.usBankAccount(),
                PaymentMethodFactory.sepaDebit(),
            )
        )
        val interactor = FakeManageScreenInteractor(state)
        PaymentSheetScreen.ManageSavedPaymentMethods(interactor)
            .title(isCompleteFlow = true, isWalletEnabled = true).test {
                assertThat(awaitItem()).isEqualTo(
                    R.string.stripe_paymentsheet_select_payment_method.resolvableString
                )
            }
    }

    @Test
    fun `title returns manage cards when isEditing=true and only cards in state`() = runTest {
        val state = createState(isEditing = true, paymentMethods = PaymentMethodFactory.cards(size = 6))
        val interactor = FakeManageScreenInteractor(state)
        PaymentSheetScreen.ManageSavedPaymentMethods(interactor)
            .title(isCompleteFlow = true, isWalletEnabled = true).test {
                assertThat(awaitItem()).isEqualTo(R.string.stripe_paymentsheet_manage_cards.resolvableString)
            }
    }

    @Test
    fun `title returns select card when isEditing=false and only cards in state`() = runTest {
        val state = createState(isEditing = false, paymentMethods = PaymentMethodFactory.cards(size = 6))
        val interactor = FakeManageScreenInteractor(state)
        PaymentSheetScreen.ManageSavedPaymentMethods(interactor)
            .title(isCompleteFlow = true, isWalletEnabled = true).test {
                assertThat(awaitItem()).isEqualTo(R.string.stripe_paymentsheet_select_card.resolvableString)
            }
    }

    private fun createState(
        paymentMethods: List<PaymentMethod> = emptyList(),
        isEditing: Boolean
    ): ManageScreenInteractor.State {
        return ManageScreenInteractor.State(
            paymentMethods = paymentMethods.map { paymentMethod ->
                DisplayableSavedPaymentMethod.create(
                    displayName = paymentMethod.id?.resolvableString ?: "unknown".resolvableString,
                    paymentMethod = paymentMethod
                )
            },
            currentSelection = null,
            isEditing = isEditing,
            canEdit = true,
        )
    }
}
