package com.stripe.android.cards

import android.content.Context
import com.stripe.android.model.CardMetadata
import com.stripe.android.model.parsers.AccountRangeJsonParser
import org.json.JSONObject

internal class DefaultCardAccountRangeStore(
    private val context: Context
) : CardAccountRangeStore {
    private val accountRangeJsonParser = AccountRangeJsonParser()

    private val prefs by lazy {
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
    }

    override suspend fun get(bin: String): List<CardMetadata.AccountRange> {
        return prefs.getStringSet(createPrefKey(bin), null)
            .orEmpty()
            .mapNotNull {
                accountRangeJsonParser.parse(JSONObject(it))
            }
    }

    override fun save(
        bin: String,
        accountRanges: List<CardMetadata.AccountRange>
    ) {
        val serializedAccountRanges = accountRanges.map {
            accountRangeJsonParser.serialize(it).toString()
        }.toSet()

        prefs.edit()
            .putStringSet(createPrefKey(bin), serializedAccountRanges)
            .apply()
    }

    private fun createPrefKey(bin: String): String = "$PREF_KEY_ACCOUNT_RANGES$bin"

    private companion object {
        private const val PREF_FILE = "InMemoryCardAccountRangeSource.Store"
        private const val PREF_KEY_ACCOUNT_RANGES = "key_account_ranges"
    }
}
