package com.stripe.android.common.nfcscan.security

internal class FakeIsDeveloperOptionsEnabled(
    private val result: Boolean = true,
) : IsDeveloperOptionsEnabled {
    override fun get(): Boolean = result
}
