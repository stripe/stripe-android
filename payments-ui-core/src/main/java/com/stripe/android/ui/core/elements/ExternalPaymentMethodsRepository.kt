package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.StripeException
import com.stripe.android.payments.core.analytics.ErrorReporter
import javax.inject.Inject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ExternalPaymentMethodsRepository @Inject constructor(private val errorReporter: ErrorReporter) {
    fun getExternalPaymentMethodSpecs(externalPaymentMethodData: String?): List<ExternalPaymentMethodSpec> {
        if (externalPaymentMethodData.isNullOrEmpty()) {
            return emptyList()
        }
        return ExternalPaymentMethodsSerializer.deserializeList(externalPaymentMethodData)
            .onFailure {
                errorReporter.report(
                    ErrorReporter.UnexpectedErrorEvent.EXTERNAL_PAYMENT_METHOD_SERIALIZATION_FAILURE,
                    StripeException.create(it)
                )
            }
            .getOrElse { emptyList() }
    }
}
