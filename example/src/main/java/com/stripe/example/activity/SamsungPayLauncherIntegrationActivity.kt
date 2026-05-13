package com.stripe.example.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.stripe.android.payments.samsungpay.Config
import com.stripe.android.payments.samsungpay.SamsungPayLauncher
import com.stripe.android.payments.samsungpay.SamsungPayResult
import com.stripe.example.databinding.SamsungPayActivityBinding

class SamsungPayLauncherIntegrationActivity : AppCompatActivity() {
    private var isSamsungPayReady = false

    private val viewBinding: SamsungPayActivityBinding by lazy {
        SamsungPayActivityBinding.inflate(layoutInflater)
    }

    private val snackbarController: SnackbarController by lazy {
        SnackbarController(viewBinding.coordinator)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        val samsungPayLauncher = SamsungPayLauncher(
            activity = this,
            config = Config(amount = 1205L),
            readyCallback = ::onSamsungPayReady,
            resultCallback = ::onSamsungPayResult
        )

        viewBinding.samsungPayButton.setOnClickListener {
            viewBinding.progressBar.isVisible = true
            samsungPayLauncher.presentForPaymentIntent("pi_placeholder_client_secret")
        }

        updateUi()
    }

    private fun updateUi() {
        viewBinding.progressBar.isInvisible = isSamsungPayReady
        viewBinding.samsungPayButton.isEnabled = isSamsungPayReady
    }

    private fun onSamsungPayReady(isReady: Boolean) {
        snackbarController.show("Samsung Pay ready? $isReady")
        isSamsungPayReady = isReady
        updateUi()
    }

    private fun onSamsungPayResult(result: SamsungPayResult) {
        viewBinding.progressBar.isInvisible = true

        when (result) {
            is SamsungPayResult.Success -> {
                "Successfully collected payment."
            }
            is SamsungPayResult.Cancel -> {
                "Customer cancelled Samsung Pay."
            }
            is SamsungPayResult.Failure -> {
                "Samsung Pay failed. ${result.error.message}"
            }
        }.let {
            snackbarController.show(it)
            viewBinding.samsungPayButton.isEnabled = false
        }
    }
}
