package com.stripe.android.common.nfcscan.scanner.apdu

internal data class ProcessingOptionsInfo(
    val aflEntries: List<AflEntry>,
    val records: Map<String, ByteArray>,
) {
    internal data class AflEntry(
        val shortFileIdentifier: Int,
        val firstRecord: Int,
        val lastRecord: Int,
    )
}
