package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import com.stripe.android.core.ApiConfiguration

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun ApiConfiguration.State.toRequestOptions(idempotencyKey: String?): ApiRequest.Options {
    return ApiRequest.Options.create(this, idempotencyKey)
}

/** An explicit null account override means that no account header should be sent. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun ApiConfiguration.State.withRequestCredentials(
    apiKey: String,
    stripeAccount: String?,
    idempotencyKey: String?,
): ApiRequest.Options {
    return copy(publishableKey = apiKey, stripeAccountId = stripeAccount).toRequestOptions(idempotencyKey)
}
