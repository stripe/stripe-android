package com.stripe.android.core.exception

import androidx.annotation.RestrictTo
import com.stripe.android.core.StripeError

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LocalStripeException(
    displayMessage: String?,
    val analyticsValue: String?,
    val errorCode: String? = null,
    val declineCode: String? = null,
    val type: String? = null,
) : StripeException(
    message = displayMessage,
    stripeError = StripeError(
        code = errorCode,
        declineCode = declineCode,
        type = type,
    )
) {
    private val localDisplayMessage = displayMessage

    override val displayMessage: String?
        get() = localDisplayMessage ?: super.displayMessage

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun analyticsValue(): String = analyticsValue ?: "unknown"
}
