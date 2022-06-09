package com.stripe.android.googlepaylauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.GooglePayResult
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.ShippingInformation
import org.json.JSONObject

/**
 * [StripeGooglePayActivity] is used to return the result of a Google Pay operation.
 * The activity will return payment data via [GooglePayLauncherResult.PaymentData].
 *
 * Use [StripeGooglePayContract] to start [StripeGooglePayActivity].
 *
 * See [Troubleshooting](https://developers.google.com/pay/api/android/support/troubleshooting)
 * for a guide to troubleshooting Google Pay issues.
 */
internal class StripeGooglePayActivity : AppCompatActivity() {
    private val paymentsClient: PaymentsClient by lazy {
        PaymentsClientFactory(this).create(args.config.environment)
    }
    private val publishableKey: String by lazy {
        PaymentConfiguration.getInstance(this).publishableKey
    }
    private val stripeAccountId: String? by lazy {
        PaymentConfiguration.getInstance(this).stripeAccountId
    }
    private val viewModel: StripeGooglePayViewModel by viewModels {
        StripeGooglePayViewModel.Factory(
            application,
            publishableKey,
            stripeAccountId,
            args
        )
    }

    private lateinit var args: StripeGooglePayContract.Args

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        overridePendingTransition(0, 0)
        setResult(
            RESULT_OK,
            Intent().putExtras(
                GooglePayLauncherResult.Canceled.toBundle()
            )
        )

        val nullableArgs = StripeGooglePayContract.Args.create(intent)
        if (nullableArgs == null) {
            finishWithResult(
                GooglePayLauncherResult.Error(
                    RuntimeException(
                        "StripeGooglePayActivity was started without arguments."
                    )
                )
            )
            return
        }
        args = nullableArgs

        args.statusBarColor?.let {
            window.statusBarColor = it
        }

        viewModel.googlePayResult.observe(this) { googlePayResult ->
            googlePayResult?.let(::finishWithResult)
        }

        if (!viewModel.hasLaunched) {
            viewModel.hasLaunched = true

            isReadyToPay(
                viewModel.createPaymentDataRequestForPaymentIntentArgs()
            )
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    /**
     * Check that Google Pay is available and ready
     */
    private fun isReadyToPay(paymentDataRequest: JSONObject) {
        paymentsClient.isReadyToPay(
            viewModel.createIsReadyToPayRequest()
        ).addOnCompleteListener { task ->
            runCatching {
                if (task.isSuccessful) {
                    payWithGoogle(paymentDataRequest)
                } else {
                    viewModel.updateGooglePayResult(
                        GooglePayLauncherResult.Unavailable
                    )
                }
            }.onFailure {
                viewModel.updateGooglePayResult(
                    GooglePayLauncherResult.Error(it)
                )
            }
        }
    }

    private fun payWithGoogle(paymentDataRequest: JSONObject) {
        AutoResolveHelper.resolveTask(
            paymentsClient.loadPaymentData(
                PaymentDataRequest.fromJson(paymentDataRequest.toString())
            ),
            this,
            LOAD_PAYMENT_DATA_REQUEST_CODE
        )
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOAD_PAYMENT_DATA_REQUEST_CODE) {
            when (resultCode) {
                RESULT_OK -> {
                    onGooglePayResult(data)
                }
                RESULT_CANCELED -> {
                    viewModel.updateGooglePayResult(
                        GooglePayLauncherResult.Canceled
                    )
                }
                AutoResolveHelper.RESULT_ERROR -> {
                    val status = AutoResolveHelper.getStatusFromIntent(data)
                    viewModel.updateGooglePayResult(
                        GooglePayLauncherResult.Error(
                            RuntimeException(
                                "Google Pay returned an error. See googlePayStatus property for more information."
                            ),
                            googlePayStatus = status
                        )
                    )
                }
                else -> {
                    viewModel.updateGooglePayResult(
                        GooglePayLauncherResult.Error(
                            RuntimeException(
                                "Google Pay returned an expected result code."
                            )
                        )
                    )
                }
            }
        }
    }

    private fun onGooglePayResult(data: Intent?) {
        val paymentData = data?.let { PaymentData.getFromIntent(it) }
        if (paymentData == null) {
            viewModel.updateGooglePayResult(
                GooglePayLauncherResult.Error(
                    IllegalArgumentException("Google Pay data was not available")
                )
            )
            return
        }

        val paymentDataJson = JSONObject(paymentData.toJson())
        val shippingInformation =
            GooglePayResult.fromJson(paymentDataJson).shippingInformation

        val params = PaymentMethodCreateParams.createFromGooglePay(paymentDataJson)
        viewModel.createPaymentMethod(params).observe(this) { result ->
            result?.fold(
                onSuccess = {
                    onPaymentMethodCreated(
                        it,
                        shippingInformation
                    )
                },
                onFailure = {
                    viewModel.paymentMethod = null
                    viewModel.updateGooglePayResult(
                        GooglePayLauncherResult.Error(it)
                    )
                }
            )
        }
    }

    private fun onPaymentMethodCreated(
        paymentMethod: PaymentMethod,
        shippingInformation: ShippingInformation?
    ) {
        viewModel.paymentMethod = paymentMethod

        viewModel.updateGooglePayResult(
            GooglePayLauncherResult.PaymentData(
                paymentMethod,
                shippingInformation
            )
        )
    }

    private fun finishWithResult(result: GooglePayLauncherResult) {
        setResult(
            RESULT_OK,
            Intent().putExtras(
                result.toBundle()
            )
        )
        finish()
    }

    private companion object {
        private const val LOAD_PAYMENT_DATA_REQUEST_CODE = 4444
    }
}
