package com.stripe.example.activity

import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.stripe.android.paymentsheet.PaymentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.example.R
import com.stripe.example.databinding.ActivityPaymentSheetCompleteBinding
import com.stripe.example.paymentsheet.PaymentSheetViewModel

class LaunchPaymentSheetCompleteActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityPaymentSheetCompleteBinding.inflate(layoutInflater)
    }

    private val viewModel: PaymentSheetViewModel by viewModels {
        PaymentSheetViewModel.Factory(
            application,
            getPreferences(MODE_PRIVATE)
        )
    }

    private val prefsManager: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        val paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)

        viewModel.inProgress.observe(this) {
            viewBinding.progressBar.visibility = if (it) View.VISIBLE else View.INVISIBLE
            viewBinding.launch.isEnabled = !it
        }
        viewModel.status.observe(this) {
            viewBinding.status.text = it
        }

        viewBinding.launch.setOnClickListener {
            val isCustomerEnabled = prefsManager.getBoolean("enable_customer", true)

            if (isCustomerEnabled) {
                fetchEphemeralKey { customerConfig ->
                    createPaymentIntent(paymentSheet, customerConfig)
                }
            } else {
                createPaymentIntent(paymentSheet, null)
            }
        }
    }

    private fun createPaymentIntent(
        paymentSheet: PaymentSheet,
        customerConfig: PaymentSheet.CustomerConfiguration?
    ) {
        viewModel.createPaymentIntent(
            "us",
            customerId = customerConfig?.id
        ).observe(this) {
            it.fold(
                onSuccess = { json ->
                    val clientSecret = json.getString("secret")

                    onPaymentIntent(
                        paymentSheet,
                        clientSecret,
                        customerConfig
                    )
                },
                onFailure = {
                    viewModel.status.postValue(viewModel.status.value + "\nFailed: ${it.message}")
                }
            )
        }
    }

    private fun onPaymentIntent(
        paymentSheet: PaymentSheet,
        paymentIntentClientSecret: String,
        customerConfig: PaymentSheet.CustomerConfiguration?
    ) {
        viewModel.inProgress.postValue(false)

        val isGooglePayEnabled = prefsManager.getBoolean("enable_googlepay", true)
        val merchantName = prefsManager.getString("merchant_name", null) ?: "Widget Store"
        val billingAddressCollection = when (prefsManager.getBoolean("require_billing_address", false)) {
            true -> PaymentSheet.BillingAddressCollectionLevel.Required
            false -> PaymentSheet.BillingAddressCollectionLevel.Automatic
        }

        paymentSheet.present(
            paymentIntentClientSecret,
            PaymentSheet.Configuration(
                merchantDisplayName = merchantName,
                customer = customerConfig,
                googlePay = when (isGooglePayEnabled) {
                    true -> {
                        PaymentSheet.GooglePayConfiguration(
                            environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                            countryCode = "US"
                        )
                    }
                    false -> null
                },

                billingAddressCollection = billingAddressCollection
            )
        )
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
            fetchEphemeralKey()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onPaymentSheetResult(
        paymentResult: PaymentResult
    ) {
        val statusString = when (paymentResult) {
            is PaymentResult.Cancelled -> {
                "MC Completed with status: Cancelled"
            }
            is PaymentResult.Failed -> {
                "MC Completed with status: Failed(${paymentResult.error.message}"
            }
            is PaymentResult.Succeeded -> {
                "MC Completed with status: Succeeded"
            }
        }
        viewModel.status.value = viewModel.status.value + "\n\n$statusString"
    }

    private fun fetchEphemeralKey(
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
}
