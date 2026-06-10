package com.stripe.android.common.nfcscan.apdu

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
    suspend fun readCard(transceiver: IsoNfcTagTransceiver): Result<NfcCardData>
}

internal class DefaultIsoCardReader @Inject constructor(
    @IOContext private val workContext: CoroutineContext,
) : IsoCardReader {
    override suspend fun readCard(
        transceiver: IsoNfcTagTransceiver
    ): Result<NfcCardData> = withContext(workContext) {
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

            parseCardData(records)
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

    private fun parseCardData(tlv: Map<String, ByteArray>): NfcCardData? {
        tlv[TAG_TRACK2]?.let { return parseFromTrack2(it) }

        val panBytes = tlv[TAG_PAN] ?: return null
        val expiryBytes = tlv[TAG_EXPIRY] ?: return null
        val pan = panBytes.joinToString("") { "%02X".format(it) }.trimEnd('F', 'f')
        val (month, year) = parseExpiry(expiryBytes) ?: return null
        return NfcCardData(pan, month, year)
    }

    private fun parseFromTrack2(bytes: ByteArray): NfcCardData? {
        val hex = bytes.joinToString("") { "%02X".format(it) }
        val sep = hex.indexOf('D').takeIf { it >= 0 } ?: return null
        val pan = hex.substring(0, sep)
        if (hex.length < sep + 5) return null
        val yy = hex.substring(sep + 1, sep + 3).toIntOrNull() ?: return null
        val mm = hex.substring(sep + 3, sep + 5).toIntOrNull() ?: return null
        return NfcCardData(pan, mm, 2000 + yy)
    }

    private fun parseExpiry(bytes: ByteArray): Pair<Int, Int>? {
        if (bytes.size < 3) return null
        val yearBcd = bytes[0].toInt() and 0xFF
        val monthBcd = bytes[1].toInt() and 0xFF
        val year = 2000 + ((yearBcd shr 4) * 10 + (yearBcd and 0x0F))
        val month = (monthBcd shr 4) * 10 + (monthBcd and 0x0F)
        return month to year
    }

    private companion object {
        const val TAG_TRACK2 = "57"
        const val TAG_PAN = "5A"
        const val TAG_EXPIRY = "5F24"

        // SFIs 1-3 cover virtually all Visa/Mastercard/Amex/Discover payment records.
        val PROBE_SFIS = 1..3
        const val MAX_RECORDS_PER_SFI = 8
    }
}
