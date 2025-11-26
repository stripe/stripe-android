package com.stripe.android.challenge.confirmation

internal class BridgeError(
    override val message: String?,
    val type: String?,
    val code: String?
) : Throwable()
