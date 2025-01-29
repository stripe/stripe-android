package com.stripe.android.payments.bankaccount

import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CollectBankAccountForACHLauncherTest {

    private val mockHostActivityLauncher =
        mock<ActivityResultLauncher<CollectBankAccountContract.Args>>()

    @Test
    fun `presentWithPaymentIntent - launches CollectBankAccountActivity with correct arguments`() {
        val launcher = makeLauncher()

        launcher.presentWithPaymentIntent(
            publishableKey = PUBLISHABLE_KEY,
            clientSecret = CLIENT_SECRET,
            stripeAccountId = STRIPE_ACCOUNT_ID,
            configuration = CONFIGURATION
        )

        verify(mockHostActivityLauncher).launch(
            CollectBankAccountContract.Args.ForPaymentIntent(
                publishableKey = PUBLISHABLE_KEY,
                stripeAccountId = STRIPE_ACCOUNT_ID,
                clientSecret = CLIENT_SECRET,
                configuration = CONFIGURATION,
                attachToIntent = true,
                hostedSurface = null
            )
        )
    }

    @Test
    fun `presentWithPaymentIntent - launches with correct attachToIntent if hostedSurface not null`() {
        val launcher = makeLauncher(hostedSurface = "payment_element")

        launcher.presentWithPaymentIntent(
            publishableKey = PUBLISHABLE_KEY,
            clientSecret = CLIENT_SECRET,
            stripeAccountId = STRIPE_ACCOUNT_ID,
            configuration = CONFIGURATION
        )

        verify(mockHostActivityLauncher).launch(
            CollectBankAccountContract.Args.ForPaymentIntent(
                publishableKey = PUBLISHABLE_KEY,
                stripeAccountId = STRIPE_ACCOUNT_ID,
                clientSecret = CLIENT_SECRET,
                configuration = CONFIGURATION,
                attachToIntent = false,
                hostedSurface = "payment_element",
            )
        )
    }

    @Test
    fun `presentWithSetupIntent - launches CollectBankAccountActivity with correct arguments`() {
        val launcher = makeLauncher()

        launcher.presentWithSetupIntent(
            publishableKey = PUBLISHABLE_KEY,
            stripeAccountId = STRIPE_ACCOUNT_ID,
            clientSecret = CLIENT_SECRET,
            configuration = CONFIGURATION,
        )

        verify(mockHostActivityLauncher).launch(
            CollectBankAccountContract.Args.ForSetupIntent(
                publishableKey = PUBLISHABLE_KEY,
                stripeAccountId = STRIPE_ACCOUNT_ID,
                clientSecret = CLIENT_SECRET,
                configuration = CONFIGURATION,
                attachToIntent = true,
                hostedSurface = null
            )
        )
    }

    @Test
    fun `presentWithSetupIntent - launches with correct attachToIntent if hostedSurface not null`() {
        val launcher = makeLauncher(hostedSurface = "payment_element")

        launcher.presentWithSetupIntent(
            publishableKey = PUBLISHABLE_KEY,
            stripeAccountId = STRIPE_ACCOUNT_ID,
            clientSecret = CLIENT_SECRET,
            configuration = CONFIGURATION,
        )

        verify(mockHostActivityLauncher).launch(
            CollectBankAccountContract.Args.ForSetupIntent(
                publishableKey = PUBLISHABLE_KEY,
                stripeAccountId = STRIPE_ACCOUNT_ID,
                clientSecret = CLIENT_SECRET,
                configuration = CONFIGURATION,
                attachToIntent = false,
                hostedSurface = "payment_element",
            )
        )
    }

    @Test
    fun `presentWithDeferredPayment - launches CollectBankAccountActivity with correct arguments`() {
        val launcher = makeLauncher()

        launcher.presentWithDeferredPayment(
            publishableKey = PUBLISHABLE_KEY,
            stripeAccountId = STRIPE_ACCOUNT_ID,
            configuration = CONFIGURATION,
            elementsSessionId = "elements_session_id",
            customerId = "customer_id",
            onBehalfOf = "on_behalf_of_id",
            amount = 1000,
            currency = "usd"
        )

        verify(mockHostActivityLauncher).launch(
            CollectBankAccountContract.Args.ForDeferredPaymentIntent(
                publishableKey = PUBLISHABLE_KEY,
                stripeAccountId = STRIPE_ACCOUNT_ID,
                configuration = CONFIGURATION,
                elementsSessionId = "elements_session_id",
                customerId = "customer_id",
                onBehalfOf = "on_behalf_of_id",
                amount = 1000,
                currency = "usd",
                hostedSurface = null
            )
        )
    }

    @Test
    fun `presentWithDeferredSetup - launches CollectBankAccountActivity with correct arguments`() {
        val launcher = makeLauncher()

        launcher.presentWithDeferredSetup(
            publishableKey = PUBLISHABLE_KEY,
            stripeAccountId = STRIPE_ACCOUNT_ID,
            configuration = CONFIGURATION,
            elementsSessionId = "elements_session_id",
            customerId = "customer_id",
            onBehalfOf = "on_behalf_of_id",
        )

        verify(mockHostActivityLauncher).launch(
            CollectBankAccountContract.Args.ForDeferredSetupIntent(
                publishableKey = PUBLISHABLE_KEY,
                stripeAccountId = STRIPE_ACCOUNT_ID,
                configuration = CONFIGURATION,
                elementsSessionId = "elements_session_id",
                customerId = "customer_id",
                onBehalfOf = "on_behalf_of_id",
                hostedSurface = null
            )
        )
    }

    private fun makeLauncher(
        hostedSurface: String? = null,
    ): CollectBankAccountForACHLauncher {
        return CollectBankAccountForACHLauncher(
            mockHostActivityLauncher,
            hostedSurface = hostedSurface,
        )
    }

    companion object {
        private const val CLIENT_SECRET = "client_secret"
        private const val PUBLISHABLE_KEY = "publishableKey"
        private const val STRIPE_ACCOUNT_ID = "stripe_account_id"
        private val CONFIGURATION = CollectBankAccountConfiguration.USBankAccount(
            name = "Carlos",
            email = null
        )
    }
}
