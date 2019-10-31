package com.stripe.android

import android.content.Context

internal interface PaymentSessionPrefs {
    fun getSelectedPaymentMethodId(customerId: String): String?
    fun saveSelectedPaymentMethodId(customerId: String, paymentMethodId: String?)

    companion object {
        private const val PREF_FILE = "PaymentSessionPrefs"

        private fun getPaymentMethodKey(customerId: String): String {
            return "customer[$customerId].payment_method"
        }

        @JvmSynthetic
        internal fun create(context: Context): PaymentSessionPrefs {
            val prefs = context.getSharedPreferences(
                PREF_FILE, Context.MODE_PRIVATE
            )
            return object : PaymentSessionPrefs {
                override fun getSelectedPaymentMethodId(customerId: String): String? {
                    return prefs?.getString(getPaymentMethodKey(customerId), null)
                }

                override fun saveSelectedPaymentMethodId(
                    customerId: String,
                    paymentMethodId: String?
                ) {
                    prefs?.edit()?.putString(getPaymentMethodKey(customerId), paymentMethodId)
                        ?.apply()
                }
            }
        }
    }
}
