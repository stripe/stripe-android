package com.stripe.android.link.account

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton

internal interface LinkStore {
    fun hasUsedLink(): Boolean
    fun markLinkAsUsed()
    fun clear()
}

@Singleton
internal class DefaultLinkStore @Inject constructor(
    context: Context,
) : LinkStore {

    private val sharedPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(FileName, Context.MODE_PRIVATE)
    }

    override fun hasUsedLink(): Boolean {
        return sharedPrefs.getBoolean(HasUsedLink, false)
    }

    override fun markLinkAsUsed() {
        sharedPrefs.edit { putBoolean(HasUsedLink, true) }
    }

    override fun clear() {
        sharedPrefs.edit { clear() }
    }

    internal companion object {
        const val FileName = "PaymentSheet_LinkStore"
        const val HasUsedLink = "has_used_link"
    }
}
