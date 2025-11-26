@file:OptIn(PaymentMethodMessagingElementPreview::class)

package com.stripe.android.paymentmethodmessaging.element.analytics

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.paymentmethodmessaging.element.PaymentMethodMessagingContent
import com.stripe.android.paymentmethodmessaging.element.PaymentMethodMessagingElement
import com.stripe.android.paymentmethodmessaging.element.PaymentMethodMessagingElementPreview

internal class FakeEventReporter : PaymentMethodMessagingEventReporter {
    private val _initTurbine = Turbine<Unit>()
    val initTurbine: ReceiveTurbine<Unit> = _initTurbine

    private val _loadStartedTurbine = Turbine<PaymentMethodMessagingElement.Configuration.State>()
    val loadStartedTurbine: ReceiveTurbine<PaymentMethodMessagingElement.Configuration.State> = _loadStartedTurbine

    private val _loadSucceededTurbine = Turbine<LoadSucceededCall>()
    val loadSucceededTurbine: ReceiveTurbine<LoadSucceededCall> = _loadSucceededTurbine

    private val _loadFailedTurbine = Turbine<Throwable>()
    val loadFailedTurbine: ReceiveTurbine<Throwable> = _loadFailedTurbine

    private val _elementDisplayedTurbine = Turbine<Unit>()
    val elementDisplayedTurbine: ReceiveTurbine<Unit> = _elementDisplayedTurbine

    private val _elementTappedTurbine = Turbine<Unit>()
    val elementTappedTurbine: ReceiveTurbine<Unit> = _elementTappedTurbine

    fun validate() {
        _initTurbine.ensureAllEventsConsumed()
        _loadStartedTurbine.ensureAllEventsConsumed()
        _loadSucceededTurbine.ensureAllEventsConsumed()
        _loadFailedTurbine.ensureAllEventsConsumed()
        _elementDisplayedTurbine.ensureAllEventsConsumed()
        _elementTappedTurbine.ensureAllEventsConsumed()
    }

    override fun onInit() {
        _initTurbine.add(Unit)
    }

    override fun onLoadStarted(configuration: PaymentMethodMessagingElement.Configuration.State) {
        _loadStartedTurbine.add(configuration)
    }

    override fun onLoadSucceeded(
        paymentMethods: List<String>,
        content: PaymentMethodMessagingContent
    ) {
        _loadSucceededTurbine.add(
            LoadSucceededCall(
                paymentMethods = paymentMethods,
                content = content
            )
        )
    }

    override fun onLoadFailed(error: Throwable) {
        _loadFailedTurbine.add(error)
    }

    override fun onElementDisplayed(appearance: PaymentMethodMessagingElement.Appearance.State) {
        _elementDisplayedTurbine.add(Unit)
    }

    override fun onElementTapped() {
        _elementTappedTurbine.add(Unit)
    }

    class LoadSucceededCall(
        val paymentMethods: List<String>,
        val content: PaymentMethodMessagingContent
    )
}
