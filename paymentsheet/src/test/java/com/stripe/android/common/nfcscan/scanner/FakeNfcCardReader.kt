package com.stripe.android.common.nfcscan.scanner

import app.cash.turbine.Turbine

internal class FakeNfcCardReader(
    private val result: Result<ScannedCardData> = Result.failure(NotImplementedError()),
) : NfcCardReader {
    val readCardCalls = Turbine<NfcTagTransceiver>()

    override suspend fun readCard(transceiver: NfcTagTransceiver): Result<ScannedCardData> {
        readCardCalls.add(transceiver)
        return result
    }

    fun ensureAllEventsConsumed() {
        readCardCalls.ensureAllEventsConsumed()
    }
}
