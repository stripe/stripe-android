package com.stripe.android.stripe3ds2.init

internal fun interface DeviceParamNotAvailableFactory {
    fun create(): Map<String, String>
}
