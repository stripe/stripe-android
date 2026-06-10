package com.stripe.android.common.nfcscan.apdu.commands

internal data class ApplicationFileLocator(
    val recordNumber: Int,
    val shortFileIdentifier: Int,
)
