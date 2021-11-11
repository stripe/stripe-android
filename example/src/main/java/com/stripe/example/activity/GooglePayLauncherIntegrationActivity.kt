package com.stripe.example.activity

import android.os.Bundle
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayLauncher
import com.stripe.example.databinding.GooglePayActivityBinding
import org.json.JSONObject

class GooglePayLauncherIntegrationActivity : StripeIntentActivity() {
    private var clientSecret = ""
    private var isGooglePayReady = false

    private val viewBinding: GooglePayActivityBinding by lazy {
        GooglePayActivityBinding.inflate(layoutInflater)
    }

    private val googlePayButton: GooglePayButton by lazy {
        viewBinding.googlePayButton
    }

    private val snackbarController: SnackbarController by lazy {
        SnackbarController(viewBinding.coordinator)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewBinding.progressBar.isVisible = true
        viewBinding.googlePayButton.isEnabled = false

        viewModel.createPaymentIntent(COUNTRY_CODE)
            .observe(this) { result ->
                result.fold(
                    onSuccess = ::onPaymentIntentCreated,
                    onFailure = { error ->
                        snackbarController.show(
                            "Could not create PaymentIntent. ${error.message}"
                        )
                    }
                )
            }

        val googlePayLauncher = GooglePayLauncher(
            activity = this,
            config = GooglePayLauncher.Config(
                environment = GooglePayEnvironment.Test,
                merchantCountryCode = COUNTRY_CODE,
                merchantName = "Widget Store",
                billingAddressConfig = GooglePayLauncher.BillingAddressConfig(
                    isRequired = true,
                    format = GooglePayLauncher.BillingAddressConfig.Format.Full,
                    isPhoneNumberRequired = true
                ),
                existingPaymentMethodRequired = true
            ),
            readyCallback = ::onGooglePayReady,
            resultCallback = ::onGooglePayResult
        )

        viewBinding.googlePayButton.setOnClickListener {
            viewBinding.progressBar.isVisible = true
            googlePayLauncher.presentForPaymentIntent(clientSecret)
        }
    }

    private fun updateUi() {
        val isLoadingComplete = isGooglePayReady && clientSecret.isNotBlank()
        viewBinding.progressBar.isInvisible = isLoadingComplete
        googlePayButton.isEnabled = isLoadingComplete
    }

    private fun onPaymentIntentCreated(json: JSONObject) {
        clientSecret = json.getString("secret")
        updateUi()
    }

    private fun onGooglePayReady(isReady: Boolean) {
        snackbarController.show("Google Pay ready? $isReady")
        isGooglePayReady = isReady
        updateUi()
    }

    private fun onGooglePayResult(result: GooglePayLauncher.Result) {
        viewBinding.progressBar.isInvisible = true

        when (result) {
            GooglePayLauncher.Result.Completed -> {
                "Successfully collected payment."
            }
            GooglePayLauncher.Result.Canceled -> {
                "Customer cancelled Google Pay."
            }
            is GooglePayLauncher.Result.Failed -> {
                "Google Pay failed. ${result.error.message}"
            }
        }.let {
            snackbarController.show(it)
        }
    }

    private companion object {
        private const val COUNTRY_CODE = "US"
    }
}
