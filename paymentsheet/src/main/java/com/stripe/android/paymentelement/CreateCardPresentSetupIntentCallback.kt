package com.stripe.android.paymentelement

import androidx.annotation.RestrictTo
import com.stripe.android.paymentsheet.CreateIntentResult

/**
 * Callback for creating setup intents with a `card_present` payment method type.
 * This should be implemented to allow customers to tap their card on their
 * device to save it.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@TapToAddPreview
fun interface CreateCardPresentSetupIntentCallback {
    /**
     * Called when the customer attempts to save their card by tapping it on
     * their device. This is called before the tap screen is presented to the customer.
     */
    suspend fun createCardPresentSetupIntent(): CreateIntentResult
}
