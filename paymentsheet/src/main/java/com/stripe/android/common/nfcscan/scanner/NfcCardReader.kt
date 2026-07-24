package com.stripe.android.common.nfcscan.scanner

import com.stripe.android.common.nfcscan.NfcScanLogger
import com.stripe.android.common.nfcscan.scanner.apdu.ApduResponseError
import com.stripe.android.common.nfcscan.scanner.apdu.GetProcessingOptionsCommand
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
            onSuccess = { cardData ->
                NfcCardReader.Result.Found(scannedCardData = cardData)
            },
            onFailure = {
                NfcScanLogger.debug(it.toDebugMessage())

                errorMapper.create(it)
            },
        )
    }

    private suspend fun readFromTransceiver(
        transceiver: NfcTagTransceiver
    ): ScannedCardData = withContext(workContext) {
        try {
            NfcScanLogger.debug("Opening transceiver")
            transceiver.open()
            NfcScanLogger.debug("Selecting PPSE")
            val applicationIdentifier = SelectPpseCommand.transceiveWith(transceiver).getOrThrow()
            NfcScanLogger.debug("Selected AID ${applicationIdentifier.value}")
            val selectedApplication = SelectApplicationCommand(applicationIdentifier)
                .transceiveWith(transceiver)
                .getOrThrow()
            NfcScanLogger.debug("Application selected; getting processing options")
            val processingOptions = GetProcessingOptionsCommand(selectedApplication.processingOptionsDataObjectList)
                .transceiveWith(transceiver)
                .getOrThrow()
            val recordLocators = processingOptions.recordLocators.ifEmpty {
                DEFAULT_RECORD_LOCATORS
            }
            NfcScanLogger.debug("Processing options recordLocators=$recordLocators; probing records")

            val records = processingOptions.records.toMutableMap()
            NfcScanLogger.debug("Processing options tags=${records.mapValues { it.value.size }.toSortedMap()}")

            probeFiles@ for (recordLocator in recordLocators) {
                for (record in recordLocator.firstRecord..recordLocator.lastRecord) {
                    val result = ReadRecordCommand(
                        recordNumber = record,
                        shortFileIdentifier = recordLocator.shortFileIdentifier,
                    )
                        .transceiveWith(transceiver)

                    result.onSuccess { result ->
                        records += result
                        NfcScanLogger.debug(
                            "Read record success sfi=${recordLocator.shortFileIdentifier} record=$record " +
                                "tags=${result.mapValues { it.value.size }.toSortedMap()} " +
                                "allTags=${records.keys.sorted()}"
                        )

                        if (cardDataParser.canParse(records)) {
                            NfcScanLogger.debug("Card data parser has enough tags; stopping record probe")
                            break@probeFiles
                        }
                    }.onFailure { error ->
                        NfcScanLogger.debug(error.toDebugMessage())

                        if (isFileNotFoundError(error)) {
                            // Breaks the record loop but moves on to the next file
                            break
                        }
                    }
                }
            }

            NfcScanLogger.debug("Record probing finished with tags=${records.keys.sorted()}")
            cardDataParser.parse(records)
                ?: throw IllegalStateException("Could not parse card data from NFC tag")
        } finally {
            NfcScanLogger.debug("Closing transceiver")
            transceiver.close()
        }
    }

    private fun isFileNotFoundError(error: Throwable): Boolean {
        return error is ApduResponseError.Command &&
            error.sw1 == PARAMETER_ERROR_SW1 && error.sw2 == FILE_NOT_FOUND_SW2
    }

    private fun Throwable.toDebugMessage(): String {
        return when (this) {
            is ApduResponseError.Invalid -> "Invalid APDU response data bytes=${data.size}"
            is ApduResponseError.Parsing -> {
                "Failed to parse APDU response data bytes=${data.size} cause=${cause?.javaClass?.simpleName}"
            }
            else -> message ?: "Unknown NFC reader error"
        }
    }

    private companion object {
        val DEFAULT_RECORD_LOCATORS = (1..8).map { shortFileIdentifier ->
            GetProcessingOptionsCommand.RecordLocator(
                shortFileIdentifier = shortFileIdentifier,
                firstRecord = 1,
                lastRecord = MAX_RECORDS_PER_SFI,
            )
        }
        const val MAX_RECORDS_PER_SFI = 8

        const val PARAMETER_ERROR_SW1 = 0x6A.toByte()
        const val FILE_NOT_FOUND_SW2 = 0x82.toByte()
    }
}
