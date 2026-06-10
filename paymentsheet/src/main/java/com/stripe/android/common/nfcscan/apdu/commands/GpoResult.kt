package com.stripe.android.common.nfcscan.apdu.commands

internal data class GpoResult(
    val applicationFileLocators: List<ApplicationFileLocator>,
    val embeddedRecords: Map<String, ByteArray>,
)
