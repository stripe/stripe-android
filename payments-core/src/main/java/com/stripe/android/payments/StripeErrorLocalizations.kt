package com.stripe.android.payments

import android.content.Context
import com.stripe.android.networking.mapErrorCodeToLocalizedMessage

object StripeErrorLocalizations {
    /**
     * Returns a localized user-facing message for a given error code.
     * This method can be used to display an appropriate error message to the user.
     *
     * @return A localized error message, or null if the error code is not recognized.
     */
    @JvmStatic
    fun forCode(context: Context, code: String): String? {
        return context.mapErrorCodeToLocalizedMessage(code)
    }
}
