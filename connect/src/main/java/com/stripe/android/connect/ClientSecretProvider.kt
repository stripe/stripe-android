package com.stripe.android.connect

import androidx.annotation.RestrictTo
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Listener to provide the Connect SDK client secret from your server.
 * Implement the [provideClientSecret] method to provide your client secret to the Connect SDK.
 */
@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface ClientSecretProvider {

    /**
     * Called when the Connect SDK needs a client secret from your server.
     * Implement a secure network call to your server here to fetch your client secret.
     * If you're unable to provide a client secret for any reason, return null.
     */
    suspend fun provideClientSecret(): String?
}

/**
 * Listener to provide the Connect SDK client secret from your server.
 * Implement the [provideClientSecret] method and use the listener parameter
 * to provide your client secret to the Connect SDK.
 *
 * This listener wraps [ClientSecretProvider] and is intended to be used by consumers
 * who don't wish to use coroutines in their applications, or who are calling the SDK
 * from Java.
 */
@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class ClientSecretProviderListenerWrapper : ClientSecretProvider {
    override suspend fun provideClientSecret(): String? {
        return suspendCoroutine { continuation ->
            provideClientSecret { result ->
                continuation.resume(result)
            }
        }
    }

    /**
     * Called when the Connect SDK needs a client secret from your server.
     * Implement a secure network call to your server here to fetch your client secret
     * and use [resultListener] to pass it to the Connect SDK.
     *
     * @param resultListener the listener used to pass the client secret to
     * to the Connect SDK.
     */
    abstract fun provideClientSecret(resultListener: ClientSecretResultListener)

    /**
     * The interface used to pass your client secret to the SDK.
     */
    @PrivateBetaConnectSDK
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun interface ClientSecretResultListener {
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
