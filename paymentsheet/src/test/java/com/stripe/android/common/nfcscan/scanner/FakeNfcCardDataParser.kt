package com.stripe.android.common.nfcscan.scanner

import app.cash.turbine.Turbine

internal class FakeNfcCardDataParser(
    private val parseResult: NfcCardDataParser.Result? = null,
) : NfcCardDataParser {
    val parseCalls = Turbine<Map<String, ByteArray>>()

    private val defaultParser = DefaultNfcCardDataParser()

    override fun parse(records: Map<String, ByteArray>): NfcCardDataParser.Result {
        parseCalls.add(records)
        return parseResult ?: defaultParser.parse(records)
    }

    fun ensureAllEventsConsumed() {
        parseCalls.ensureAllEventsConsumed()
    }
}
