package com.stripe.android.common.nfcscan.scanner

import com.stripe.android.common.nfcscan.scanner.apdu.GetProcessingOptionsCommand
import com.stripe.android.common.nfcscan.scanner.apdu.PdolTemplate
import com.stripe.android.common.nfcscan.scanner.apdu.ReadRecordCommand
import com.stripe.android.common.nfcscan.scanner.apdu.SelectApplicationCommand
import com.stripe.android.common.nfcscan.scanner.apdu.SelectPpseCommand
import com.stripe.android.common.nfcscan.scanner.apdu.pdol.PdolBuilder
import com.stripe.android.core.injection.IOContext
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import kotlin.collections.plusAssign
import kotlin.coroutines.CoroutineContext

internal interface NfcCardReader {
    suspend fun readCard(transceiver: NfcTagTransceiver): Result

    sealed interface Result {
        data class Found(val scannedCardData: ScannedCardData) : Result
        data class Error(val error: Throwable) : Result
    }
}

internal class ApduCardReader @Inject constructor(
    @IOContext private val workContext: CoroutineContext,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val pdolBuilder: PdolBuilder,
    private val cardDataParser: NfcCardDataParser,
) : NfcCardReader {
    override suspend fun readCard(transceiver: NfcTagTransceiver): NfcCardReader.Result {
        return runCatching {
            readFromTransceiver(transceiver)
        }.fold(
            onSuccess = {
                NfcCardReader.Result.Found(it)
            },
            onFailure = {
                NfcCardReader.Result.Error(mapError(it))
            },
        )
    }

    private suspend fun readFromTransceiver(
        transceiver: NfcTagTransceiver
    ): ScannedCardData = withContext(workContext) {
        try {
            transceiver.open()

            val applicationIdentifier = SelectPpseCommand.transceiveWith(transceiver).getOrThrow()

            val pdolTemplate = SelectApplicationCommand(applicationIdentifier)
                .transceiveWith(transceiver)
                .getOrThrow()

            val pdolData = when (pdolTemplate) {
                is PdolTemplate.Available -> pdolBuilder.fromTemplate(
                    paymentMethodMetadata = paymentMethodMetadata,
                    template = pdolTemplate.data,
                )
                else -> byteArrayOf()
            }

            val processingOptionsInfo = GetProcessingOptionsCommand(pdolData)
                .transceiveWith(transceiver)
                .getOrThrow()

            val records = processingOptionsInfo.records.toMutableMap()

            processingOptionsInfo.aflEntries.forEach { entry ->
                for (record in entry.firstRecord..entry.lastRecord) {
                    ReadRecordCommand(record, entry.shortFileIdentifier)
                        .transceiveWith(transceiver)
                        .onSuccess { readRecords ->
                            records += readRecords
                        }
                }
            }

            cardDataParser.parse(records)
        } finally {
            transceiver.close()
        }
    }

    private fun mapError(error: Throwable): Throwable {
        return when (error) {
            is NfcScanningError -> error
            is SecurityException -> GenericNfcScanningError(TRANSCEIVER_SECURITY_ERROR_CODE)
            is IOException -> GenericNfcScanningError(TRANSCEIVER_IO_ERROR_CODE)
            else -> error
        }
    }

    private companion object {
        const val TRANSCEIVER_SECURITY_ERROR_CODE = "nfcTransceiverSecurityError"
        const val TRANSCEIVER_IO_ERROR_CODE = "nfcTransceiverIoError"
    }
}
