package com.stripe.android

import androidx.annotation.RestrictTo
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface CreateIntentCallback {

    suspend fun onCreateIntent(paymentMethodId: String): Result

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface Result {

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Success(val clientSecret: String) : Result

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Failure(val error: Throwable) : Result
    }

    companion object {

        @JvmStatic
        fun forJava(callback: LegacyCreateIntentCallback): CreateIntentCallback {
            return CreateIntentCallback { paymentMethodId ->
                suspendCoroutine { continuation ->
                    callback.onCreateIntent(paymentMethodId, continuation::resume)
                }
            }
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface CreateIntentCallbackForServerSideConfirmation : CreateIntentCallback {

    override suspend fun onCreateIntent(paymentMethodId: String): CreateIntentCallback.Result {
        error("This should not have been called")
    }

    suspend fun onCreateIntent(
        paymentMethodId: String,
        shouldSavePaymentMethod: Boolean,
    ): CreateIntentCallback.Result

    companion object {

        @JvmStatic
        fun forJava(
            callback: LegacyCreateIntentCallbackForServerSideConfirmation,
        ): CreateIntentCallbackForServerSideConfirmation {
            return CreateIntentCallbackForServerSideConfirmation { paymentMethodId, shouldSavePM ->
                suspendCoroutine { continuation ->
                    callback.onCreateIntent(paymentMethodId, shouldSavePM, continuation::resume)
                }
            }
        }
    }
}

fun interface LegacyCreateIntentCallback {

    fun interface ResultListener {
        fun onResult(result: CreateIntentCallback.Result)
    }

    fun onCreateIntent(
        paymentMethodId: String,
        resultListener: ResultListener,
    )
}

fun interface LegacyCreateIntentCallbackForServerSideConfirmation {

    fun onCreateIntent(
        paymentMethodId: String,
        shouldSavePaymentMethod: Boolean,
        resultListener: LegacyCreateIntentCallback.ResultListener,
    )
}

