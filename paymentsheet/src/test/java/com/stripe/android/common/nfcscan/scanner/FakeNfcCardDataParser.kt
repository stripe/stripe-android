package com.stripe.android.common.nfcscan.scanner

import app.cash.turbine.Turbine

internal class FakeNfcCardDataParser(
    private val parseResult: ScannedCardData,
    private val parseError: NfcScanningError? = null,
) : NfcCardDataParser {
    val parseCalls = Turbine<Map<String, ByteArray>>()

    override fun parse(records: Map<String, ByteArray>): ScannedCardData {
        parseCalls.add(records)

        if (parseError != null) {
            throw parseError
        }

        return parseResult
    }

    fun ensureAllEventsConsumed() {
        parseCalls.ensureAllEventsConsumed()
    }
}
