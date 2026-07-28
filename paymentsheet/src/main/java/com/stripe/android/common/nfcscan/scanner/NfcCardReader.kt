package com.stripe.android.common.nfcscan.scanner

import com.stripe.android.common.nfcscan.scanner.apdu.GetProcessingOptionsCommand
import com.stripe.android.common.nfcscan.scanner.apdu.ReadRecordCommand
import com.stripe.android.common.nfcscan.scanner.apdu.SelectApplicationCommand
import com.stripe.android.common.nfcscan.scanner.apdu.SelectPpseCommand
import com.stripe.android.common.nfcscan.scanner.apdu.pdol.PdolBuilder
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
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
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val pdolBuilder: PdolBuilder,
    private val errorMapper: NfcCardReader.ErrorCreator,
    private val cardDataParser: NfcCardDataParser,
) : NfcCardReader {
    override suspend fun readCard(transceiver: NfcTagTransceiver): NfcCardReader.Result {
        return runCatching {
            readFromTransceiver(transceiver)
        }.fold(
            onSuccess = { cardData ->
                NfcCardReader.Result.Found(scannedCardData = cardData)
            },
            onFailure = errorMapper::create,
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

            val pdolData = pdolBuilder.fromTemplate(
                paymentMethodMetadata = paymentMethodMetadata,
                template = pdolTemplate,
            )

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
                ?: throw IllegalStateException("Could not parse card data from NFC tag")
        } finally {
            transceiver.close()
        }
    }
}
