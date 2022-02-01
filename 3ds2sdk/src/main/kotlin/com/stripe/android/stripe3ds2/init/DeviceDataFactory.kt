package com.stripe.android.stripe3ds2.init

fun interface DeviceDataFactory {
    fun create(): Map<String, Any?>
}
