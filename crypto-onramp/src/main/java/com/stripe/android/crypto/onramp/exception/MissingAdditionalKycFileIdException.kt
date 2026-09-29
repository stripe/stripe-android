package com.stripe.android.crypto.onramp.exception

internal class MissingAdditionalKycFileIdException :
    IllegalStateException("Uploaded additional KYC document is missing a file ID")
