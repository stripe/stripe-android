package com.stripe.android.payments.paymentlauncher

import android.graphics.Color
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.model.ConfirmPaymentIntentParams
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StripePaymentLauncherTest {
    private val mockHostActivityLauncher =
        mock<ActivityResultLauncher<PaymentLauncherContract.Args>>()
    private val paymentLauncher = StripePaymentLauncher(
        publishableKeyProvider = { PUBLISHABLE_KEY },
        stripeAccountIdProvider = { STRIPE_ACCOUNT_ID },
        hostActivityLauncher = mockHostActivityLauncher,
        enableLogging = false,
        productUsage = mock(),
        includePaymentSheetAuthenticators = false,
        statusBarColor = Color.RED,
    )

    @Test
    fun `verify confirm payment creates correct params`() {
        val params = mock<ConfirmPaymentIntentParams>()

        paymentLauncher.confirm(params)

        verify(mockHostActivityLauncher).launch(
            argWhere { arg ->
                arg is PaymentLauncherContract.Args.IntentConfirmationArgs &&
                    arg.confirmStripeIntentParams == params
            }
        )
    }

    @Test
    fun `verify confirm setup creates correct params`() {
        val params = mock<ConfirmPaymentIntentParams>()

        paymentLauncher.confirm(params)

        verify(mockHostActivityLauncher).launch(
            argWhere { arg ->
                arg is PaymentLauncherContract.Args.IntentConfirmationArgs &&
                    arg.confirmStripeIntentParams == params
            }
        )
    }

    @Test
    fun `verify handle next action for payment creates correct params`() {
        paymentLauncher.handleNextActionForPaymentIntent(CLIENT_SECRET)

        verify(mockHostActivityLauncher).launch(
            argWhere { arg ->
                arg is PaymentLauncherContract.Args.PaymentIntentNextActionArgs &&
                    arg.paymentIntentClientSecret == CLIENT_SECRET
            }
        )
    }

    @Test
    fun `verify handle next action for setup creates correct params`() {
        paymentLauncher.handleNextActionForSetupIntent(CLIENT_SECRET)

        verify(mockHostActivityLauncher).launch(
            argWhere { arg ->
                arg is PaymentLauncherContract.Args.SetupIntentNextActionArgs &&
                    arg.setupIntentClientSecret == CLIENT_SECRET
            }
        )
    }

    companion object {
        const val PUBLISHABLE_KEY = "publishableKey"
        const val STRIPE_ACCOUNT_ID = "stripeAccountId"
        const val CLIENT_SECRET = "clientSecret"
    }
}
