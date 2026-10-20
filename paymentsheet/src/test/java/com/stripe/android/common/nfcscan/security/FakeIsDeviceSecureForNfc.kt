package com.stripe.android.common.nfcscan.security

internal class FakeIsDeviceSecureForNfc(
    var result: Boolean = true,
) : IsDeviceSecureForNfc {
    override fun get(): Boolean = result
}
