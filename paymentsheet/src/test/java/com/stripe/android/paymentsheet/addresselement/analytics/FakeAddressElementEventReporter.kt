package com.stripe.android.paymentsheet.addresselement.analytics

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.paymentsheet.addresselement.AddressDetails

internal class FakeAddressElementEventReporter : AddressElementEventReporter {
    var onBeforeCanceled: (() -> Unit)? = null

    private val _shownCalls = Turbine<ShownCall>()
    val shownCalls: ReceiveTurbine<ShownCall> = _shownCalls

    private val _saveStartedCalls = Turbine<AddressCall>()
    val saveStartedCalls: ReceiveTurbine<AddressCall> = _saveStartedCalls

    private val _saveFailedCalls = Turbine<SaveFailedCall>()
    val saveFailedCalls: ReceiveTurbine<SaveFailedCall> = _saveFailedCalls

    private val _saveCompletedCalls = Turbine<AddressCall>()
    val saveCompletedCalls: ReceiveTurbine<AddressCall> = _saveCompletedCalls

    private val _canceledCalls = Turbine<AddressCall>()
    val canceledCalls: ReceiveTurbine<AddressCall> = _canceledCalls

    override fun onShown(country: String?) {
        _shownCalls.add(ShownCall(country))
    }

    override fun onSaveStarted(
        addressDetails: AddressDetails,
        autocompleteAddressDetails: AddressDetails?,
    ) {
        _saveStartedCalls.add(AddressCall(addressDetails, autocompleteAddressDetails))
    }

    override fun onSaveFailed(
        addressDetails: AddressDetails,
        autocompleteAddressDetails: AddressDetails?,
        error: Throwable,
    ) {
        _saveFailedCalls.add(
            SaveFailedCall(
                addressCall = AddressCall(addressDetails, autocompleteAddressDetails),
                error = error,
            )
        )
    }

    override fun onSaveCompleted(
        addressDetails: AddressDetails,
        autocompleteAddressDetails: AddressDetails?,
    ) {
        _saveCompletedCalls.add(AddressCall(addressDetails, autocompleteAddressDetails))
    }

    override fun onCanceled(
        addressDetails: AddressDetails,
        autocompleteAddressDetails: AddressDetails?,
    ) {
        onBeforeCanceled?.invoke()
        _canceledCalls.add(AddressCall(addressDetails, autocompleteAddressDetails))
    }

    fun validate() {
        _shownCalls.ensureAllEventsConsumed()
        _saveStartedCalls.ensureAllEventsConsumed()
        _saveFailedCalls.ensureAllEventsConsumed()
        _saveCompletedCalls.ensureAllEventsConsumed()
        _canceledCalls.ensureAllEventsConsumed()
    }

    data class ShownCall(val country: String?)

    data class AddressCall(
        val addressDetails: AddressDetails,
        val autocompleteAddressDetails: AddressDetails?,
    )

    data class SaveFailedCall(
        val addressCall: AddressCall,
        val error: Throwable,
    )
}
