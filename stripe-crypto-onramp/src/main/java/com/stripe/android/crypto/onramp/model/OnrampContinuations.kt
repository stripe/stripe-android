package com.stripe.android.crypto.onramp.model

import kotlinx.coroutines.CompletableDeferred
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Internal class that manages continuations for suspend function results in the Onramp coordinator.
 * This provides a clean way to handle async operations that bridge callback-based APIs to suspend functions.
 */
@Singleton
internal class OnrampContinuations @Inject constructor() {

    private var configurationDeferred: CompletableDeferred<OnrampConfigurationResult>? = null
    private var lookupDeferred: CompletableDeferred<OnrampLinkLookupResult>? = null
    private var authenticationDeferred: CompletableDeferred<OnrampVerificationResult>? = null
    private var registerUserDeferred: CompletableDeferred<OnrampRegisterUserResult>? = null

    /**
     * Starts a configuration operation and returns a deferred that will complete when the result is available.
     */
    suspend fun awaitConfiguration(): OnrampConfigurationResult {
        val deferred = CompletableDeferred<OnrampConfigurationResult>()
        configurationDeferred = deferred
        return deferred.await()
    }

    /**
     * Completes the configuration operation with the given result.
     */
    fun completeConfiguration(result: OnrampConfigurationResult) {
        val deferred = configurationDeferred ?: return
        configurationDeferred = null
        deferred.complete(result)
    }

    /**
     * Starts a lookup operation and returns a deferred that will complete when the result is available.
     */
    suspend fun awaitLookup(): OnrampLinkLookupResult {
        val deferred = CompletableDeferred<OnrampLinkLookupResult>()
        lookupDeferred = deferred
        return deferred.await()
    }

    /**
     * Completes the lookup operation with the given result.
     */
    fun completeLookup(result: OnrampLinkLookupResult) {
        val deferred = lookupDeferred ?: return
        lookupDeferred = null
        deferred.complete(result)
    }

    /**
     * Starts an authentication operation and returns a deferred that will complete when the result is available.
     */
    suspend fun awaitAuthentication(): OnrampVerificationResult {
        val deferred = CompletableDeferred<OnrampVerificationResult>()
        authenticationDeferred = deferred
        return deferred.await()
    }

    /**
     * Completes the authentication operation with the given result.
     */
    fun completeAuthentication(result: OnrampVerificationResult) {
        val deferred = authenticationDeferred ?: return
        authenticationDeferred = null
        deferred.complete(result)
    }

    /**
     * Starts a user registration operation and returns a deferred that will complete when the result is available.
     */
    suspend fun awaitRegistration(): OnrampRegisterUserResult {
        val deferred = CompletableDeferred<OnrampRegisterUserResult>()
        registerUserDeferred = deferred
        return deferred.await()
    }

    /**
     * Completes the user registration operation with the given result.
     */
    fun completeRegistration(result: OnrampRegisterUserResult) {
        val deferred = registerUserDeferred ?: return
        registerUserDeferred = null
        deferred.complete(result)
    }
} 