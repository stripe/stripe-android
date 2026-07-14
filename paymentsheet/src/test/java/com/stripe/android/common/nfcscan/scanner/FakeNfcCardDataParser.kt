package com.stripe.android.common.nfcscan.scanner

import app.cash.turbine.Turbine

internal class FakeNfcCardDataParser(
    private val parseResult: ScannedCardData? = null,
    private val canParseResult: Boolean? = null,
) : NfcCardDataParser {
    val parseCalls = Turbine<Map<String, ByteArray>>()
    val canParseCalls = Turbine<Map<String, ByteArray>>()

    private val defaultParser = DefaultNfcCardDataParser()

    override fun canParse(records: Map<String, ByteArray>): Boolean {
        canParseCalls.add(records)
        return canParseResult ?: defaultParser.canParse(records)
    }

    override fun parse(records: Map<String, ByteArray>): ScannedCardData? {
        parseCalls.add(records)
        return parseResult
    }

    fun ensureAllEventsConsumed() {
        parseCalls.ensureAllEventsConsumed()
    }
}
