package com.stripe.android.paymentsheet.addresselement

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AddressElementResultStateHolder @Inject constructor() {
    private val _result = MutableStateFlow<AddressLauncherResult?>(null)

    val result: StateFlow<AddressLauncherResult?> = _result.asStateFlow()

    fun setResult(result: AddressLauncherResult) {
        _result.compareAndSet(expect = null, update = result)
    }
}
