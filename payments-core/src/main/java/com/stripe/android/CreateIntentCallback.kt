package com.stripe.android

import androidx.annotation.RestrictTo

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
}
