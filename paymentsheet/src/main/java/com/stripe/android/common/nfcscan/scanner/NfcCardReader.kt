package com.stripe.android.common.nfcscan.scanner

import com.stripe.android.common.nfcscan.scanner.apdu.ApduResponseError
import com.stripe.android.common.nfcscan.scanner.apdu.ReadRecordCommand
import com.stripe.android.common.nfcscan.scanner.apdu.SelectApplicationCommand
import com.stripe.android.common.nfcscan.scanner.apdu.SelectPpseCommand
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.strings.ResolvableString
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.collections.plusAssign
import kotlin.coroutines.CoroutineContext

internal interface NfcCardReader {
    interface ErrorCreator {
        fun create(error: Throwable): Result.Error
    }

    suspend fun readCard(transceiver: NfcTagTransceiver): Result

    sealed interface Result {
        data class Found(val scannedCardData: ScannedCardData) : Result
        data class Error(
            val errorCode: String,
            val userMessage: ResolvableString,
        ) : Result
    }
}

internal class ApduCardReader @Inject constructor(
    @IOContext private val workContext: CoroutineContext,
    private val errorMapper: NfcCardReader.ErrorCreator,
    private val cardDataParser: NfcCardDataParser,
) : NfcCardReader {
    override suspend fun readCard(transceiver: NfcTagTransceiver): NfcCardReader.Result {
        return runCatching {
            readFromTransceiver(transceiver)
        }.fold(
            onSuccess = { parseResult ->
                when (parseResult) {
                    is NfcCardDataParser.Result.Success ->
                        NfcCardReader.Result.Found(scannedCardData = parseResult.cardData)
                    is NfcCardDataParser.Result.Error ->
                        NfcCardReader.Result.Error(
                            errorCode = parseResult.analyticsValue,
                            userMessage = parseResult.userMessage,
                        )
                }
            },
            onFailure = errorMapper::create,
        )
    }

    private suspend fun readFromTransceiver(
        transceiver: NfcTagTransceiver
    ): NfcCardDataParser.Result = withContext(workContext) {
        try {
            transceiver.open()
            val applicationIdentifier = SelectPpseCommand.transceiveWith(transceiver).getOrThrow()
            SelectApplicationCommand(applicationIdentifier).transceiveWith(transceiver).getOrThrow()

            val records = mutableMapOf<String, ByteArray>()

            for (sfi in PROBE_SFIS) {
                for (record in 1..MAX_RECORDS_PER_SFI) {
                    val result = ReadRecordCommand(record, sfi)
                        .transceiveWith(transceiver)

                    result.onSuccess { result ->
                        records += result
                    }.onFailure { error ->
                        if (!isRecordNotFoundError(error)) {
                            throw error
                        }
                    }
                }
            }

            cardDataParser.parse(records)
        } finally {
            transceiver.close()
        }
    }

    private fun isRecordNotFoundError(error: Throwable): Boolean {
        return error is ApduResponseError.Command &&
            error.sw1 == RECORD_NOT_FOUND_SW1 && error.sw2 == RECORD_NOT_FOUND_SW2
    }

    private companion object {
        // SFIs 1-3 cover virtually all Visa/Mastercard/Amex/Discover payment records.
        val PROBE_SFIS = 1..3
        const val MAX_RECORDS_PER_SFI = 8

        const val RECORD_NOT_FOUND_SW1 = 0x6A.toByte()
        const val RECORD_NOT_FOUND_SW2 = 0x83.toByte()
    }
}
