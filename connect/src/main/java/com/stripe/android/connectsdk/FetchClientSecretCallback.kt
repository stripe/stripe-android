package com.stripe.android.connectsdk

@PrivateBetaConnectSDK
fun interface FetchClientSecretCallback {
    fun fetchClientSecret(resultCallback: ClientSecretResultCallback)

    interface ClientSecretResultCallback {
        fun onResult(secret: String?)
    }
}
