package com.stripe.android.connect

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Lambda to fetch the Connect SDK client secret from your server.
 * Implement [invoke] to provide your client secret to the Connect SDK.
 */
@PrivateBetaConnectSDK
fun interface FetchClientSecret {

    /**
     * Called when the Connect SDK needs a client secret from your server.
     * Implement a secure network call to your server here to fetch your client secret.
     * If you're unable to provide a client secret for any reason, return null.
     */
    suspend operator fun invoke(): String?
}

/**
 * Task implementing [FetchClientSecret] to be used by consumers who don't wish to use
 * coroutines in their applications, or who are calling the SDK from Java.
 */
@PrivateBetaConnectSDK
abstract class FetchClientSecretTask : FetchClientSecret {
    override suspend fun invoke(): String? {
        return suspendCoroutine { continuation ->
            fetchClientSecret { result ->
                continuation.resume(result)
            }
        }
    }

    /**
     * Called when the Connect SDK needs a client secret from your server.
     * Implement a secure network call to your server here to fetch your client secret
     * and use [callback] to pass it to the Connect SDK.
     *
     * @param callback the callback used to pass the client secret to
     * to the Connect SDK.
     */
    abstract fun fetchClientSecret(callback: ResultCallback)

    /**
     * The interface used to pass your client secret to the SDK.
     */
    @PrivateBetaConnectSDK
    fun interface ResultCallback {
        /**
         * Once you have retrieved your client Secret, call [onResult] with the client secret
         * as the parameter. If there is an error or you're unable to provide a client secret,
         * call the method with null.
         * [onResult] must be called exactly once. Subsequent calls after the first one
         * will be ignored.
         */
        fun onResult(clientSecret: String?)
    }
}
