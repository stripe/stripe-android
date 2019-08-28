package com.stripe.android

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale

internal open class PaymentSessionPrefs private constructor(
    private val prefs: SharedPreferences?
) {
    constructor(context: Context) :
        this(context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE))

    open fun getSelectedPaymentMethodId(customerId: String): String? {
        return prefs?.getString(getPaymentMethodKey(customerId), null)
    }

    open fun saveSelectedPaymentMethodId(customerId: String, paymentMethodId: String) {
        prefs?.edit()?.putString(getPaymentMethodKey(customerId), paymentMethodId)
            ?.apply()
    }

    companion object {
        private const val PREF_FILE = "PaymentSessionPrefs"

        private fun getPaymentMethodKey(customerId: String): String {
            return String.format(Locale.US, "customer[%s].payment_method", customerId)
        }
    }
}
