package com.stripe.example.activity

import android.content.SharedPreferences
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.example.R
import com.stripe.example.paymentsheet.PaymentSheetViewModel

internal abstract class BasePaymentSheetActivity : AppCompatActivity() {
    protected val viewModel: PaymentSheetViewModel by viewModels {
        PaymentSheetViewModel.Factory(
            application
        )
    }

    private val prefsManager: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    protected val merchantName: String
        get() = prefsManager.getString("merchant_name", null) ?: "Widget Store"

    protected val isCustomerEnabled: Boolean
        get() = prefsManager.getBoolean("enable_customer", true)

    protected val isReturningCustomer: Boolean
        get() = prefsManager.getBoolean("returning_customer", true)

    protected val isSetupIntent: Boolean
        get() = prefsManager.getBoolean("setup_intent", false)

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

    protected val customer: String
        get() = if (isCustomerEnabled && isReturningCustomer) {
            "returning"
        } else if (isCustomerEnabled) {
            temporaryCustomerId ?: "new"
        } else {
            "new"
        }

    protected val mode: String
        get() = if (isSetupIntent) "setup" else "payment"

    protected var temporaryCustomerId: String? = null

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
        }

        return super.onOptionsItemSelected(item)
    }

    protected fun prepareCheckout(
        onSuccess: (PaymentSheet.CustomerConfiguration, String) -> Unit
    ) {
        viewModel.prepareCheckout(customer, mode)
            .observe(this) { checkoutResponse ->
                if (checkoutResponse != null) {
                    temporaryCustomerId = if (isCustomerEnabled && !isReturningCustomer) {
                        checkoutResponse.customerId
                    } else {
                        null
                    }

                    // Re-initing here because the ExampleApplication inits with the key from
                    // gradle properties
                    PaymentConfiguration.init(this, checkoutResponse.publishableKey)

                    onSuccess(
                        PaymentSheet.CustomerConfiguration(
                            id = checkoutResponse.customerId,
                            ephemeralKeySecret = checkoutResponse.customerEphemeralKeySecret
                        ),
                        checkoutResponse.intentClientSecret
                    )
                }
            }
    }

    protected open fun onPaymentSheetResult(
        paymentResult: PaymentSheetResult
    ) {
        viewModel.status.value = paymentResult.toString()
    }
}
