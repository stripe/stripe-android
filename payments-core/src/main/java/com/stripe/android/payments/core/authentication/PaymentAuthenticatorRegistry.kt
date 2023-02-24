package com.stripe.android.payments.core.authentication

import androidx.annotation.RestrictTo
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.core.ActivityResultLauncherHost

/**
 * Registry to map [Authenticatable] to the corresponding [PaymentAuthenticator].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface PaymentAuthenticatorRegistry : ActivityResultLauncherHost {

    /**
     * Returns the correct [PaymentAuthenticator] to handle the [Authenticatable].
     */
    fun <Authenticatable> getAuthenticator(
        authenticatable: Authenticatable
    ): PaymentAuthenticator<Authenticatable>

    fun registerAuthenticator(
        key: Class<out StripeIntent.NextActionData>,
        authenticator: PaymentAuthenticator<StripeIntent>,
    )

    fun unregisterAuthenticator(key: Class<out StripeIntent.NextActionData>)
}
