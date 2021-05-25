package com.stripe.android.paymentsheet

import android.content.Context
import android.content.SharedPreferences
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal class DefaultPrefsRepository(
    private val context: Context,
    private val customerId: String,
    private val isGooglePayReady: suspend () -> Boolean,
    private val workContext: CoroutineContext
) : PrefsRepository {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
    }

    override suspend fun getSavedSelection(): SavedSelection = withContext(workContext) {
        val prefData = prefs.getString(getKey(), null).orEmpty().split(":")
        val key = prefData.firstOrNull()
        when (key) {
            "google_pay" -> {
                SavedSelection.GooglePay.takeIf { isGooglePayReady() }
            }
            "payment_method" -> {
                prefData.getOrNull(1)?.let {
                    SavedSelection.PaymentMethod(id = it)
                }
            }
            else -> null
        } ?: SavedSelection.None
    }

    override fun savePaymentSelection(paymentSelection: PaymentSelection?) {
        when (paymentSelection) {
            PaymentSelection.GooglePay -> {
                "google_pay"
            }
            is PaymentSelection.Saved -> {
                "payment_method:${paymentSelection.paymentMethod.id.orEmpty()}"
            }
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
        return "customer[$customerId]"
    }

    private companion object {
        private const val PREF_FILE = "DefaultPrefsRepository"
    }
}
