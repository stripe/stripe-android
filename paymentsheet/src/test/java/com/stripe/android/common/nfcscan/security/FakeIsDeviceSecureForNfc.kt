package com.stripe.android.common.nfcscan.security

internal class FakeIsDeviceSecureForNfc(
    private val result: Boolean = true,
) : IsDeviceSecureForNfc {
    override fun get(): Boolean = result
}
