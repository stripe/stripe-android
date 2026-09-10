package com.stripe.android.payments.core.analytics

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import javax.inject.Inject

/**
 * [ErrorReporter] which sends error analytics via [AnalyticsRequestExecutor].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class RealErrorReporter @Inject constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory
) : ErrorReporter {
    override fun report(
        errorEvent: ErrorReporter.ErrorEvent,
        stripeException: StripeException?,
        additionalNonPiiParams: Map<String, String>,
    ) {
        val unexpectedErrorParams = if (errorEvent is ErrorReporter.UnexpectedErrorEvent) {
            mapOf(
                FIELD_ERROR_CODE to errorEvent.partialEventName,
                FIELD_ERROR_MESSAGE to errorEvent.partialEventName
                    .replace('.', ' ')
                    .replace('_', ' '),
            )
        } else {
            emptyMap()
        }
        val paramsFromStripeException = if (stripeException == null) {
            emptyMap()
        } else {
            ErrorReporter.getAdditionalParamsFromStripeException(stripeException = stripeException)
        }
        val additionalParams = unexpectedErrorParams + paramsFromStripeException + additionalNonPiiParams
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createRequest(errorEvent, additionalParams)
        )
    }

    private companion object {
        const val FIELD_ERROR_CODE = "error_code"
        const val FIELD_ERROR_MESSAGE = "error_message"
    }
}
