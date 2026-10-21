package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Bank account summary for the funding instrument used to create a crypto payment token.
 *
 * @param last4 The last four digits of the account number, when available. These do not attest
 * account ownership, settlement speed, mandate validity, or successful payment.
 */
@ExperimentalCryptoOnramp
class OnrampBankAccountDetails internal constructor(
    val last4: String?,
)
