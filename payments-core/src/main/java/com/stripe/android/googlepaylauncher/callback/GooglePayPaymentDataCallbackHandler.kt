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
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateCallbackRegistry.Selection
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
        val selection = validateInputs(
            request = request,
            selection = GooglePayPaymentDataUpdateCallbackRegistry.get(),
            onCompleteListener = onCompleteListener,
            googlePayJsonFactory = googlePayJsonFactory,
            errorReporter = errorReporter,
            stringResolver = stringResolver,
        ) ?: return
        val paymentDataRequest = requireNotNull(request)

        selection.workScope.launch {
            val update = runCatching {
                GooglePayPaymentDataUpdate.fromIntermediatePaymentData(paymentDataRequest)
            }.getOrElse { error ->
                handleUnexpectedError(
                    event = ErrorReporter.UnexpectedErrorEvent.GOOGLE_PAY_DYNAMIC_CALLBACK_PARSING_FAILURE,
                    throwable = error,
                    googlePayJsonFactory = googlePayJsonFactory,
                    errorReporter = errorReporter,
                    stringResolver = stringResolver,
                    onCompleteListener = onCompleteListener,
                    additionalNonPiiParams = failureStageParams("parse_intermediate_payment_data"),
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
                    additionalNonPiiParams = failureStageParams("serialize_callback_response"),
                )

                return@launch
            }

            onCompleteListener.complete(PaymentDataRequestUpdate.fromJson(json.toString()))
        }
    }

    private fun validateInputs(
        request: IntermediatePaymentData?,
        selection: Selection?,
        onCompleteListener: OnCompleteListener<PaymentDataRequestUpdate>,
        googlePayJsonFactory: GooglePayJsonFactory,
        errorReporter: ErrorReporter,
        stringResolver: (ResolvableString) -> String,
    ): Selection? {
        val event = when {
            request == null -> ErrorReporter.UnexpectedErrorEvent.GOOGLE_PAY_DYNAMIC_CALLBACK_MISSING_REQUEST
            selection == null -> ErrorReporter.UnexpectedErrorEvent.GOOGLE_PAY_DYNAMIC_CALLBACK_MISSING_CALLBACK
            else -> return selection
        }
        handleUnexpectedError(
            event = event,
            googlePayJsonFactory = googlePayJsonFactory,
            errorReporter = errorReporter,
            stringResolver = stringResolver,
            onCompleteListener = onCompleteListener,
            additionalNonPiiParams = inputPresenceParams(request != null, selection != null),
        )
        return null
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
        throwable: Throwable? = null,
        callbackTrigger: GooglePayPaymentDataUpdate.CallbackTrigger? = null,
        onCompleteListener: OnCompleteListener<PaymentDataRequestUpdate>,
        googlePayJsonFactory: GooglePayJsonFactory,
        errorReporter: ErrorReporter,
        stringResolver: (ResolvableString) -> String,
        additionalNonPiiParams: Map<String, String>,
    ) {
        val callbackTriggerParams = callbackTrigger?.let {
            mapOf("callback_trigger" to it.name)
        }.orEmpty()
        errorReporter.report(
            errorEvent = event,
            stripeException = throwable?.let { StripeException.create(it) },
            additionalNonPiiParams = callbackTriggerParams + additionalNonPiiParams,
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

    private fun inputPresenceParams(
        hasRequest: Boolean,
        hasRegisteredCallback: Boolean,
    ): Map<String, String> {
        return mapOf(
            "has_request" to hasRequest.toString(),
            "has_registered_callback" to hasRegisteredCallback.toString(),
        )
    }

    private fun failureStageParams(stage: String): Map<String, String> {
        return mapOf("failure_stage" to stage)
    }
}
