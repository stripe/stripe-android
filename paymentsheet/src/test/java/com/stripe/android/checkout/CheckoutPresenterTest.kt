package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.PaymentElement
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.content.FakeEmbeddedContentHelper
import dagger.Lazy
import javax.inject.Provider
import kotlin.test.Test

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutPresenterTest {

    @Test
    fun `paymentElement with immediate action stores behavior before retrieving element`() {
        val holder = CheckoutRowSelectionBehaviorHolder()
        val callback = {}
        val presenter = createPresenter(holder)

        presenter.paymentElement(PaymentElement.RowSelectionBehavior.immediateAction(callback))

        assertThat(holder.behavior.immediateActionCallbackOrNull()).isSameInstanceAs(callback)
    }

    @Test
    fun `paymentElement without behavior stores default behavior`() {
        val holder = CheckoutRowSelectionBehaviorHolder().apply {
            behavior = PaymentElement.RowSelectionBehavior.immediateAction {}
        }
        val presenter = createPresenter(holder)

        presenter.paymentElement()

        assertThat(holder.behavior.immediateActionCallbackOrNull()).isNull()
    }

    private fun createPresenter(holder: CheckoutRowSelectionBehaviorHolder): CheckoutPresenter {
        val paymentElement = PaymentElement(FakeEmbeddedContentHelper())
        return CheckoutPresenter(
            paymentElementProvider = Lazy { paymentElement },
            rowSelectionBehaviorHolder = holder,
            currencySelectorElementProvider = Lazy { error("unused") },
            shippingAddressElementProvider = Lazy { error("unused") },
            expressCheckoutElementProvider = Lazy { error("unused") },
            checkoutConfirmationPerformerProvider = Provider { error("unused") },
        )
    }
}
