package com.stripe.android.challenge.confirmation

internal class BridgeException(
    override val message: String?,
    val type: String?,
    val code: String?,
    override val cause: Throwable? = null,
) : Throwable() {
    constructor(cause: Throwable?) : this(
        message = cause?.message,
        type = null,
        code = null,
        cause = cause
    )
}
