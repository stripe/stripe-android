package com.stripe.example.activity

import android.content.Context
import android.content.SharedPreferences
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.stripe.android.paymentsheet.PaymentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.example.R
import com.stripe.example.paymentsheet.PaymentSheetViewModel

internal abstract class BasePaymentSheetActivity : AppCompatActivity() {
    protected val viewModel: PaymentSheetViewModel by viewModels {
        PaymentSheetViewModel.Factory(
            application,
            getPreferences(Context.MODE_PRIVATE)
        )
    }

    private val prefsManager: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    protected val merchantName: String
        get() = prefsManager.getString("merchant_name", null) ?: "Widget Store"

    protected val isCustomerEnabled: Boolean
        get() = prefsManager.getBoolean("enable_customer", true)

    protected val billingAddressCollection: PaymentSheet.BillingAddressCollectionLevel
        get() {
            return when (prefsManager.getBoolean("require_billing_address", false)) {
                true -> PaymentSheet.BillingAddressCollectionLevel.Required
                false -> PaymentSheet.BillingAddressCollectionLevel.Automatic
            }
        }

    protected val googlePayConfig: PaymentSheet.GooglePayConfiguration?
        get() {
            return when (prefsManager.getBoolean("enable_googlepay", true)) {
                true -> {
                    PaymentSheet.GooglePayConfiguration(
                        environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                        countryCode = "US"
                    )
                }
                false -> null
            }
        }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.payment_sheet, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.config) {
            PaymentSheetConfigBottomSheet().show(
                supportFragmentManager,
                PaymentSheetConfigBottomSheet.TAG
            )
            return true
        } else if (item.itemId == R.id.refresh_key) {
            viewModel.clearKeys()
            onRefreshEphemeralKey()
        }
        return super.onOptionsItemSelected(item)
    }

    protected fun fetchEphemeralKey(
        onSuccess: (PaymentSheet.CustomerConfiguration) -> Unit = {}
    ) {
        viewModel.fetchEphemeralKey()
            .observe(this) { newEphemeralKey ->
                if (newEphemeralKey != null) {
                    onSuccess(
                        PaymentSheet.CustomerConfiguration(
                            id = newEphemeralKey.customer,
                            ephemeralKeySecret = newEphemeralKey.key
                        )
                    )
                }
            }
    }

    protected fun onPaymentSheetResult(
        paymentResult: PaymentResult
    ) {
        viewModel.status.value = paymentResult.toString()
    }

    protected fun onError(error: Throwable) {
        viewModel.status.postValue(
            """
            ${viewModel.status.value}
            
            Failed: ${error.message}
            """.trimIndent()
        )
    }

    abstract fun onRefreshEphemeralKey()
}
