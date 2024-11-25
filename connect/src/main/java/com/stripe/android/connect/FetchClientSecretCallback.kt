package com.stripe.android.connect

import androidx.annotation.RestrictTo

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface FetchClientSecretCallback {

    @PrivateBetaConnectSDK
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun fetchClientSecret(resultCallback: ClientSecretResultCallback)

    @PrivateBetaConnectSDK
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    interface ClientSecretResultCallback {
        fun onResult(secret: String?)
    }
}
