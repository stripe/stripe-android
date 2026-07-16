package com.stripe.android.checkout

import com.stripe.android.paymentelement.CheckoutSessionPreview
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.mock
import javax.inject.Provider
import kotlin.test.Test

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutPresenterTest {

    @Test
    fun `confirm delegates to the confirmation helper`() = runTest {
        val confirmationHelper = FakeCheckoutConfirmationHelper()
        val presenter = CheckoutPresenter(
            paymentElementProvider = Provider { mock() },
            currencySelectorElementProvider = Provider { mock() },
            shippingAddressElementProvider = Provider { mock() },
            expressCheckoutElementProvider = Provider { mock() },
            confirmationHelper = confirmationHelper,
        )

        presenter.confirm()

        confirmationHelper.confirmCalls.awaitItem()
        confirmationHelper.ensureAllEventsConsumed()
    }
}
