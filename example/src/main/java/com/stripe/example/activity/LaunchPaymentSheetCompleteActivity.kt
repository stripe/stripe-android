package com.stripe.example.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.paymentsheet.PaymentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.example.databinding.ActivityPaymentSheetCompleteBinding
import com.stripe.example.paymentsheet.EphemeralKey
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

    private lateinit var ephemeralKey: EphemeralKey

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewModel.inProgress.observe(this) {
            viewBinding.progressBar.visibility = if (it) View.VISIBLE else View.INVISIBLE
            viewBinding.launch.isEnabled = !it
            viewBinding.clear.isEnabled = !it
        }
        viewModel.status.observe(this) {
            viewBinding.status.text = it
        }

        viewBinding.clear.setOnClickListener {
            viewModel.clearKeys()
            fetchEphemeralKey()
        }
        viewBinding.launch.setOnClickListener {
            viewModel.createPaymentIntent("us", ephemeralKey.customer).observe(this) {
                it.fold(
                    onSuccess = { json ->
                        viewModel.inProgress.postValue(false)
                        val secret = json.getString("secret")
                        val checkout = PaymentSheet(
                            secret,
                            PaymentSheet.Configuration(
                                merchantDisplayName = "Widget Store",
                                customer = PaymentSheet.CustomerConfiguration(
                                    id = ephemeralKey.customer,
                                    ephemeralKeySecret = ephemeralKey.key
                                ),
                                googlePay = PaymentSheet.GooglePayConfiguration(
                                    environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                                    countryCode = "US"
                                ),
                                billingAddressCollection = PaymentSheet.BillingAddressCollectionLevel.Automatic
                            )
                        )
                        checkout.present(this)
                    },
                    onFailure = {
                        viewModel.status.postValue(viewModel.status.value + "\nFailed: ${it.message}")
                    }
                )
            }
        }

        fetchEphemeralKey()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val paymentSheetResult = PaymentSheet.Result.fromIntent(data)
        if (paymentSheetResult != null) {
            val statusString = when (val status = paymentSheetResult.status) {
                is PaymentResult.Cancelled -> {
                    "MC Completed with status: Cancelled"
                }
                is PaymentResult.Failed -> {
                    "MC Completed with status: Failed(${status.error.message}"
                }
                is PaymentResult.Succeeded -> {
                    "MC Completed with status: Succeeded"
                }
            }
            viewModel.status.value = viewModel.status.value + "\n\n$statusString"
        }
    }

    private fun fetchEphemeralKey() {
        viewModel.fetchEphemeralKey()
            .observe(this) { newEphemeralKey ->
                if (newEphemeralKey != null) {
                    ephemeralKey = newEphemeralKey
                }
            }
    }
}
