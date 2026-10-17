package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

@ExperimentalCryptoOnramp
fun interface OnrampVerifyKycCallback {
    fun onResult(result: OnrampVerifyKycInfoResult)
}

@ExperimentalCryptoOnramp
sealed interface OnrampVerifyKycInfoResult {
    /**
     * KYC verified successfully.
     */
    @ExperimentalCryptoOnramp
    class Confirmed internal constructor() : OnrampVerifyKycInfoResult

    /**
     * The user indicated they need to update their address.
     */
    @ExperimentalCryptoOnramp
    class UpdateAddress internal constructor() : OnrampVerifyKycInfoResult

    /**
     * The KYC verification was cancelled.
     */
    @ExperimentalCryptoOnramp
    class Cancelled internal constructor() : OnrampVerifyKycInfoResult

    /**
     * Verification failed.
     * @param error The error that caused the failure
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampVerifyKycInfoResult
}
