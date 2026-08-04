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

    override fun create(error: Throwable): NfcCardReader.Result.Error {
        createCalls.add(error)
        return result
    }

    fun ensureAllEventsConsumed() {
        createCalls.ensureAllEventsConsumed()
    }
}
