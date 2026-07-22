package com.stripe.android.checkout.ece

import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import javax.inject.Inject

internal interface ExpressCheckoutElementEventReporter {
    fun onEceDisplayed()

    fun onEceWalletTapped()
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

    override fun onEceWalletTapped() {
        fireEvent(
            eventName = ECE_WALLET_TAPPED_EVENT_NAME,
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
    }
}
