package com.stripe.android.view

import com.stripe.android.PaymentAuthWebViewStarter
import com.stripe.android.PaymentController
import com.stripe.android.StripeIntentResult
import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentAuthWebViewActivityViewModelTest {
    @Test
    fun cancelIntentSource() {
        val viewModel = PaymentAuthWebViewActivityViewModel(
            PaymentAuthWebViewStarter.Args(
                clientSecret = "client_secret",
                url = "https://example.com"
            )
        )

        val intent = requireNotNull(viewModel.cancelIntentSource().value)

        assertEquals(
            StripeIntentResult.Outcome.CANCELED,
            PaymentController.Result.fromIntent(intent)?.flowOutcome
        )
    }

    @Test
    fun toolbarBackgroundColor_returnsCorrectValue() {
        val viewModel = PaymentAuthWebViewActivityViewModel(
            PaymentAuthWebViewStarter.Args(
                clientSecret = "client_secret",
                url = "https://example.com",
                toolbarCustomization = StripeToolbarCustomization().apply {
                    setBackgroundColor("#ffffff")
                }
            )
        )
        assertEquals("#ffffff", viewModel.toolbarBackgroundColor)
    }

    @Test
    fun buttonText_returnsCorrectValue() {
        val viewModel = PaymentAuthWebViewActivityViewModel(
            PaymentAuthWebViewStarter.Args(
                clientSecret = "client_secret",
                url = "https://example.com",
                toolbarCustomization = StripeToolbarCustomization().apply {
                    setButtonText("close")
                }
            )
        )
        assertEquals("close", viewModel.buttonText)
    }

    @Test
    fun toolbarTitle_returnsCorrectValue() {
        val toolbarCustomization = StripeToolbarCustomization().apply {
            setHeaderText("auth webview")
        }
        val viewModel = PaymentAuthWebViewActivityViewModel(
            PaymentAuthWebViewStarter.Args(
                clientSecret = "client_secret",
                url = "https://example.com",
                toolbarCustomization = toolbarCustomization
            )
        )
        assertEquals(PaymentAuthWebViewActivityViewModel.ToolbarTitleData(
            "auth webview",
            toolbarCustomization
        ), viewModel.toolbarTitle)
    }
}
