package com.stripe.android

import android.content.Context
import android.content.SharedPreferences

internal interface PaymentSessionPrefs {
    fun getPaymentMethodId(customerId: String?): String?
    fun savePaymentMethodId(customerId: String, paymentMethodId: String?)

    class Default(context: Context) : PaymentSessionPrefs {
        val prefs: SharedPreferences by lazy {
            context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
        }

        override fun getPaymentMethodId(customerId: String?): String? {
            return customerId?.let {
                prefs.getString(getPaymentMethodKey(it), null)
            }
        }

        override fun savePaymentMethodId(
            customerId: String,
            paymentMethodId: String?
        ) {
            prefs.edit()
                .putString(getPaymentMethodKey(customerId), paymentMethodId)
                .apply()
        }
    }

    companion object {
        private const val PREF_FILE = "PaymentSessionPrefs"

        private fun getPaymentMethodKey(customerId: String?): String {
            return "customer[$customerId].payment_method"
        }
    }
}
