package com.stripe.android

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface AbsCreateIntentCallback

@ExperimentalPaymentSheetDecouplingApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface CreateIntentCallback : AbsCreateIntentCallback {

    suspend fun onCreateIntent(paymentMethodId: String): Result

    @ExperimentalPaymentSheetDecouplingApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface Result {

        @ExperimentalPaymentSheetDecouplingApi
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Success(val clientSecret: String) : Result

        @ExperimentalPaymentSheetDecouplingApi
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Failure(
            internal val cause: Exception,
            internal val displayMessage: String? = null
        ) : Result
    }
}

@ExperimentalPaymentSheetDecouplingApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface CreateIntentCallbackForServerSideConfirmation : AbsCreateIntentCallback {

    suspend fun onCreateIntent(
        paymentMethodId: String,
        shouldSavePaymentMethod: Boolean,
    ): CreateIntentCallback.Result
}
