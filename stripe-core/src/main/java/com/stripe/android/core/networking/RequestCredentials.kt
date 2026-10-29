package com.stripe.android.core.networking

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.ApiKeyValidator
import kotlinx.parcelize.Parcelize

/** Effective request identity. Session policy and idempotency are supplied separately. */
@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class RequestCredentials(
    val apiKey: String,
    val stripeAccount: String?,
) : Parcelable {
    init {
        ApiKeyValidator().requireValid(apiKey)
    }

    val apiKeyIsUserKey: Boolean get() = apiKey.startsWith("uk_")
    val apiKeyIsLiveMode: Boolean get() = !apiKey.contains("test")

    override fun toString(): String = "RequestCredentials(apiKey=***)"

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        const val UNDEFINED_PUBLISHABLE_KEY = "pk_undefined"
    }
}

/** Null override inherits the session identity; a null account on an override remains null. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun resolveRequestCredentials(
    apiConfiguration: ApiConfiguration.State?,
    credentialsOverride: RequestCredentials?,
): RequestCredentials = credentialsOverride ?: requireNotNull(apiConfiguration).let {
    RequestCredentials(it.publishableKey, it.stripeAccountId)
}
