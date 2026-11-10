package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import com.stripe.android.core.ApiConfiguration

/** Snapshots session policy and the credentials of one operation. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun ApiConfiguration.State.toRequestOptions(idempotencyKey: String?): ApiRequest.Options {
    return withRequestCredentials(publishableKey, stripeAccountId, idempotencyKey)
}

/** An explicit null account override means that no account header should be sent. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun ApiConfiguration.State.withRequestCredentials(
    apiKey: String,
    stripeAccount: String?,
    idempotencyKey: String?,
): ApiRequest.Options {
    return ApiRequest.Options(
        apiKey = apiKey,
        stripeAccount = stripeAccount,
        idempotencyKey = idempotencyKey,
        betas = betas.toSet(),
    )
}
