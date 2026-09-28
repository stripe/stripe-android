package com.stripe.android.common.nfcscan.scanner

import app.cash.turbine.Turbine
import com.stripe.android.core.strings.resolvableString

internal class FakeNfcCardReader(
    private val result: NfcCardReader.Result = NfcCardReader.Result.Error(
        error = GenericNfcScanningError(
            errorCode = "notImplemented",
            userMessage = resolvableString("Not implemented"),
        ),
    ),
) : NfcCardReader {
    val readCardCalls = Turbine<NfcTagTransceiver>()

    override suspend fun readCard(transceiver: NfcTagTransceiver): NfcCardReader.Result {
        readCardCalls.add(transceiver)
        return result
    }

    fun ensureAllEventsConsumed() {
        readCardCalls.ensureAllEventsConsumed()
    }
}
