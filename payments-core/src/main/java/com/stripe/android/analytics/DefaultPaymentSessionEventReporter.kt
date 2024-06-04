package com.stripe.android.analytics

import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
internal class DefaultPaymentSessionEventReporter @Inject internal constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
    private val durationProvider: DurationProvider,
    @IOContext private val workContext: CoroutineContext
) : PaymentSessionEventReporter {
    override fun onLoadStarted() {
        durationProvider.start(DurationProvider.Key.Loading)

        fireEvent(PaymentSessionEvent.LoadStarted())
    }

    override fun onLoadSucceeded(code: PaymentMethodCode?) {
        fireEvent(
            PaymentSessionEvent.LoadSucceeded(
                duration = durationProvider.end(DurationProvider.Key.Loading),
                code = code,
            )
        )
    }

    override fun onLoadFailed(error: Throwable) {
        fireEvent(
            PaymentSessionEvent.LoadFailed(
                duration = durationProvider.end(DurationProvider.Key.Loading),
                error = error
            )
        )
    }

    override fun onOptionsShown() {
        fireEvent(
            PaymentSessionEvent.ShowPaymentOptions()
        )
    }

    override fun onFormShown(code: PaymentMethodCode) {
        durationProvider.start(DurationProvider.Key.ConfirmButtonClicked)

        fireEvent(
            PaymentSessionEvent.ShowPaymentOptionForm(code)
        )
    }

    override fun onFormInteracted(code: PaymentMethodCode) {
        fireEvent(
            PaymentSessionEvent.PaymentOptionFormInteraction(code)
        )
    }

    override fun onCardNumberCompleted() {
        fireEvent(
            PaymentSessionEvent.CardNumberCompleted()
        )
    }

    override fun onDoneButtonTapped(code: PaymentMethodCode) {
        fireEvent(
            PaymentSessionEvent.TapDoneButton(
                code = code,
                duration = durationProvider.end(DurationProvider.Key.ConfirmButtonClicked)
            )
        )
    }

    private fun fireEvent(event: PaymentSessionEvent) {
        CoroutineScope(workContext).launch {
            analyticsRequestExecutor.executeAsync(
                paymentAnalyticsRequestFactory.createRequest(
                    event = event,
                    additionalParams = event.additionalParams
                )
            )
        }
    }
}
