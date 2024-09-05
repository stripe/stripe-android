package com.stripe.android.paymentsheet.navigation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.FakeAddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.FakeAddPaymentMethodInteractor.Companion.createState
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import com.stripe.android.R as PaymentsCoreR

internal class PaymentSheetScreenAddAnotherPaymentMethodTest {
    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Test
    fun `title returns null when isWalletEnabled`() = runTest {
        val interactor = FakeAddPaymentMethodInteractor(createState())
        PaymentSheetScreen.AddAnotherPaymentMethod(interactor)
            .title(isCompleteFlow = false, isWalletEnabled = true).test {
                assertThat(awaitItem()).isNull()
            }
    }

    @Test
    fun `title returns null when isCompleteFlow`() = runTest {
        val interactor = FakeAddPaymentMethodInteractor(createState())
        PaymentSheetScreen.AddAnotherPaymentMethod(interactor)
            .title(isCompleteFlow = true, isWalletEnabled = false).test {
                assertThat(awaitItem()).isNull()
            }
    }

    @Test
    fun `title returns add card with one supported payment method`() = runTest {
        val state = createState(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card")
                )
            )
        )
        val interactor = FakeAddPaymentMethodInteractor(state)
        PaymentSheetScreen.AddAnotherPaymentMethod(interactor)
            .title(isCompleteFlow = false, isWalletEnabled = false).test {
                assertThat(awaitItem()).isEqualTo(PaymentsCoreR.string.stripe_title_add_a_card.resolvableString)
            }
    }

    @Test
    fun `title returns choose payment method with non card one supported payment method`() = runTest {
        val state = createState(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("us_bank_account"),
                    clientSecret = null,
                ),
            )
        )
        val interactor = FakeAddPaymentMethodInteractor(state)
        PaymentSheetScreen.AddAnotherPaymentMethod(interactor)
            .title(isCompleteFlow = false, isWalletEnabled = false).test {
                assertThat(awaitItem()).isEqualTo(R.string.stripe_paymentsheet_choose_payment_method.resolvableString)
            }
    }

    @Test
    fun `title returns choose payment method with more than one supported payment method`() = runTest {
        val interactor = FakeAddPaymentMethodInteractor(createState())
        PaymentSheetScreen.AddAnotherPaymentMethod(interactor)
            .title(isCompleteFlow = false, isWalletEnabled = false).test {
                assertThat(awaitItem()).isEqualTo(R.string.stripe_paymentsheet_choose_payment_method.resolvableString)
            }
    }
}
