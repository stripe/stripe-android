package com.stripe.android.paymentsheet.addresselement.analytics

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.paymentsheet.addresselement.AddressDetails

internal class FakeAddressElementEventReporter : AddressElementEventReporter {
    private val _shownCalls = Turbine<ShownCall>()
    val shownCalls: ReceiveTurbine<ShownCall> = _shownCalls

    private val _canceledCalls = Turbine<AnalyticsCall>()
    val canceledCalls: ReceiveTurbine<AnalyticsCall> = _canceledCalls

    private val _saveStartedCalls = Turbine<AnalyticsCall>()
    val saveStartedCalls: ReceiveTurbine<AnalyticsCall> = _saveStartedCalls

    private val _saveFailedCalls = Turbine<SaveFailedCall>()
    val saveFailedCalls: ReceiveTurbine<SaveFailedCall> = _saveFailedCalls

    private val _saveCompletedCalls = Turbine<AnalyticsCall>()
    val saveCompletedCalls: ReceiveTurbine<AnalyticsCall> = _saveCompletedCalls

    override fun onShown(country: String?) {
        _shownCalls.add(ShownCall(country))
    }

    override fun onCanceled(
        addressDetails: AddressDetails,
        autocompleteAddressDetails: AddressDetails?,
    ) {
        _canceledCalls.add(AnalyticsCall(addressDetails, autocompleteAddressDetails))
    }

    override fun onSaveStarted(
        addressDetails: AddressDetails,
        autocompleteAddressDetails: AddressDetails?,
    ) {
        _saveStartedCalls.add(AnalyticsCall(addressDetails, autocompleteAddressDetails))
    }

    override fun onSaveFailed(
        addressDetails: AddressDetails,
        autocompleteAddressDetails: AddressDetails?,
        error: Throwable,
    ) {
        _saveFailedCalls.add(
            SaveFailedCall(
                analyticsCall = AnalyticsCall(addressDetails, autocompleteAddressDetails),
                error = error,
            )
        )
    }

    override fun onSaveCompleted(
        addressDetails: AddressDetails,
        autocompleteAddressDetails: AddressDetails?,
    ) {
        _saveCompletedCalls.add(AnalyticsCall(addressDetails, autocompleteAddressDetails))
    }

    fun ensureAllEventsConsumed() {
        _shownCalls.ensureAllEventsConsumed()
        _canceledCalls.ensureAllEventsConsumed()
        _saveStartedCalls.ensureAllEventsConsumed()
        _saveFailedCalls.ensureAllEventsConsumed()
        _saveCompletedCalls.ensureAllEventsConsumed()
    }

    data class ShownCall(
        val country: String?,
    )

    data class AnalyticsCall(
        val addressDetails: AddressDetails,
        val autocompleteAddressDetails: AddressDetails?,
    )

    data class SaveFailedCall(
        val analyticsCall: AnalyticsCall,
        val error: Throwable,
    )
}
