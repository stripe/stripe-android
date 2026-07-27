package com.stripe.android.checkout.ece

import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.paymentsheet.analytics.linkContext
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.utils.filterNotNullValues
import javax.inject.Inject

internal interface ExpressCheckoutElementEventReporter {
    fun onEceDisplayed()

    fun onEceWalletTapped(
        expressButton: ExpressButton,
    )

    fun onEcePaymentSuccess()

    fun onEcePaymentFailure()
}

internal class DefaultExpressCheckoutElementEventReporter @Inject constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
) : ExpressCheckoutElementEventReporter {
    override fun onEceDisplayed() {
        fireEvent(
            eventName = ECE_DISPLAYED_EVENT_NAME,
            additionalParams = emptyMap(),
        )
    }

    override fun onEceWalletTapped(
        expressButton: ExpressButton,
    ) {
        val params = mapOf(
            FIELD_SELECTED_LPM to expressButton.toWalletType().code,
            FIELD_LINK_CONTEXT to (expressButton.toSelection() as? PaymentSelection.Link)?.linkContext()
        ).filterNotNullValues()
        fireEvent(
            eventName = ECE_WALLET_TAPPED_EVENT_NAME,
            additionalParams = params,
        )
    }

    override fun onEcePaymentSuccess() {
        fireEvent(
            eventName = ECE_PAYMENT_SUCCESS_EVENT_NAME,
            additionalParams = emptyMap(),
        )
    }

    override fun onEcePaymentFailure() {
        fireEvent(
            eventName = ECE_PAYMENT_FAILURE_EVENT_NAME,
            additionalParams = emptyMap(),
        )
    }

    private fun fireEvent(
        eventName: String,
        additionalParams: Map<String, Any>,
    ) {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createRequest(
                event = object : AnalyticsEvent {
                    override val eventName: String = eventName
                },
                additionalParams = additionalParams,
            )
        )
    }

    private companion object {
        const val ECE_DISPLAYED_EVENT_NAME = "mc_ece_init"
        const val ECE_WALLET_TAPPED_EVENT_NAME = "mc_ece_wallet_tapped"
        const val ECE_PAYMENT_SUCCESS_EVENT_NAME = "mc_ece_payment_success"
        const val ECE_PAYMENT_FAILURE_EVENT_NAME = "mc_ece_payment_failure"

        const val FIELD_SELECTED_LPM = "selected_lpm"
        const val FIELD_LINK_CONTEXT = "link_context"
    }
}
