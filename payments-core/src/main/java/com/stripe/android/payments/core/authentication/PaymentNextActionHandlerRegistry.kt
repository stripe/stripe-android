package com.stripe.android.payments.core.authentication

import com.stripe.android.payments.core.ActivityResultLauncherHost

/**
 * Registry to map [Actionable] to the corresponding [PaymentNextActionHandler].
 */
internal interface PaymentNextActionHandlerRegistry : ActivityResultLauncherHost {

    /**
     * Returns the correct [PaymentNextActionHandler] to handle the [Actionable].
     */
    fun <Actionable> getNextActionHandler(
        actionable: Actionable
    ): PaymentNextActionHandler<Actionable>
}
