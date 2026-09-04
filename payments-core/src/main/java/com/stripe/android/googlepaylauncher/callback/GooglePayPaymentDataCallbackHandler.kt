package com.stripe.android.googlepaylauncher.callback

import com.google.android.gms.wallet.callback.IntermediatePaymentData
import com.google.android.gms.wallet.callback.OnCompleteListener
import com.google.android.gms.wallet.callback.PaymentDataRequestUpdate
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.R
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataError
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdate
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateCallbackRegistry
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateResponse
import com.stripe.android.payments.core.analytics.ErrorReporter
import kotlinx.coroutines.launch

internal object GooglePayPaymentDataCallbackHandler {
    fun onPaymentDataChanged(
        request: IntermediatePaymentData?,
        onCompleteListener: OnCompleteListener<PaymentDataRequestUpdate>,
        googlePayJsonFactory: GooglePayJsonFactory,
        errorReporter: ErrorReporter,
        stringResolver: (ResolvableString) -> String
    ) {
        val selection = GooglePayPaymentDataUpdateCallbackRegistry.get()

        val hasNoRequest = request == null

        if (hasNoRequest || selection == null) {
            val event = if (hasNoRequest) {
                ErrorReporter.UnexpectedErrorEvent.GOOGLE_PAY_DYNAMIC_CALLBACK_MISSING_REQUEST
            } else {
                ErrorReporter.UnexpectedErrorEvent.GOOGLE_PAY_DYNAMIC_CALLBACK_MISSING_CALLBACK
            }

            handleUnexpectedError(
                event = event,
                throwable = null,
                callbackTrigger = null,
                googlePayJsonFactory = googlePayJsonFactory,
                errorReporter = errorReporter,
                stringResolver = stringResolver,
                onCompleteListener = onCompleteListener,
            )

            return
        }

        selection.workScope.launch {
            val update = runCatching {
                GooglePayPaymentDataUpdate.fromIntermediatePaymentData(request)
            }.getOrElse { error ->
                handleUnexpectedError(
                    event = ErrorReporter.UnexpectedErrorEvent.GOOGLE_PAY_DYNAMIC_CALLBACK_PARSING_FAILURE,
                    throwable = error,
                    callbackTrigger = null,
                    googlePayJsonFactory = googlePayJsonFactory,
                    errorReporter = errorReporter,
                    stringResolver = stringResolver,
                    onCompleteListener = onCompleteListener,
                )

                return@launch
            }

            val response = runCatching {
                selection.callback.onPaymentDataChanged(update)
            }.getOrElse { error ->
                merchantFailureResponse(error, update.callbackTrigger, stringResolver)
            }

            val json = runCatching {
                response.toJson(googlePayJsonFactory)
            }.getOrElse { error ->
                handleUnexpectedError(
                    event = ErrorReporter.UnexpectedErrorEvent.GOOGLE_PAY_DYNAMIC_CALLBACK_PARSING_FAILURE,
                    throwable = error,
                    callbackTrigger = update.callbackTrigger,
                    googlePayJsonFactory = googlePayJsonFactory,
                    errorReporter = errorReporter,
                    stringResolver = stringResolver,
                    onCompleteListener = onCompleteListener,
                )

                return@launch
            }

            onCompleteListener.complete(PaymentDataRequestUpdate.fromJson(json.toString()))
        }
    }

    private fun merchantFailureResponse(
        error: Throwable,
        callbackTrigger: GooglePayPaymentDataUpdate.CallbackTrigger?,
        stringResolver: (ResolvableString) -> String,
    ): GooglePayPaymentDataUpdateResponse {
        return GooglePayPaymentDataUpdateResponse(
            newTransactionInfo = null,
            error = GooglePayPaymentDataError(
                reason = GooglePayPaymentDataError.Reason.OtherError,
                message = error.message ?: stringResolver(R.string.stripe_internal_error.resolvableString),
                intent = callbackTrigger.toErrorIntent(),
            ),
        )
    }

    private fun handleUnexpectedError(
        event: ErrorReporter.UnexpectedErrorEvent,
        throwable: Throwable?,
        callbackTrigger: GooglePayPaymentDataUpdate.CallbackTrigger?,
        onCompleteListener: OnCompleteListener<PaymentDataRequestUpdate>,
        googlePayJsonFactory: GooglePayJsonFactory,
        errorReporter: ErrorReporter,
        stringResolver: (ResolvableString) -> String,
    ) {
        errorReporter.report(
            errorEvent = event,
            stripeException = throwable?.let { StripeException.create(it) }
        )

        onCompleteListener.complete(
            PaymentDataRequestUpdate.fromJson(
                GooglePayPaymentDataUpdateResponse(
                    newTransactionInfo = null,
                    error = GooglePayPaymentDataError(
                        reason = GooglePayPaymentDataError.Reason.OtherError,
                        message = stringResolver(R.string.stripe_internal_error.resolvableString),
                        intent = callbackTrigger.toErrorIntent(),
                    ),
                ).toJson(
                    googlePayJsonFactory = googlePayJsonFactory,
                ).toString()
            )
        )
    }

    private fun GooglePayPaymentDataUpdate.CallbackTrigger?.toErrorIntent(): GooglePayPaymentDataError.Intent {
        return when (this) {
            null,
            GooglePayPaymentDataUpdate.CallbackTrigger.Initialize,
            GooglePayPaymentDataUpdate.CallbackTrigger.ShippingAddress ->
                GooglePayPaymentDataError.Intent.ShippingAddress
            GooglePayPaymentDataUpdate.CallbackTrigger.ShippingOption ->
                GooglePayPaymentDataError.Intent.ShippingOption
            GooglePayPaymentDataUpdate.CallbackTrigger.Offer ->
                GooglePayPaymentDataError.Intent.Offer
        }
    }
}
