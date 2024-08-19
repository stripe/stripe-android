package com.stripe.android.paymentsheet.navigation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.luxe.LpmRepositoryTestHelpers
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInteractor
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import com.stripe.android.R as PaymentsCoreR

internal class PaymentSheetScreenAddFirstPaymentMethodTest {

    @Test
    fun `title returns null when isWalletEnabled`() = runTest {
        val interactor = FakeAddPaymentMethodInteractor(stateFlowOf(createState()))
        PaymentSheetScreen.AddFirstPaymentMethod(interactor)
            .title(isCompleteFlow = false, isWalletEnabled = true).test {
                assertThat(awaitItem()).isNull()
            }
    }

    @Test
    fun `title returns add payment method when isCompleteFlow`() = runTest {
        val interactor = FakeAddPaymentMethodInteractor(stateFlowOf(createState()))
        PaymentSheetScreen.AddFirstPaymentMethod(interactor)
            .title(isCompleteFlow = true, isWalletEnabled = false).test {
                assertThat(awaitItem())
                    .isEqualTo(R.string.stripe_paymentsheet_add_payment_method_title.resolvableString)
            }
    }

    @Test
    fun `title returns add card with one supported payment method`() = runTest {
        val state = createState(supportedPaymentMethods = listOf(LpmRepositoryTestHelpers.card))
        val interactor = FakeAddPaymentMethodInteractor(stateFlowOf(state))
        PaymentSheetScreen.AddFirstPaymentMethod(interactor)
            .title(isCompleteFlow = false, isWalletEnabled = false).test {
                assertThat(awaitItem()).isEqualTo(PaymentsCoreR.string.stripe_title_add_a_card.resolvableString)
            }
    }

    @Test
    fun `title returns choose payment method with non card one supported payment method`() = runTest {
        val state = createState(supportedPaymentMethods = listOf(LpmRepositoryTestHelpers.usBankAccount))
        val interactor = FakeAddPaymentMethodInteractor(stateFlowOf(state))
        PaymentSheetScreen.AddFirstPaymentMethod(interactor)
            .title(isCompleteFlow = false, isWalletEnabled = false).test {
                assertThat(awaitItem()).isEqualTo(R.string.stripe_paymentsheet_choose_payment_method.resolvableString)
            }
    }

    @Test
    fun `title returns choose payment method with more than one supported payment method`() = runTest {
        val interactor = FakeAddPaymentMethodInteractor(stateFlowOf(createState()))
        PaymentSheetScreen.AddFirstPaymentMethod(interactor)
            .title(isCompleteFlow = false, isWalletEnabled = false).test {
                assertThat(awaitItem()).isEqualTo(R.string.stripe_paymentsheet_choose_payment_method.resolvableString)
            }
    }

    private fun createState(
        supportedPaymentMethods: List<SupportedPaymentMethod> = listOf(
            LpmRepositoryTestHelpers.card,
            LpmRepositoryTestHelpers.usBankAccount,
        ),
    ): AddPaymentMethodInteractor.State {
        return AddPaymentMethodInteractor.State(
            selectedPaymentMethodCode = PaymentMethod.Type.Card.code,
            supportedPaymentMethods = supportedPaymentMethods,
            arguments = mock(),
            formElements = emptyList(),
            paymentSelection = null,
            processing = false,
            usBankAccountFormArguments = mock(),
        )
    }
}
