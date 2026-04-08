package com.stripe.android.crypto.onramp.exception

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

@ExperimentalCryptoOnramp
class PaymentFailedException internal constructor() : IllegalStateException("Payment failed")
