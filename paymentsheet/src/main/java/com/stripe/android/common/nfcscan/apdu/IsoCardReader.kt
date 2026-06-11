package com.stripe.android.common.nfcscan.apdu

import com.stripe.android.common.nfcscan.ScannedCardData
import com.stripe.android.common.nfcscan.apdu.commands.AdpuResponseError
import com.stripe.android.common.nfcscan.apdu.commands.ReadRecordCommand
import com.stripe.android.common.nfcscan.apdu.commands.SelectApplicationCommand
import com.stripe.android.common.nfcscan.apdu.commands.SelectPpseCommand
import com.stripe.android.core.injection.IOContext
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal interface IsoCardReader {
    suspend fun readCard(transceiver: IsoNfcTagTransceiver): Result<ScannedCardData>
}

internal class DefaultIsoCardReader @Inject constructor(
    @IOContext private val workContext: CoroutineContext,
    private val cardDataParser: IsoCardDataParser,
) : IsoCardReader {
    override suspend fun readCard(
        transceiver: IsoNfcTagTransceiver
    ): Result<ScannedCardData> = withContext(workContext) {
        try {
            val applicationIdentifier = SelectPpseCommand.transceiveWith(transceiver).getOrThrow()
            SelectApplicationCommand(applicationIdentifier).transceiveWith(transceiver)

            val records = mutableMapOf<String, ByteArray>()

            for (sfi in PROBE_SFIS) {
                for (record in 1..MAX_RECORDS_PER_SFI) {
                    val result = ReadRecordCommand(record, sfi)
                        .transceiveWith(transceiver)

                    if (result.isFailure) continue

                    records += result.getOrThrow()
                }
            }

            cardDataParser.parse(records)
                ?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("Could not parse card data from NFC tag"))
        } catch (e: AdpuResponseError) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: SecurityException) {
            Result.failure(e)
        }
    }

    private companion object {
        // SFIs 1-3 cover virtually all Visa/Mastercard/Amex/Discover payment records.
        val PROBE_SFIS = 1..3
        const val MAX_RECORDS_PER_SFI = 8
    }
}
