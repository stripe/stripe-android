package com.stripe.android.connect

import androidx.annotation.RestrictTo

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface FetchClientSecretCallback {

    fun fetchClientSecret(resultCallback: ClientSecretResultCallback)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    interface ClientSecretResultCallback {
        fun onResult(secret: String?)
    }
}
