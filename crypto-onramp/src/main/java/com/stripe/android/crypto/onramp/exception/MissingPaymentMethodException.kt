package com.stripe.android.crypto.onramp.exception

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

@ExperimentalCryptoOnramp
class MissingPaymentMethodException internal constructor() : IllegalStateException("Missing payment method")
