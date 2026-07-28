package com.stripe.android.common.nfcscan.scanner

import app.cash.turbine.Turbine

internal class FakeNfcCardDataParser(
    private val parseResult: ScannedCardData? = null,
) : NfcCardDataParser {
    val parseCalls = Turbine<Map<String, ByteArray>>()

    override fun parse(records: Map<String, ByteArray>): ScannedCardData? {
        parseCalls.add(records)
        return parseResult
    }

    fun ensureAllEventsConsumed() {
        parseCalls.ensureAllEventsConsumed()
    }
}
