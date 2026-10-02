package com.stripe.android.paymentsheet.addresselement

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AddressElementResultStateHolder @Inject constructor() {
    private val _formEnabled = MutableStateFlow(true)
    val formEnabled: StateFlow<Boolean> = _formEnabled.asStateFlow()

    private val _result = MutableStateFlow<AddressElementActivityContract.Result?>(null)

    val result: StateFlow<AddressElementActivityContract.Result?> = _result.asStateFlow()

    fun setFormEnabled(isEnabled: Boolean) {
        _formEnabled.value = isEnabled
    }

    fun onUserCancel() {
        if (formEnabled.value) {
            setResult(AddressElementActivityContract.Result.Canceled)
        }
    }

    fun setResult(result: AddressElementActivityContract.Result): Boolean =
        _result.compareAndSet(expect = null, update = result)
}
