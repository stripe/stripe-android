package com.stripe.android.payments.paymentlauncher

import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.payments.core.injection.WeakMapInjectorRegistry
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
        { PUBLISHABLE_KEY },
        { STRIPE_ACCOUNT_ID },
        mockHostActivityLauncher,
        context = mock(),
        enableLogging = false,
        ioContext = mock(),
        uiContext = mock(),
        stripeRepository = mock(),
        analyticsRequestFactory = mock(),
        productUsage = mock()
    )

    @Test
    fun `verify confirm payment creates correct params`() {
        val params = mock<ConfirmPaymentIntentParams>()

        paymentLauncher.confirm(params)

        verify(mockHostActivityLauncher).launch(
            argWhere { arg ->
                arg is PaymentLauncherContract.Args.IntentConfirmationArgs &&
                    arg.injectorKey == (PaymentLauncher::class.simpleName + WeakMapInjectorRegistry.CURRENT_REGISTER_KEY.get()) &&
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
                    arg.injectorKey == (PaymentLauncher::class.simpleName + WeakMapInjectorRegistry.CURRENT_REGISTER_KEY.get()) &&
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
                    arg.injectorKey == (PaymentLauncher::class.simpleName + WeakMapInjectorRegistry.CURRENT_REGISTER_KEY.get()) &&
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
                    arg.injectorKey == (PaymentLauncher::class.simpleName + WeakMapInjectorRegistry.CURRENT_REGISTER_KEY.get()) &&
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
