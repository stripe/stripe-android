package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp
import com.stripe.android.crypto.onramp.exception.SamsungPayException

/**
 * Detailed result of checking Samsung Pay availability.
 */
@ExperimentalCryptoOnramp
sealed class SamsungPayAvailabilityResult {
    /**
     * Samsung Pay is ready to be presented.
     */
    @ExperimentalCryptoOnramp
    class Available internal constructor() : SamsungPayAvailabilityResult()

    /**
     * Samsung Pay is unavailable. [error] contains a stable reason and recovery guidance.
     */
    @ExperimentalCryptoOnramp
    class Unavailable internal constructor(
        val error: SamsungPayException,
    ) : SamsungPayAvailabilityResult()
}
