package com.stripe.android.paymentsheet.elements

import android.content.res.Resources
import androidx.annotation.VisibleForTesting
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal data class BankRepository @Inject internal constructor(
    val resources: Resources?
) {
    // This is needed for @Preview and inject does not support a constructor with default parameters.
    internal constructor() : this(null)

    private val bankItemMap = mutableMapOf<SupportedBankType, List<DropdownItemSpec>?>()

    internal fun get(bankType: SupportedBankType) = requireNotNull(bankItemMap[bankType])

    init {
        initialize(
            SupportedBankType.values().associateWith { bank ->
                resources?.assets?.open(bank.assetFileName)
            }
        )
    }

    @VisibleForTesting
    internal fun initialize(supportedBankTypeInputStreamMap: Map<SupportedBankType, InputStream?>) {
        supportedBankTypeInputStreamMap.forEach { (bankType, banksOfType) ->
            bankItemMap[bankType] = parseBank(banksOfType)
        }
    }

    private val format = Json { ignoreUnknownKeys = true }

    private fun parseBank(inputStream: InputStream?) =
        getJsonStringFromInputStream(inputStream)?.let {
            format.decodeFromString<List<DropdownItemSpec>>(
                it
            )
        }

    private fun getJsonStringFromInputStream(inputStream: InputStream?) =
        inputStream?.bufferedReader().use { it?.readText() }
}
