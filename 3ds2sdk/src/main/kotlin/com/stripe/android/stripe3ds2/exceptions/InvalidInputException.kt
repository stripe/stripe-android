package com.stripe.android.stripe3ds2.exceptions

// NOTE: Copied from reference app spec

/**
 * The InvalidInputException class shall represent a run-time exception that occurs due to one of
 * the following reasons:
 * - Parameter value is mandatory, but was not provided.
 * - Parameter value does not conform to the specified format.
 * - Parameter value exceeds the maximum limit.
 * - Parameter value does not meet the minimum length criteria.
 */
class InvalidInputException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause) {
    constructor(t: Throwable) : this(
        t.message.orEmpty(),
        t.cause
    )
}
