package com.stripe.android.connect

import androidx.annotation.RestrictTo

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface FetchClientSecretCallback {

    @PrivateBetaConnectSDK
    fun fetchClientSecret(resultCallback: ClientSecretResultCallback)

    @PrivateBetaConnectSDK
    interface ClientSecretResultCallback {
        fun onResult(secret: String?)
    }
}
