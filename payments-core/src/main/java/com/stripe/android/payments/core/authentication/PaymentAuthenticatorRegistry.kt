package com.stripe.android.payments.core.authentication

import com.stripe.android.payments.core.ActivityResultLauncherHost

/**
 * Registry to map [Authenticatable] to the corresponding [PaymentAuthenticator].
 */
internal interface PaymentAuthenticatorRegistry : ActivityResultLauncherHost {

    /**
     * Returns the correct [PaymentAuthenticator] to handle the [Authenticatable].
     */
    fun <Authenticatable> getAuthenticator(
        authenticatable: Authenticatable
    ): PaymentAuthenticator<Authenticatable>
}
