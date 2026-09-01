package com.stripe.android.paymentsheet.example.playground.checkout

import android.content.Context

internal class CheckoutControllerExampleCustomerStore(
    context: Context,
) {
    private val sharedPreferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun getCustomerId(): String? {
        return sharedPreferences.getString(CUSTOMER_ID_KEY, null)
    }

    fun saveCustomerId(customerId: String) {
        sharedPreferences.edit().putString(CUSTOMER_ID_KEY, customerId).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "CheckoutControllerExample"
        const val CUSTOMER_ID_KEY = "customer_id"
    }
}
