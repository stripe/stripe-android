package com.stripe.android.common.nfcscan.scanner

import com.stripe.android.common.nfcscan.scanner.apdu.SelectPpseCommand
import com.stripe.android.core.injection.IOContext
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal interface NfcCardReader {
    suspend fun readCard(transceiver: NfcTagTransceiver): Result<ScannedCardData>
}

internal class ApduCardReader @Inject constructor(
    @IOContext private val workContext: CoroutineContext,
) : NfcCardReader {
    override suspend fun readCard(
        transceiver: NfcTagTransceiver
    ): Result<ScannedCardData> = withContext(workContext) {
        runCatching {
            SelectPpseCommand.transceiveWith(transceiver).getOrThrow()

            throw IllegalStateException("Could not parse card data from NFC tag")
        }
    }
}
