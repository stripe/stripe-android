package com.stripe.android.paymentsheet.analytics

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.model.PaymentSelection

internal class FakeLoadingEventReporter : LoadingEventReporter {
    private val _loadStartedTurbine = Turbine<LoadStartedCall>()
    val loadStartedTurbine: ReceiveTurbine<LoadStartedCall> = _loadStartedTurbine

    private val _loadSucceededTurbine = Turbine<LoadSucceededCall>()
    val loadSucceededTurbine: ReceiveTurbine<LoadSucceededCall> = _loadSucceededTurbine

    private val _loadFailedTurbine = Turbine<LoadFailedCall>()
    val loadFailedTurbine: ReceiveTurbine<LoadFailedCall> = _loadFailedTurbine

    private val _elementsSessionLoadFailedTurbine = Turbine<ElementsSessionLoadFailedCall>()
    val elementsSessionLoadFailedTurbine: ReceiveTurbine<ElementsSessionLoadFailedCall> =
        _elementsSessionLoadFailedTurbine

    private val _lpmSpecFailureTurbine = Turbine<LpmSpecFailureCall>()
    val lpmSpecFailureTurbine: ReceiveTurbine<LpmSpecFailureCall> = _lpmSpecFailureTurbine

    fun validate() {
        _loadStartedTurbine.ensureAllEventsConsumed()
        _loadSucceededTurbine.ensureAllEventsConsumed()
        _loadFailedTurbine.ensureAllEventsConsumed()
        _elementsSessionLoadFailedTurbine.ensureAllEventsConsumed()
        _lpmSpecFailureTurbine.ensureAllEventsConsumed()
    }

    override fun onLoadStarted(initializedViaCompose: Boolean) {
        _loadStartedTurbine.add(
            LoadStartedCall(
                initializedViaCompose = initializedViaCompose,
            )
        )
    }

    override fun onLoadSucceeded(
        paymentSelection: PaymentSelection?,
        paymentMethodMetadata: PaymentMethodMetadata,
    ) {
        _loadSucceededTurbine.add(
            LoadSucceededCall(
                paymentSelection = paymentSelection,
                paymentMethodMetadata = paymentMethodMetadata,
            )
        )
    }

    override fun onLoadFailed(error: Throwable) {
        _loadFailedTurbine.add(
            LoadFailedCall(
                error = error,
            )
        )
    }

    override fun onElementsSessionLoadFailed(error: Throwable) {
        _elementsSessionLoadFailedTurbine.add(
            ElementsSessionLoadFailedCall(
                error = error,
            )
        )
    }

    override fun onLpmSpecFailure(errorMessage: String?) {
        _lpmSpecFailureTurbine.add(
            LpmSpecFailureCall(
                errorMessage = errorMessage,
            )
        )
    }

    class LoadStartedCall(
        val initializedViaCompose: Boolean,
    )

    class LoadSucceededCall(
        val paymentSelection: PaymentSelection?,
        val paymentMethodMetadata: PaymentMethodMetadata,
    )

    class LoadFailedCall(
        val error: Throwable,
    )

    class ElementsSessionLoadFailedCall(
        val error: Throwable,
    )

    class LpmSpecFailureCall(
        val errorMessage: String?,
    )
}
