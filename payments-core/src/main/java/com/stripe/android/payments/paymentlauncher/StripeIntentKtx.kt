package com.stripe.android.payments.paymentlauncher

import com.stripe.android.StripeIntentResult
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.AuthenticationException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.core.exception.RateLimitException
import com.stripe.android.core.exception.StripeException
import com.stripe.android.exception.CardException
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent

internal fun StripeIntentResult<StripeIntent>.toFailureThrowable(): StripeException {
    return intent.toStripeException(displayMessage = failureMessage)
        ?: LocalStripeException(
            displayMessage = failureMessage,
            analyticsValue = "failedIntentOutcomeError",
        )
}

private fun StripeIntent.toStripeException(displayMessage: String?): StripeException? {
    return when (this) {
        is PaymentIntent -> lastPaymentError?.toStripeException(displayMessage)
        is SetupIntent -> lastSetupError?.toStripeException(displayMessage)
    }
}

private fun PaymentIntent.Error.toStripeException(displayMessage: String?): StripeException {
    return toStripeError(displayMessage).toStripeException(type?.code)
}

private fun SetupIntent.Error.toStripeException(
    displayMessage: String?
): StripeException {
    return toStripeError(displayMessage).toStripeException(type?.code)
}

private fun PaymentIntent.Error.toStripeError(displayMessage: String?): StripeError {
    return StripeError(
        type = type?.code,
        message = displayMessage ?: message,
        code = code,
        param = param,
        declineCode = declineCode,
        charge = charge,
        docUrl = docUrl,
    )
}

private fun SetupIntent.Error.toStripeError(
    displayMessage: String?
): StripeError {
    return StripeError(
        type = type?.code,
        message = displayMessage ?: message,
        code = code,
        param = param,
        declineCode = declineCode,
        docUrl = docUrl,
    )
}

private fun StripeError.toStripeException(typeCode: String?): StripeException {
    return when (typeCode) {
        PaymentIntent.Error.Type.CardError.code -> CardException(this)
        PaymentIntent.Error.Type.AuthenticationError.code -> AuthenticationException(this)
        PaymentIntent.Error.Type.InvalidRequestError.code -> InvalidRequestException(this)
        PaymentIntent.Error.Type.RateLimitError.code -> RateLimitException(this)
        else -> APIException(this)
    }
}
