package com.stripe.android.common.taptoadd.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.taptoadd.TapToAddMode
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.R
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.PaymentMethodFactory.update
import kotlinx.coroutines.test.runTest
import org.junit.Test
import com.stripe.android.ui.core.R as StripeUiCoreR

internal class DefaultTapToAddConfirmationInteractorTest {
    @Test
    fun `state has card brand and last4 from payment method`() = runTest {
        val paymentMethod = PaymentMethodFactory.card().update(
            last4 = "1234",
            brand = CardBrand.MasterCard,
            addCbcNetworks = false,
        )

        val metadata = PaymentMethodMetadataFactory.create(isTapToAddSupported = true)

        val interactor = DefaultTapToAddConfirmationInteractor(
            tapToAddMode = TapToAddMode.Continue,
            paymentMethod = paymentMethod,
            paymentMethodMetadata = metadata,
        )

        interactor.state.test {
            val state = awaitItem()

            assertThat(state.cardBrand).isEqualTo(CardBrand.MasterCard)
            assertThat(state.last4).isEqualTo("1234")
        }
    }

    @Test
    fun `expected state in Complete mode`() = runTest {
        val paymentMethod = PaymentMethodFactory.card(last4 = "4242")

        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                amount = 5099,
            ),
            isTapToAddSupported = true,
        )

        val interactor = DefaultTapToAddConfirmationInteractor(
            tapToAddMode = TapToAddMode.Complete,
            paymentMethod = paymentMethod,
            paymentMethodMetadata = metadata,
        )

        interactor.state.test {
            val state = awaitItem()

            assertThat(state.title).isEqualTo(
                resolvableString(
                    StripeUiCoreR.string.stripe_pay_button_amount,
                    "$50.99"
                )
            )
            assertThat(state.primaryButton.locked).isTrue()
            assertThat(state.primaryButton.label).isEqualTo(
                R.string.stripe_paymentsheet_pay_button_label.resolvableString
            )
            assertThat(state.primaryButton.locked).isTrue()
        }
    }

    @Test
    fun `expected state in Continue mode`() = runTest {
        val paymentMethod = PaymentMethodFactory.card(last4 = "4242")

        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            isTapToAddSupported = true,
        )

        val interactor = DefaultTapToAddConfirmationInteractor(
            tapToAddMode = TapToAddMode.Continue,
            paymentMethod = paymentMethod,
            paymentMethodMetadata = metadata,
        )

        interactor.state.test {
            val state = awaitItem()

            assertThat(state.title)
                .isEqualTo(StripeUiCoreR.string.stripe_continue_button_label.resolvableString)
            assertThat(state.primaryButton.label)
                .isEqualTo(StripeUiCoreR.string.stripe_continue_button_label.resolvableString)
            assertThat(state.primaryButton.locked).isFalse()
        }
    }
}
