package com.stripe.example.activity

import android.os.Bundle
import android.view.View
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.example.databinding.ActivityPaymentSheetCompleteBinding

internal class LaunchPaymentSheetCompleteActivity : BasePaymentSheetActivity() {
    private val viewBinding by lazy {
        ActivityPaymentSheetCompleteBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewModel.inProgress.observe(this) {
            viewBinding.progressBar.visibility = if (it) View.VISIBLE else View.INVISIBLE
            viewBinding.launch.isEnabled = !it
        }
        viewModel.status.observe(this) {
            viewBinding.status.text = it
        }

        viewBinding.launch.setOnClickListener {
            if (isCustomerEnabled) {
                fetchEphemeralKey { customerConfig ->
                    createPaymentIntent(
                        customerConfig
                    )
                }
            } else {
                createPaymentIntent(
                    null
                )
            }
        }

        fetchEphemeralKey()
    }

    private fun createPaymentIntent(
        customerConfig: PaymentSheet.CustomerConfiguration?
    ) {
        viewModel.createPaymentIntent(
            COUNTRY_CODE,
            customerId = customerConfig?.id
        ).observe(this) { result ->
            result.fold(
                onSuccess = { json ->
                    val clientSecret = json.getString("secret")

                    onPaymentIntent(
                        clientSecret,
                        customerConfig
                    )
                },
                onFailure = ::onError
            )
        }
    }

    private fun onPaymentIntent(
        paymentIntentClientSecret: String,
        customerConfig: PaymentSheet.CustomerConfiguration?
    ) {
        viewModel.inProgress.postValue(false)

        // show PaymentSheet
    }

    override fun onRefreshEphemeralKey() {
        fetchEphemeralKey()
    }

    private companion object {
        private const val COUNTRY_CODE = "us"
    }
}
