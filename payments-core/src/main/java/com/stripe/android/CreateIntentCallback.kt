package com.stripe.android

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface AbsCreateIntentCallback

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface CreateIntentCallback : AbsCreateIntentCallback {

    suspend fun onCreateIntent(paymentMethodId: String): Result

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface Result {

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Success(val clientSecret: String) : Result

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Failure(val errorMessage: String) : Result
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface CreateIntentCallbackForServerSideConfirmation : AbsCreateIntentCallback {

    suspend fun onCreateIntent(
        paymentMethodId: String,
        shouldSavePaymentMethod: Boolean,
    ): CreateIntentCallback.Result
}
