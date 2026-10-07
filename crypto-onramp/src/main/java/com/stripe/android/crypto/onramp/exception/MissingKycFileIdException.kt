package com.stripe.android.crypto.onramp.exception

internal class MissingKycFileIdException :
    IllegalStateException("Uploaded KYC document is missing a file ID")
