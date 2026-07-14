package com.stripe.android.common.nfcscan.scanner

import com.stripe.android.common.nfcscan.scanner.apdu.ReadRecordCommand
import com.stripe.android.common.nfcscan.scanner.apdu.SelectApplicationCommand
import com.stripe.android.common.nfcscan.scanner.apdu.SelectPpseCommand
import com.stripe.android.core.injection.IOContext
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.collections.plusAssign
import kotlin.coroutines.CoroutineContext

internal interface NfcCardReader {
    suspend fun readCard(transceiver: NfcTagTransceiver): Result<ScannedCardData>
}

internal class ApduCardReader @Inject constructor(
    @IOContext private val workContext: CoroutineContext,
    private val cardDataParser: NfcCardDataParser,
) : NfcCardReader {
    override suspend fun readCard(
        transceiver: NfcTagTransceiver
    ): Result<ScannedCardData> = withContext(workContext) {
        runCatching {
            val applicationIdentifier = SelectPpseCommand.transceiveWith(transceiver).getOrThrow()
            SelectApplicationCommand(applicationIdentifier).transceiveWith(transceiver)

            val records = mutableMapOf<String, ByteArray>()

            probeFiles@ for (sfi in PROBE_SFIS) {
                for (record in 1..MAX_RECORDS_PER_SFI) {
                    val result = ReadRecordCommand(record, sfi)
                        .transceiveWith(transceiver)

                    result.onSuccess { result ->
                        records += result

                        if (cardDataParser.canParse(records)) {
                            break@probeFiles
                        }
                    }
                }
            }

            cardDataParser.parse(records)
                ?: throw IllegalStateException("Could not parse card data from NFC tag")
        }
    }

    private companion object {
        // SFIs 1-3 cover virtually all Visa/Mastercard/Amex/Discover payment records.
        val PROBE_SFIS = 1..3
        const val MAX_RECORDS_PER_SFI = 8
    }
}
