package com.stripe.android

import androidx.annotation.RestrictTo
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ConfirmCallback {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface Result {

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Success(val clientSecret: String) : Result

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Failure(val error: Throwable) : Result
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface ConfirmCallbackForServerSideConfirmation : ConfirmCallback {

    suspend fun onConfirmResponse(
        paymentMethodId: String,
        shouldSavePaymentMethod: Boolean,
    ): ConfirmCallback.Result
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface ConfirmCallbackForClientSideConfirmation : ConfirmCallback {

    suspend fun onConfirmResponse(
        paymentMethodId: String,
    ): ConfirmCallback.Result
}

internal fun interface LegacyConfirmCallbackForServerSideConfirmation : ConfirmCallbackForServerSideConfirmation {

    override suspend fun onConfirmResponse(
        paymentMethodId: String,
        shouldSavePaymentMethod: Boolean
    ): ConfirmCallback.Result {
        return suspendCoroutine { continuation ->
            onConfirmResponse(
                paymentMethodId = paymentMethodId,
                shouldSavePaymentMethod = shouldSavePaymentMethod,
                resultListener = continuation::resume,
            )
        }
    }

    fun onConfirmResponse(
        paymentMethodId: String,
        shouldSavePaymentMethod: Boolean,
        resultListener: ResultListener,
    )

    fun interface ResultListener {
        fun onResult(result: ConfirmCallback.Result)
    }
}

internal fun interface LegacyConfirmCallbackForClientSideConfirmation : ConfirmCallbackForClientSideConfirmation {

    override suspend fun onConfirmResponse(paymentMethodId: String): ConfirmCallback.Result {
        return suspendCoroutine { continuation ->
            onConfirmResponse(
                paymentMethodId = paymentMethodId,
                resultListener = continuation::resume,
            )
        }
    }

    fun onConfirmResponse(
        paymentMethodId: String,
        resultListener: ResultListener,
    )

    fun interface ResultListener {
        fun onResult(result: ConfirmCallback.Result)
    }
}
