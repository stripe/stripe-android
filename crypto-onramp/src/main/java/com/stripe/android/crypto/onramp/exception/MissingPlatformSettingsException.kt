package com.stripe.android.crypto.onramp.exception

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

@ExperimentalCryptoOnramp
class MissingPlatformSettingsException : IllegalStateException("Failed to retrieve platform settings")
