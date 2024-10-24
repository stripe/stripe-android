package com.stripe.android.connect

@PrivateBetaConnectSDK
fun interface FetchClientSecretCallback {
    fun fetchClientSecret(resultCallback: ClientSecretResultCallback)

    interface ClientSecretResultCallback {
        fun onResult(secret: String?)
    }
}
