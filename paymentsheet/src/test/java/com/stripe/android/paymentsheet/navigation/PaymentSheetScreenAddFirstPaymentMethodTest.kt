package com.stripe.android.paymentsheet.navigation

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

internal class PaymentSheetScreenAddFirstPaymentMethodTest {
    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Test
    fun `title returns null when isWalletEnabled`() = runTest {
        val interactor = FakeAddPaymentMethodInteractor(createState())
        assertThat(
            PaymentSheetScreen.AddFirstPaymentMethod(interactor)
                .title(isCompleteFlow = false, isWalletEnabled = true)
        ).isNull()
    }

    @Test
    fun `title returns add payment method when isCompleteFlow`() = runTest {
        val interactor = FakeAddPaymentMethodInteractor(createState())
        assertThat(
            PaymentSheetScreen.AddFirstPaymentMethod(interactor)
                .title(isCompleteFlow = true, isWalletEnabled = false)
        ).isEqualTo(R.string.stripe_paymentsheet_add_payment_method_title.resolvableString)
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
        assertThat(
            PaymentSheetScreen.AddFirstPaymentMethod(interactor)
                .title(isCompleteFlow = false, isWalletEnabled = false)
        ).isEqualTo(PaymentsCoreR.string.stripe_title_add_a_card.resolvableString)
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
        assertThat(
            PaymentSheetScreen.AddFirstPaymentMethod(interactor)
                .title(isCompleteFlow = false, isWalletEnabled = false)
        ).isEqualTo(R.string.stripe_paymentsheet_choose_payment_method.resolvableString)
    }

    @Test
    fun `title returns choose payment method with more than one supported payment method`() = runTest {
        val interactor = FakeAddPaymentMethodInteractor(createState())
        assertThat(
            PaymentSheetScreen.AddFirstPaymentMethod(interactor)
                .title(isCompleteFlow = false, isWalletEnabled = false)
        ).isEqualTo(R.string.stripe_paymentsheet_choose_payment_method.resolvableString)
    }
}
