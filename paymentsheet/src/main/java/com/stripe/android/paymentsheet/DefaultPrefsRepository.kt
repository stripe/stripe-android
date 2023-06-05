package com.stripe.android.paymentsheet

import android.content.Context
import android.content.SharedPreferences
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.toSavedSelection
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal class DefaultPrefsRepository(
    private val context: Context,
    private val customerId: String?,
    private val workContext: CoroutineContext
) : PrefsRepository {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
    }

    override suspend fun getSavedSelection(
        isGooglePayAvailable: Boolean,
        isLinkAvailable: Boolean
    ) = withContext(workContext) {
        val prefData = prefs.getString(getKey(), null).orEmpty().split(":")
        when (prefData.firstOrNull()) {
            "google_pay" -> SavedSelection.GooglePay.takeIf { isGooglePayAvailable }
            "link" -> SavedSelection.Link.takeIf { isLinkAvailable }
            "payment_method" -> prefData.getOrNull(1)?.let {
                SavedSelection.PaymentMethod(id = it)
            }
            else -> null
        } ?: SavedSelection.None
    }

    override fun savePaymentSelection(paymentSelection: PaymentSelection?) {
        setSavedSelection(paymentSelection?.toSavedSelection())
    }

    override fun setSavedSelection(savedSelection: SavedSelection?) {
        when (savedSelection) {
            SavedSelection.GooglePay -> "google_pay"
            SavedSelection.Link -> "link"
            is SavedSelection.PaymentMethod -> "payment_method:${savedSelection.id}"
            else -> null
        }?.let { value ->
            write(value)
        }
    }

    private fun write(value: String) {
        prefs.edit()
            .putString(getKey(), value)
            .apply()
    }

    private fun getKey(): String {
        return customerId?.let { "customer[$it]" } ?: "guest"
    }

    private companion object {
        private const val PREF_FILE = "DefaultPrefsRepository"
    }
}
