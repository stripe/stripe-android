package com.stripe.android.common.nfcscan.hardware

internal class FakeNfcHardwareDelegate(
    private val result: Boolean = true,
) : NfcHardwareDelegate {
    override fun isAvailable(): Boolean = result
}
