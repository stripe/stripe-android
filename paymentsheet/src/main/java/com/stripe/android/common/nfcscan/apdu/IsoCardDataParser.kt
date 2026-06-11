package com.stripe.android.common.nfcscan.apdu

import com.stripe.android.common.nfcscan.ScannedCardData
import javax.inject.Inject
import kotlin.collections.joinToString

internal interface IsoCardDataParser {
    fun parse(records: Map<String, ByteArray>): ScannedCardData?
}

internal class DefaultIsoCardDataParser @Inject constructor() : IsoCardDataParser {
    override fun parse(records: Map<String, ByteArray>): ScannedCardData? {
        records[TAG_TRACK2]?.let { return parseFromTrack2(it) }

        val panBytes = records[TAG_PAN] ?: return null
        val expiryBytes = records[TAG_EXPIRY] ?: return null
        val pan = panBytes.joinToString("") { "%02X".format(it) }.trimEnd('F', 'f')
        val (month, year) = parseExpiry(expiryBytes) ?: return null
        return ScannedCardData(pan, month, year)
    }

    private fun parseFromTrack2(bytes: ByteArray): ScannedCardData? {
        val hex = bytes.joinToString("") { "%02X".format(it) }
        val sep = hex.indexOf('D').takeIf { it >= 0 } ?: return null
        val pan = hex.substring(0, sep)
        if (hex.length < sep + 5) return null
        val yy = hex.substring(sep + 1, sep + 3).toIntOrNull() ?: return null
        val mm = hex.substring(sep + 3, sep + 5).toIntOrNull() ?: return null
        return ScannedCardData(pan, mm, 2000 + yy)
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
    }
}
