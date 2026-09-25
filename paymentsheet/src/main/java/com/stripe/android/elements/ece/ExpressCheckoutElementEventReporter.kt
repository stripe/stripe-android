@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.elements.ece

import com.stripe.android.checkout.CheckoutControllerStateHolder
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.elements.CheckoutGooglePayConfiguration
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.paymentelement.CheckoutSessionPreview
import javax.inject.Inject

internal interface ExpressCheckoutElementEventReporter {
    fun onEceDisplayed()
}

internal class DefaultExpressCheckoutElementEventReporter @Inject constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    private val durationProvider: DurationProvider,
    private val stateHolder: CheckoutControllerStateHolder,
) : ExpressCheckoutElementEventReporter {
    override fun onEceDisplayed() {
        durationProvider.start(DurationProvider.Key.ExpressCheckoutElement)
        fireEvent(
            eventName = ECE_DISPLAYED_EVENT_NAME,
            additionalParams = emptyMap(),
        )
    }

    private fun defaultParams(): Map<String, Any> {
        val state = stateHolder.state ?: return emptyMap()
        val expressCheckoutElementConfiguration = state.configuration.expressCheckoutElementConfiguration
            ?: return emptyMap()
        val paymentMethodMetadata = state.expressCheckoutElementPaymentMethodMetadata ?: return emptyMap()
        val orderedLpms = stateHolder.session.value?.availableExpressButtonTypes.orEmpty().map {
            when (it) {
                is ExpressButtonType.GooglePay -> "google_pay"
                ExpressButtonType.Link -> "link"
            }
        }
        return paymentMethodMetadata.analyticsMetadata.paramsMap + mapOf(
            FIELD_ORDERED_LPMS to orderedLpms.joinToString(","),
            FIELD_ECE_CONFIG to mapOf(
                FIELD_LINK_VISIBILITY to expressCheckoutElementConfiguration.linkConfiguration.display
                    .toAnalyticsValue(),
                FIELD_GOOGLE_PAY_VISIBILITY to (
                    expressCheckoutElementConfiguration.googlePayConfiguration.display.toAnalyticsValue()
                    ),
            ),
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
                additionalParams = defaultParams() + additionalParams,
            )
        )
    }

    private companion object {
        const val ECE_DISPLAYED_EVENT_NAME = "elements.express_checkout_element.init"

        const val FIELD_ORDERED_LPMS = "ordered_lpms"
        const val FIELD_ECE_CONFIG = "ece_config"
        const val FIELD_LINK_VISIBILITY = "link_visibility"
        const val FIELD_GOOGLE_PAY_VISIBILITY = "google_pay_visibility"

        fun CheckoutGooglePayConfiguration.Display.toAnalyticsValue(): String {
            return when (this) {
                CheckoutGooglePayConfiguration.Display.Automatic -> "automatic"
                CheckoutGooglePayConfiguration.Display.Never -> "never"
            }
        }

        fun ExpressCheckoutElement.Configuration.LinkConfiguration.Display.toAnalyticsValue(): String {
            return when (this) {
                ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Automatic -> "automatic"
                ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Never -> "never"
                ExpressCheckoutElement.Configuration.LinkConfiguration.Display.WalletButtonHidden ->
                    "wallet_button_hidden"
            }
        }
    }
}
