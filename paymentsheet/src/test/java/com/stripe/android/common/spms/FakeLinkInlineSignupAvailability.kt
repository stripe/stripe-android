package com.stripe.android.common.spms

internal class FakeLinkInlineSignupAvailability(
    private val result: LinkInlineSignupAvailability.Result,
) : LinkInlineSignupAvailability {
    override fun availability(): LinkInlineSignupAvailability.Result = result
}
