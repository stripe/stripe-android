package com.stripe.android.view

import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentAuthWebViewStarter
import com.stripe.android.PaymentController
import com.stripe.android.StripeIntentResult
import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class PaymentAuthWebViewActivityViewModelTest {
    @Test
    fun cancellationResult() {
        val viewModel = PaymentAuthWebViewActivityViewModel(
            PaymentAuthWebViewStarter.Args(
                clientSecret = "client_secret",
                url = "https://example.com",
                shouldCancelSource = true
            )
        )

        val intent = viewModel.cancellationResult
        val resultIntent = PaymentController.Result.fromIntent(requireNotNull(intent))
        assertThat(
            resultIntent?.flowOutcome
        ).isEqualTo(StripeIntentResult.Outcome.CANCELED)
        assertThat(resultIntent?.shouldCancelSource).isTrue()
    }

    @Test
    fun `cancellationResult should set correct outcome when user nav is allowed`() {
        val viewModel = PaymentAuthWebViewActivityViewModel(
            PaymentAuthWebViewStarter.Args(
                clientSecret = "client_secret",
                url = "https://example.com",
                shouldCancelSource = true,
                shouldCancelIntentOnUserNavigation = false
            )
        )

        val intent = viewModel.cancellationResult
        val resultIntent = PaymentController.Result.fromIntent(requireNotNull(intent))
        assertThat(
            resultIntent?.flowOutcome
        ).isEqualTo(StripeIntentResult.Outcome.SUCCEEDED)
        assertThat(resultIntent?.shouldCancelSource).isTrue()
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
        assertEquals(
            PaymentAuthWebViewActivityViewModel.ToolbarTitleData(
                "auth webview",
                toolbarCustomization
            ),
            viewModel.toolbarTitle
        )
    }
}
