package com.stripe.android.payments

import android.content.Context
import com.stripe.android.networking.mapErrorCodeToLocalizedMessage

object StripeErrorLocalizations {
    /**
     * Returns a localized user-facing message for a given error code.
     * This method can be used to display an appropriate error message to the user.
     *
     * @param context The object containing Android context information
     * @param code The error code string from Stripe API (e.g., "incorrect_number", "card_declined")
     * @param declineCode The decline code string from Stripe API
     *   (e.g., "insufficient_funds", "card_velocity_exceeded")
     *
     * @return A localized error message, or null if the error code is not recognized.
     */
    @JvmStatic
    @JvmOverloads
    fun forCode(context: Context, code: String, declineCode: String? = null): String? {
        return declineCode?.let {
            context.mapErrorCodeToLocalizedMessage(declineCode)
        } ?: context.mapErrorCodeToLocalizedMessage(code)
    }
}
