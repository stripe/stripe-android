package com.stripe.example.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayLauncher
import com.stripe.example.databinding.GooglePayActivityBinding

class PayWithGoogleActivity : AppCompatActivity() {

    // TODO(mshafrir-stripe): fetch client_secret from backend
    private val clientSecret = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewBinding = GooglePayActivityBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        val snackbarController = SnackbarController(viewBinding.coordinator)

        viewBinding.googlePayButton.isEnabled = false

        val googlePayLauncher = GooglePayLauncher(
            activity = this,
            config = GooglePayLauncher.Config(
                environment = GooglePayEnvironment.Test,
                merchantCountryCode = "JP",
                merchantName = "Widget Store",
                billingAddressConfig = GooglePayLauncher.BillingAddressConfig(
                    isRequired = true,
                    format = GooglePayLauncher.BillingAddressConfig.Format.Full,
                    isPhoneNumberRequired = true
                )
            ),
            readyCallback = { isReady ->
                snackbarController.show("Google Pay ready? $isReady")
                viewBinding.googlePayButton.isEnabled = isReady
            },
        ) { result ->
            snackbarController.show(result.toString())
        }

        viewBinding.googlePayButton.setOnClickListener {
            googlePayLauncher.presentForPaymentIntent(clientSecret)
        }
    }
}
