package com.stripe.android.payments.core.authentication

import com.stripe.android.payments.core.ActivityResultLauncherHost

/**
 * Registry to map [Authenticatable] to the corresponding [PaymentNextActionHandler].
 */
internal interface PaymentAuthenticatorRegistry : ActivityResultLauncherHost {

    /**
     * Returns the correct [PaymentNextActionHandler] to handle the [Authenticatable].
     */
    fun <Authenticatable> getAuthenticator(
        authenticatable: Authenticatable
    ): PaymentNextActionHandler<Authenticatable>
}
