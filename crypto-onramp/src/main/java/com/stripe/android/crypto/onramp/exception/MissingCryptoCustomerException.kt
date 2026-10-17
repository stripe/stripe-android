package com.stripe.android.crypto.onramp.exception

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

@ExperimentalCryptoOnramp
class MissingCryptoCustomerException internal constructor() : IllegalStateException("Missing crypto customer ID")
