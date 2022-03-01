package com.stripe.android.payments.paymentlauncher

import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RestrictTo
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory

/**
 * [AssistedFactory] to create a [StripePaymentLauncher] with shared dependencies already created
 * elsewhere.
 *
 * Used when [PaymentLauncher] is declared as an internal dependency by another daggerized public
 * API (e.g PaymentSheet).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@AssistedFactory
interface StripePaymentLauncherAssistedFactory {
    fun create(
        @Assisted(PUBLISHABLE_KEY) publishableKey: () -> String,
        @Assisted(STRIPE_ACCOUNT_ID) stripeAccountId: () -> String?,
        hostActivityLauncher: ActivityResultLauncher<PaymentLauncherContract.Args>
    ): StripePaymentLauncher
}
