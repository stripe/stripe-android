package com.stripe.android.payments.bankaccount

import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.payments.bankaccount.navigation.CollectSessionForDeferredPaymentsContract
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StripeCollectSessionForDeferredPaymentsLauncherTest {

    private val mockHostActivityLauncher =
        mock<ActivityResultLauncher<CollectSessionForDeferredPaymentsContract.Args>>()

    private val launcher = StripeCollectSessionForDeferredPaymentsLauncher(mockHostActivityLauncher)

    @Test
    fun `present - launches CollectSessionActivity with correct arguments`() {
        launcher.presentForDeferredPayment(
            publishableKey = PUBLISHABLE_KEY,
            stripeAccountId = STRIPE_ACCOUNT_ID,
        )

        verify(mockHostActivityLauncher).launch(
            CollectSessionForDeferredPaymentsContract.Args(
                publishableKey = PUBLISHABLE_KEY,
                stripeAccountId = STRIPE_ACCOUNT_ID,
            )
        )
    }

    companion object {
        private const val PUBLISHABLE_KEY = "publishableKey"
        private const val STRIPE_ACCOUNT_ID = "stripe_account_id"
    }
}
