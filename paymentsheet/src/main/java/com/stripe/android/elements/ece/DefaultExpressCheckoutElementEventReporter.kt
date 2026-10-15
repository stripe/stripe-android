package com.stripe.android.elements.ece

import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.elements.CheckoutGooglePayConfiguration
import com.stripe.android.elements.ExpressCheckoutElement
import javax.inject.Inject

internal class DefaultExpressCheckoutElementEventReporter @Inject constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    private val durationProvider: DurationProvider,
) : ExpressCheckoutElementEventReporter {
    override fun onEceDisplayed(content: ExpressCheckoutElementIntegration.Content?) {
        durationProvider.start(DurationProvider.Key.ExpressCheckoutElement)
        fireEvent(
            eventName = ECE_DISPLAYED_EVENT_NAME,
            additionalParams = defaultParams(content),
        )
    }

    private fun defaultParams(content: ExpressCheckoutElementIntegration.Content?): Map<String, Any> {
        content ?: return emptyMap()
        val expressCheckoutElementConfiguration = content.configuration
        val paymentMethodMetadata = content.paymentMethodMetadata
        val orderedLpms = content.availableExpressButtonTypes.map {
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
                additionalParams = additionalParams,
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
            }
        }
    }
}
