package com.stripe.android.ui.core.elements

import android.content.res.Resources
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class BankRepository @Inject constructor(
    val resources: Resources?
) {
    private val bankItemMap = mutableMapOf<SupportedBankType, List<DropdownItemSpec>?>()

    private val format = Json { ignoreUnknownKeys = true }

    fun get(bankType: SupportedBankType) = requireNotNull(bankItemMap[bankType])

    init {
        initialize(
            SupportedBankType.values().associateWith { bank ->
                resources?.assets?.open(bank.assetFileName)
            }
        )
    }

    @VisibleForTesting
    fun initialize(supportedBankTypeInputStreamMap: Map<SupportedBankType, InputStream?>) {
        supportedBankTypeInputStreamMap.forEach { (bankType, banksOfType) ->
            bankItemMap[bankType] = parseBank(banksOfType)
        }
    }

    private fun parseBank(inputStream: InputStream?) =
        getJsonStringFromInputStream(inputStream)?.let {
            format.decodeFromString<List<DropdownItemSpec>>(
                it
            )
        }

    private fun getJsonStringFromInputStream(inputStream: InputStream?) =
        inputStream?.bufferedReader().use { it?.readText() }
}
