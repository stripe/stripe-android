package com.stripe.android.paymentsheet

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ExternalPaymentMethodConfirmHandler {

    fun createIntent(
        context: Context,
        externalPaymentMethodType: String,
    ): Intent
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface ExternalPaymentMethodResult {

    val resultCode: Int

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object Completed : ExternalPaymentMethodResult {
        override val resultCode: Int = Activity.RESULT_OK
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object Canceled : ExternalPaymentMethodResult {
        override val resultCode: Int = Activity.RESULT_CANCELED
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object Failed : ExternalPaymentMethodResult {
        override val resultCode: Int = Activity.RESULT_FIRST_USER
        const val ERROR_MESSAGE_EXTRA: String = "external_payment_method_error_message"
    }
}
