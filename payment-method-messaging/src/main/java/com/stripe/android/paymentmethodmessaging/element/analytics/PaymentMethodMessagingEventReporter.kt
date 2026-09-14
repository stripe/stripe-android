@file:OptIn(PaymentMethodMessagingElementPreview::class)

package com.stripe.android.paymentmethodmessaging.element.analytics

import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.paymentmethodmessaging.element.PaymentMethodMessagingContent
import com.stripe.android.paymentmethodmessaging.element.PaymentMethodMessagingElement
import com.stripe.android.paymentmethodmessaging.element.PaymentMethodMessagingElementPreview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

internal interface PaymentMethodMessagingEventReporter {
    fun onInit()
    fun onLoadStarted(configuration: PaymentMethodMessagingElement.Configuration.State)
    fun onLoadSucceeded(paymentMethods: List<String>, content: PaymentMethodMessagingContent)
    fun onLoadFailed(error: Throwable)
    fun onElementDisplayed(appearance: PaymentMethodMessagingElement.Appearance.State)
    fun onElementTapped()
}

internal class DefaultPaymentMethodMessagingEventReporter @Inject constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    private val durationProvider: DurationProvider,
    @IOContext private val workContext: CoroutineContext,
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
) : PaymentMethodMessagingEventReporter {

    private var lastAppearance: PaymentMethodMessagingElement.Appearance.State? = null

    private fun fireEvent(event: PaymentMethodMessagingEvent,) {
        val publishableKey = runCatching { publishableKeyProvider() }.getOrNull()
        CoroutineScope(workContext).launch {
            analyticsRequestExecutor.executeAsync(
                analyticsRequestFactory.createRequest(
                    event,
                    event.additionalParams,
                    publishableKey = publishableKey,
                )
            )
        }
    }

    override fun onInit() {
        fireEvent(PaymentMethodMessagingEvent.Init())
    }

    override fun onLoadStarted(configuration: PaymentMethodMessagingElement.Configuration.State) {
        durationProvider.start(DurationProvider.Key.Loading)
        fireEvent(
            PaymentMethodMessagingEvent.LoadStarted(
                configuration = configuration
            )
        )
    }

    override fun onLoadSucceeded(
        paymentMethods: List<String>,
        content: PaymentMethodMessagingContent,
    ) {
        val duration = durationProvider.end(DurationProvider.Key.Loading)
        val contentType = when (content) {
            is PaymentMethodMessagingContent.SinglePartner -> ContentType.SINGLE_PARTNER
            is PaymentMethodMessagingContent.MultiPartner -> ContentType.MULTI_PARTNER
            PaymentMethodMessagingContent.NoContent -> ContentType.NO_CONTENT
        }
        fireEvent(
            PaymentMethodMessagingEvent.LoadSucceeded(
                paymentMethods = paymentMethods,
                contentType = contentType,
                duration = duration
            )
        )
    }

    override fun onLoadFailed(error: Throwable) {
        val duration = durationProvider.end(DurationProvider.Key.Loading)
        fireEvent(
            PaymentMethodMessagingEvent.LoadFailed(
                error = error,
                duration = duration
            )
        )
    }

    override fun onElementDisplayed(appearance: PaymentMethodMessagingElement.Appearance.State) {
        if (lastAppearance != appearance) {
            fireEvent(
                PaymentMethodMessagingEvent.ElementDisplayed(
                    appearance = appearance
                )
            )
        }
        lastAppearance = appearance
    }

    override fun onElementTapped() {
        fireEvent(
            PaymentMethodMessagingEvent.ElementTapped()
        )
    }
}
