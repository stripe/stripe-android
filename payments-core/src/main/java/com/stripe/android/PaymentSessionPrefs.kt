package com.stripe.android

import android.content.Context
import android.content.SharedPreferences

internal interface PaymentSessionPrefs {
    fun getPaymentMethod(customerId: String?): SelectedPaymentMethod?
    fun savePaymentMethod(customerId: String, paymentMethod: SelectedPaymentMethod?)

    class Default(context: Context) : PaymentSessionPrefs {
        val prefs: SharedPreferences by lazy {
            context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
        }

        override fun getPaymentMethod(customerId: String?): SelectedPaymentMethod? {
            return SelectedPaymentMethod.fromString(
                customerId?.let {
                    prefs.getString(getPaymentMethodKey(it), null)
                }
            )
        }

        override fun savePaymentMethod(
            customerId: String,
            paymentMethod: SelectedPaymentMethod?
        ) {
            prefs.edit()
                .putString(getPaymentMethodKey(customerId), paymentMethod?.stringValue)
                .apply()
        }
    }

    companion object {
        private const val PREF_FILE = "PaymentSessionPrefs"
        const val GOOGLE_PAY = "GooglePay"

        private fun getPaymentMethodKey(customerId: String?): String {
            return "customer[$customerId].payment_method"
        }
    }

    sealed class SelectedPaymentMethod(val stringValue: String) {
        class Saved(paymentMethodId: String) : SelectedPaymentMethod(paymentMethodId)
        data object GooglePay : SelectedPaymentMethod(GOOGLE_PAY)

        companion object {
            fun fromString(value: String?) = when (value) {
                GOOGLE_PAY -> GooglePay
                is String -> Saved(value)
                else -> null
            }
        }
    }
}
