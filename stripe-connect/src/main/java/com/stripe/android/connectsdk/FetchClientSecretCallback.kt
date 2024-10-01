package com.stripe.android.connectsdk

@PrivateBetaConnectSDK
interface FetchClientSecretCallback {
    fun fetchClientSecret(resultCallback: ClientSecretResultCallback)

    interface ClientSecretResultCallback {
        fun onResult(secret: String)
        fun onError()
    }
}
