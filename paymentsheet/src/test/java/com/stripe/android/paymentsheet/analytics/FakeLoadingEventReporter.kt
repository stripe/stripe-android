package com.stripe.android.paymentsheet.analytics

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.model.LinkDisabledReason
import com.stripe.android.model.LinkMode
import com.stripe.android.model.LinkSignupDisabledReason
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader

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
        linkEnabled: Boolean,
        linkMode: LinkMode?,
        linkDisabledReasons: List<LinkDisabledReason>?,
        linkSignupDisabledReasons: List<LinkSignupDisabledReason>?,
        googlePaySupported: Boolean,
        linkDisplay: PaymentSheet.LinkConfiguration.Display,
        currency: String?,
        initializationMode: PaymentElementLoader.InitializationMode,
        financialConnectionsAvailability: FinancialConnectionsAvailability?,
        orderedLpms: List<String>,
        requireCvcRecollection: Boolean,
        hasDefaultPaymentMethod: Boolean?,
        setAsDefaultEnabled: Boolean?,
        paymentMethodOptionsSetupFutureUsage: Boolean,
        setupFutureUsage: StripeIntent.Usage?,
    ) {
        _loadSucceededTurbine.add(
            LoadSucceededCall(
                paymentSelection = paymentSelection,
                linkEnabled = linkEnabled,
                linkMode = linkMode,
                linkDisabledReasons = linkDisabledReasons,
                linkSignupDisabledReasons = linkSignupDisabledReasons,
                googlePaySupported = googlePaySupported,
                linkDisplay = linkDisplay,
                currency = currency,
                initializationMode = initializationMode,
                financialConnectionsAvailability = financialConnectionsAvailability,
                orderedLpms = orderedLpms,
                requireCvcRecollection = requireCvcRecollection,
                hasDefaultPaymentMethod = hasDefaultPaymentMethod,
                setAsDefaultEnabled = setAsDefaultEnabled,
                paymentMethodOptionsSetupFutureUsage = paymentMethodOptionsSetupFutureUsage,
                setupFutureUsage = setupFutureUsage,
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
        val linkEnabled: Boolean,
        val linkMode: LinkMode?,
        val linkDisabledReasons: List<LinkDisabledReason>?,
        val linkSignupDisabledReasons: List<LinkSignupDisabledReason>?,
        val googlePaySupported: Boolean,
        val linkDisplay: PaymentSheet.LinkConfiguration.Display,
        val currency: String?,
        val initializationMode: PaymentElementLoader.InitializationMode,
        val financialConnectionsAvailability: FinancialConnectionsAvailability?,
        val orderedLpms: List<String>,
        val requireCvcRecollection: Boolean,
        val hasDefaultPaymentMethod: Boolean?,
        val setAsDefaultEnabled: Boolean?,
        val paymentMethodOptionsSetupFutureUsage: Boolean,
        val setupFutureUsage: StripeIntent.Usage?,
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
