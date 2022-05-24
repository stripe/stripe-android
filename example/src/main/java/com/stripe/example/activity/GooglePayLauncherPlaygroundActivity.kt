package com.stripe.example.activity

import android.os.Bundle
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayLauncher
import com.stripe.android.googlepaylauncher.GooglePayLauncherContract
import com.stripe.example.databinding.GooglePayActivityPlaygroundBinding
import org.json.JSONObject

/**
 * This class provides the logic to play with different Google Pay configurations
 * to see how they behave.
 */
class GooglePayLauncherPlaygroundActivity : StripeIntentActivity() {
    private var clientSecret = ""
    private lateinit var config: GooglePayLauncher.Config

    private val viewBinding: GooglePayActivityPlaygroundBinding by lazy {
        GooglePayActivityPlaygroundBinding.inflate(layoutInflater)
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

        viewBinding.googlePayMerchantCountryCode.doAfterTextChanged {
            updateConfig()
        }
        viewBinding.googlePayMerchantName.doAfterTextChanged {
            updateConfig()
        }
        viewBinding.googlePayBilling.setOnCheckedChangeListener { _, _ -> updateConfig() }
        viewBinding.googlePayPhoneRequired.setOnCheckedChangeListener { _, _ -> updateConfig() }
        viewBinding.googlePayBillingRequired.setOnCheckedChangeListener { _, _ -> updateConfig() }
        viewBinding.googlePayEnvironment.setOnCheckedChangeListener { _, _ -> updateConfig() }
        viewBinding.googlePayExistingPm.setOnCheckedChangeListener { _, _ -> updateConfig() }
        viewBinding.googlePayAllowCreditCards.setOnCheckedChangeListener { _, _ -> updateConfig() }

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

        val googlePayLauncher = registerForActivityResult(GooglePayLauncherContract()) {
            onGooglePayResult(it)
        }

        viewBinding.googlePayButton.setOnClickListener {
            viewBinding.progressBar.isVisible = true
            googlePayLauncher.launch(
                GooglePayLauncherContract.PaymentIntentArgs(
                    clientSecret,
                    config
                )
            )
        }

        updateConfig()
        updateUi()
    }

    private fun updateConfig() {
        config = GooglePayLauncher.Config(
            environment = if (viewBinding.googlePayEnvironmentTest.isChecked) {
                GooglePayEnvironment.Test
            } else {
                GooglePayEnvironment.Production
            },
            merchantCountryCode = viewBinding.googlePayMerchantCountryCode.text.toString(),
            merchantName = viewBinding.googlePayMerchantName.text.toString(),
            billingAddressConfig = GooglePayLauncher.BillingAddressConfig(
                isRequired = viewBinding.googlePayBillingRequiredTrue.isChecked,
                format = if (viewBinding.googlePayBillingFull.isChecked) {
                    GooglePayLauncher.BillingAddressConfig.Format.Full
                } else {
                    GooglePayLauncher.BillingAddressConfig.Format.Min
                },
                isPhoneNumberRequired = viewBinding.googlePayPhoneRequiredTrue.isChecked
            ),
            existingPaymentMethodRequired = viewBinding.googlePayExistingPmTrue.isChecked,
            allowCreditCards = viewBinding.googlePayAllowCreditCardsTrue.isChecked
        )
    }

    private fun updateUi() {
        val isLoadingComplete = clientSecret.isNotBlank()
        viewBinding.progressBar.isInvisible = isLoadingComplete
        googlePayButton.isEnabled = isLoadingComplete
    }

    private fun onPaymentIntentCreated(json: JSONObject) {
        clientSecret = json.getString("secret")
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
