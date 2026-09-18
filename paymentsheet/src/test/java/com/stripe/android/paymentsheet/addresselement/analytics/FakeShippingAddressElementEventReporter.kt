package com.stripe.android.paymentsheet.addresselement.analytics

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine

internal class FakeShippingAddressElementEventReporter : ShippingAddressElementEventReporter {
    private val _shownCalls = Turbine<ShippingAddressElementAnalyticsData>()
    val shownCalls: ReceiveTurbine<ShippingAddressElementAnalyticsData> = _shownCalls

    private val _canceledCalls = Turbine<ShippingAddressElementAnalyticsData>()
    val canceledCalls: ReceiveTurbine<ShippingAddressElementAnalyticsData> = _canceledCalls

    private val _saveStartedCalls = Turbine<ShippingAddressElementAnalyticsData>()
    val saveStartedCalls: ReceiveTurbine<ShippingAddressElementAnalyticsData> = _saveStartedCalls

    private val _saveFailedCalls = Turbine<SaveFailedCall>()
    val saveFailedCalls: ReceiveTurbine<SaveFailedCall> = _saveFailedCalls

    private val _saveCompletedCalls = Turbine<ShippingAddressElementAnalyticsData>()
    val saveCompletedCalls: ReceiveTurbine<ShippingAddressElementAnalyticsData> = _saveCompletedCalls

    override fun onShown(addressData: ShippingAddressElementAnalyticsData) {
        _shownCalls.add(addressData)
    }

    override fun onCanceled(addressData: ShippingAddressElementAnalyticsData) {
        _canceledCalls.add(addressData)
    }

    override fun onSaveStarted(addressData: ShippingAddressElementAnalyticsData) {
        _saveStartedCalls.add(addressData)
    }

    override fun onSaveFailed(addressData: ShippingAddressElementAnalyticsData, error: Throwable) {
        _saveFailedCalls.add(SaveFailedCall(addressData, error))
    }

    override fun onSaveCompleted(addressData: ShippingAddressElementAnalyticsData) {
        _saveCompletedCalls.add(addressData)
    }

    fun ensureAllEventsConsumed() {
        _shownCalls.ensureAllEventsConsumed()
        _canceledCalls.ensureAllEventsConsumed()
        _saveStartedCalls.ensureAllEventsConsumed()
        _saveFailedCalls.ensureAllEventsConsumed()
        _saveCompletedCalls.ensureAllEventsConsumed()
    }

    data class SaveFailedCall(
        val addressData: ShippingAddressElementAnalyticsData,
        val error: Throwable,
    )
}
