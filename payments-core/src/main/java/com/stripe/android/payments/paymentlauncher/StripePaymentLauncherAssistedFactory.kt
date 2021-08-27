package com.stripe.android.payments.paymentlauncher

import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.payments.core.injection.PUBLISHABLE_KEY
import com.stripe.android.payments.core.injection.STRIPE_ACCOUNT_ID
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import javax.inject.Provider

/**
 * [AssistedFactory] to create a [StripePaymentLauncher] with shared dependencies already created
 * elsewhere.
 *
 * Used when [PaymentLauncher] is declared as an internal dependency by another daggerized public
 * API (e.g PaymentSheet).
 */
@AssistedFactory
internal interface StripePaymentLauncherAssistedFactory {
    fun create(
        @Assisted(PUBLISHABLE_KEY) publishableKey: Provider<String>,
        @Assisted(STRIPE_ACCOUNT_ID) stripeAccountId: Provider<String?>,
        hostActivityLauncher: ActivityResultLauncher<PaymentLauncherContract.Args>
    ): StripePaymentLauncher
}
