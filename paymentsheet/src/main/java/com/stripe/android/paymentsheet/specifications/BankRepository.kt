package com.stripe.android.paymentsheet.specifications

import android.content.res.Resources
import androidx.annotation.VisibleForTesting
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.InputStream

internal data class BankRepository(
    val resources: Resources
) {
    private val bankItemMap = mutableMapOf<SupportedBankType, List<DropdownItem>?>()

    internal fun get(bankType: SupportedBankType) = requireNotNull(bankItemMap[bankType])

    fun init() {
        init(
            SupportedBankType.values().associateWith { bank ->
                val assets = resources.assets
                assets.open(bank.assetFileName)
            }
        )
    }

    @VisibleForTesting
    internal fun init(supportedBankTypeInputStreamMap: Map<SupportedBankType, InputStream>) {
        supportedBankTypeInputStreamMap.forEach { (bankType, banksOfType) ->
            bankItemMap[bankType] = parseBank(banksOfType)
        }
    }

    private val format = Json { ignoreUnknownKeys = true }

    private fun parseBank(inputStream: InputStream?) =
        getJsonStringFromInputStream(inputStream)?.let {
            format.decodeFromString<List<DropdownItem>>(
                it
            )
        }

    private fun getJsonStringFromInputStream(inputStream: InputStream?) =
        inputStream?.bufferedReader().use { it?.readText() }
}
