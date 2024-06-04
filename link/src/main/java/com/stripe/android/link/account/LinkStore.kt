package com.stripe.android.link.account

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.RestrictTo
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LinkStore @Inject constructor(
    context: Context,
) {

    private val sharedPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(FileName, Context.MODE_PRIVATE)
    }

    fun hasUsedLink(): Boolean {
        return sharedPrefs.getBoolean(HasUsedLink, false)
    }

    fun markLinkAsUsed() {
        sharedPrefs.edit { putBoolean(HasUsedLink, true) }
    }

    fun clear() {
        sharedPrefs.edit { clear() }
    }

    internal companion object {
        const val FileName = "PaymentSheet_LinkStore"
        const val HasUsedLink = "has_used_link"
    }
}
