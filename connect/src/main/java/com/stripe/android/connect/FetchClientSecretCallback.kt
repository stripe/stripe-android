package com.stripe.android.connect

@PrivateBetaConnectSDK
fun interface FetchClientSecretCallback {

    @PrivateBetaConnectSDK
    fun fetchClientSecret(resultCallback: ClientSecretResultCallback)

    @PrivateBetaConnectSDK
    interface ClientSecretResultCallback {
        fun onResult(secret: String?)
    }
}
