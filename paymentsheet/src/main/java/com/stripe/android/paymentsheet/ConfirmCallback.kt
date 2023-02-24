package com.stripe.android.paymentsheet

import androidx.annotation.RestrictTo
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ConfirmCallback {
    /**
     * 🚧 Under construction 🚧
     * The model for the confirmation response from the integrators servers.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface Result {

        /**
         * 🚧 Under construction 🚧
         * @param clientSecret the client secret returned by the integrators server
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Success(val clientSecret: String) : Result

        /**
         * 🚧 Under construction 🚧
         * @param error the error returned while retrieving the confirmation response
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Failure(val error: String) : Result
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface ConfirmCallbackForServerSideConfirmation : ConfirmCallback {

    /**
     * 🚧 Under construction 🚧
     * The callback to implement to retrieve the intent client secret in the deferred flow.
     *
     * @param paymentMethodId the payment method ID to create the intent with
     * @param shouldSavePaymentMethod whether or not the payment method should be saved
     *
     * @return a [Result] which contains the intent client secret and whether or not the
     * intent was confirmed on the integrators server.
     */
    suspend fun onConfirmResponse(paymentMethodId: String, shouldSavePaymentMethod: Boolean): ConfirmCallback.Result
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface ConfirmCallbackForClientSideConfirmation : ConfirmCallback {

    /**
     * 🚧 Under construction 🚧
     * The callback to implement to retrieve the intent client secret in the deferred flow.
     *
     * @param paymentMethodId the payment method ID to create the intent with
     *
     * @return a [Result] which contains the intent client secret and whether or not the
     * intent was confirmed on the integrators server.
     */
    suspend fun onConfirmResponse(paymentMethodId: String): ConfirmCallback.Result
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
                onResult = {
                    continuation.resume(it)
                }
            )
        }
    }

    fun onConfirmResponse(
        paymentMethodId: String,
        shouldSavePaymentMethod: Boolean,
        onResult: (ConfirmCallback.Result) -> Unit
    )
}

internal fun interface LegacyConfirmCallbackForClientSideConfirmation : ConfirmCallbackForClientSideConfirmation {

    override suspend fun onConfirmResponse(paymentMethodId: String): ConfirmCallback.Result {
        return suspendCoroutine { continuation ->
            onConfirmResponse(
                paymentMethodId = paymentMethodId,
                onResult = {
                    continuation.resume(it)
                }
            )
        }
    }

    fun onConfirmResponse(
        paymentMethodId: String,
        onResult: (ConfirmCallback.Result) -> Unit
    )
}
