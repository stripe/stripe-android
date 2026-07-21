package com.stripe.android.common.nfcscan.scanner

import app.cash.turbine.Turbine
import com.stripe.android.core.strings.resolvableString

internal class FakeNfcCardReaderErrorCreator(
    private val result: NfcCardReader.Result.Error = NfcCardReader.Result.Error(
        errorCode = "testError",
        userMessage = resolvableString("test error"),
    ),
) : NfcCardReader.ErrorCreator {
    val createCalls = Turbine<Throwable>()
    val createFromRecordsCalls = Turbine<Map<String, ByteArray>>()

    override fun create(error: Throwable): NfcCardReader.Result.Error {
        createCalls.add(error)
        return result
    }

    override fun create(records: MutableMap<String, ByteArray>): NfcCardReader.Result.Error {
        createFromRecordsCalls.add(records)
        return result
    }

    fun ensureAllEventsConsumed() {
        createCalls.ensureAllEventsConsumed()
        createFromRecordsCalls.ensureAllEventsConsumed()
    }
}
