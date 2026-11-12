package com.stripe.android.checkout.analytics

import android.content.Context
import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Operation
import com.stripe.android.checkout.injection.DaggerCheckoutAnalyticsReporterComponent
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlin.time.Duration

internal class LocalOperation @AssistedInject constructor(
    @Assisted operation: Operation,
    @Assisted override val params: Map<String, Any?>,
    analyticsReporter: CheckoutAnalyticsReporter,
) : CheckoutAnalyticsOperation<Nothing>(operation, analyticsReporter::fireEvent) {
    fun invoked(response: CheckoutSessionResponse?) {
        reportOutcome(outcome = null, response = response, error = null)
    }

    override fun elapsed(): Duration? = null

    override fun analyticsError(error: Nothing): CheckoutAnalyticsEvent.Error = error

    @AssistedFactory
    fun interface Factory {
        fun create(
            operation: Operation,
            params: Map<String, Any?>,
        ): LocalOperation
    }

    companion object {
        fun createFallbackFactory(context: Context): Factory {
            return DaggerCheckoutAnalyticsReporterComponent.factory()
                .create(context.applicationContext)
                .localOperationFactory
        }
    }
}
