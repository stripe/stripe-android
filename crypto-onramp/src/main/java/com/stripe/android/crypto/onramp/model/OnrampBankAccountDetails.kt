package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/** A snapshot of the selected bank account, not proof of ownership or mandate validity. */
@ExperimentalCryptoOnramp
class OnrampBankAccountDetails internal constructor(
    val last4: String?
)
