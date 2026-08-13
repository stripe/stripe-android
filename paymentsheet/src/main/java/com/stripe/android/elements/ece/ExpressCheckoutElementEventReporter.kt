@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.elements.ece

import com.stripe.android.checkout.CheckoutControllerStateHolder
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.core.utils.mapOfDurationInSeconds
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentsheet.analytics.PaymentSheetConfirmationError
import com.stripe.android.paymentsheet.analytics.linkContext
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.utils.toConfirmationError
import com.stripe.android.utils.filterNotNullValues
import javax.inject.Inject

internal interface ExpressCheckoutElementEventReporter {
    fun onEceDisplayed()

    fun onEceWalletTapped(
        expressButton: ExpressButton,
    )

    fun onEcePaymentSuccess(
        expressButton: ExpressButton,
    )

    fun onEcePaymentFailure(
        expressButton: ExpressButton,
        error: ConfirmationHandler.Result.Failed,
    )
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

    override fun onEceWalletTapped(
        expressButton: ExpressButton,
    ) {
        fireEvent(
            eventName = ECE_WALLET_TAPPED_EVENT_NAME,
            additionalParams = durationProvider.elapsed(DurationProvider.Key.ExpressCheckoutElement)
                .mapOfDurationInSeconds() + paymentMethodParams(expressButton),
        )
    }

    override fun onEcePaymentSuccess(expressButton: ExpressButton) {
        fireEvent(
            eventName = ECE_PAYMENT_SUCCESS_EVENT_NAME,
            additionalParams = durationProvider.elapsed(DurationProvider.Key.ExpressCheckoutElement)
                .mapOfDurationInSeconds() + paymentMethodParams(expressButton),
        )
    }

    override fun onEcePaymentFailure(
        expressButton: ExpressButton,
        error: ConfirmationHandler.Result.Failed,
    ) {
        val confirmationError = error.toConfirmationError()
            ?: PaymentSheetConfirmationError.Stripe(error.cause)
        fireEvent(
            eventName = ECE_PAYMENT_FAILURE_EVENT_NAME,
            additionalParams = durationProvider.elapsed(DurationProvider.Key.ExpressCheckoutElement)
                .mapOfDurationInSeconds() +
                paymentMethodParams(expressButton) +
                mapOf(
                    FIELD_ERROR_MESSAGE to confirmationError.analyticsValue,
                    FIELD_ERROR_CODE to confirmationError.errorCode,
                ).filterNotNullValues(),
        )
    }

    private fun paymentMethodParams(expressButton: ExpressButton): Map<String, Any> {
        return mapOf(
            FIELD_SELECTED_LPM to expressButton.toWalletType().code,
            FIELD_LINK_CONTEXT to (expressButton.toSelection() as? PaymentSelection.Link)?.linkContext()
        ).filterNotNullValues()
    }

    private fun defaultParams(): Map<String, Any> {
        val state = stateHolder.state ?: return emptyMap()
        val expressCheckoutElementConfiguration = state.configuration.expressCheckoutElementConfiguration
        val orderedLpms = stateHolder.session.value?.availableExpressButtonTypes.orEmpty().map {
            when (it) {
                is ExpressButtonType.GooglePay -> "google_pay"
                ExpressButtonType.Link -> "link"
            }
        }
        return state.paymentMethodMetadata.analyticsMetadata.paramsMap + mapOf(
            FIELD_ORDERED_LPMS to orderedLpms.joinToString(","),
            FIELD_ECE_CONFIG to mapOf(
                FIELD_LINK_VISIBILITY to expressCheckoutElementConfiguration.linkVisibility.name.lowercase(),
                FIELD_GOOGLE_PAY_VISIBILITY to (expressCheckoutElementConfiguration.googlePayConfiguration.display == ExpressCheckoutElement.Configuration.GooglePayConfiguration.Display.Automatic),
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
        const val ECE_DISPLAYED_EVENT_NAME = "mc_ece_init"
        const val ECE_WALLET_TAPPED_EVENT_NAME = "mc_ece_wallet_tapped"
        const val ECE_PAYMENT_SUCCESS_EVENT_NAME = "mc_ece_payment_success"
        const val ECE_PAYMENT_FAILURE_EVENT_NAME = "mc_ece_payment_failure"

        const val FIELD_SELECTED_LPM = "selected_lpm"
        const val FIELD_LINK_CONTEXT = "link_context"
        const val FIELD_ERROR_MESSAGE = "error_message"
        const val FIELD_ERROR_CODE = "error_code"
        const val FIELD_ORDERED_LPMS = "ordered_lpms"
        const val FIELD_ECE_CONFIG = "ece_config"
        const val FIELD_LINK_VISIBILITY = "link_visibility"
        const val FIELD_GOOGLE_PAY_VISIBILITY = "google_pay_visibility"
    }
}
