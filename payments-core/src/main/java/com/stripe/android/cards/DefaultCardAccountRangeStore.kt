package com.stripe.android.cards

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.stripe.android.model.AccountRange
import com.stripe.android.model.parsers.AccountRangeJsonParser
import org.json.JSONObject

internal class DefaultCardAccountRangeStore(
    private val context: Context
) : CardAccountRangeStore {
    private val accountRangeJsonParser = AccountRangeJsonParser()

    private val prefs by lazy {
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
    }

    override suspend fun get(bin: Bin): List<AccountRange> {
        return prefs.getStringSet(createPrefKey(bin), null)
            .orEmpty()
            .mapNotNull {
                accountRangeJsonParser.parse(JSONObject(it))
            }
    }

    override fun save(
        bin: Bin,
        accountRanges: List<AccountRange>
    ) {
        val serializedAccountRanges = accountRanges.map {
            accountRangeJsonParser.serialize(it).toString()
        }.toSet()

        prefs.edit()
            .putStringSet(createPrefKey(bin), serializedAccountRanges)
            .apply()
    }

    override suspend fun contains(
        bin: Bin
    ): Boolean = prefs.contains(createPrefKey(bin))

    @VisibleForTesting
    internal fun createPrefKey(bin: Bin): String = "$PREF_KEY_ACCOUNT_RANGES:$bin"

    private companion object {
        private const val VERSION = 2
        private const val PREF_FILE = "InMemoryCardAccountRangeSource.Store.$VERSION"
        private const val PREF_KEY_ACCOUNT_RANGES = "key_account_ranges"
    }
}
