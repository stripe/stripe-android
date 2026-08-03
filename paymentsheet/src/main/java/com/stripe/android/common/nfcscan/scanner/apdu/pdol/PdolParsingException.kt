package com.stripe.android.common.nfcscan.scanner.apdu.pdol

internal class PdolParsingException(
    override val cause: Throwable,
) : Exception("Failed to parse PDOL template", cause)
