package com.stripe.android.crypto.onramp.exception

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

@ExperimentalCryptoOnramp
class MissingConsumerSecretException internal constructor() : IllegalStateException("Missing consumer secret")
