package com.stripe.android.stripe3ds2.exceptions

// NOTE: Copied from reference app spec

/**
 * Thrown when internal error is encountered by the 3DS2 SDK.
 */
class SDKRuntimeException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause) {
    constructor(t: Throwable) : this(
        t.message.orEmpty(),
        t.cause
    )
}
