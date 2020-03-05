package com.stripe.example.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import com.stripe.android.ApiResultCallback
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.Stripe
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.example.StripeFactory
import com.stripe.example.databinding.GooglePayActivityBinding
import org.json.JSONObject

class PayWithGoogleActivity : AppCompatActivity() {
    private val viewBinding: GooglePayActivityBinding by lazy {
        GooglePayActivityBinding.inflate(layoutInflater)
    }

    private val stripe: Stripe by lazy {
        StripeFactory(this).create()
    }

    private val paymentsClient: PaymentsClient by lazy {
        Wallet.getPaymentsClient(this,
            Wallet.WalletOptions.Builder()
                .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                .build())
    }
    private val googlePayJsonFactory: GooglePayJsonFactory by lazy {
        GooglePayJsonFactory(this)
    }
    private val snackbarController: SnackbarController by lazy {
        SnackbarController(viewBinding.coordinator)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewBinding.googlePayButton.isEnabled = false
        viewBinding.googlePayButton.setOnClickListener {
            payWithGoogle()
        }

        isReadyToPay()
    }

    /**
     * Check that Google Pay is available and ready
     */
    private fun isReadyToPay() {
        viewBinding.progressBar.visibility = View.VISIBLE
        val request = IsReadyToPayRequest.fromJson(
            googlePayJsonFactory.createIsReadyToPayRequest().toString()
        )

        paymentsClient.isReadyToPay(request)
            .addOnCompleteListener { task ->
                viewBinding.progressBar.visibility = View.INVISIBLE

                try {
                    if (task.isSuccessful) {
                        showSnackbar("Google Pay is ready")
                        viewBinding.googlePayButton.isEnabled = true
                    } else {
                        showSnackbar("Google Pay is unavailable")
                    }
                } catch (exception: ApiException) {
                    Log.e("StripeExample", "Exception in isReadyToPay", exception)
                    showSnackbar("Exception: ${exception.localizedMessage}")
                }
            }
    }

    /**
     * Launch the Google Pay sheet
     */
    private fun payWithGoogle() {
        AutoResolveHelper.resolveTask(
            paymentsClient.loadPaymentData(
                PaymentDataRequest.fromJson(
                    googlePayJsonFactory.createPaymentDataRequest(
                        transactionInfo = GooglePayJsonFactory.TransactionInfo(
                            currencyCode = "USD",
                            totalPrice = 10000,
                            totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Final
                        ),
                        merchantInfo = GooglePayJsonFactory.MerchantInfo(
                            merchantName = "Widget Store"
                        ),
                        shippingAddressParameters = GooglePayJsonFactory.ShippingAddressParameters(
                            isRequired = true,
                            allowedCountryCodes = setOf("US", "DE"),
                            phoneNumberRequired = true
                        ),
                        billingAddressParameters = GooglePayJsonFactory.BillingAddressParameters(
                            isRequired = true,
                            format = GooglePayJsonFactory.BillingAddressParameters.Format.Full,
                            isPhoneNumberRequired = true
                        )
                    ).toString()
                )
            ),
            this@PayWithGoogleActivity,
            LOAD_PAYMENT_DATA_REQUEST_CODE
        )
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOAD_PAYMENT_DATA_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    if (data != null) {
                        handleGooglePayResult(data)
                    }
                }
                Activity.RESULT_CANCELED -> {
                    showSnackbar("Canceled")
                }
                AutoResolveHelper.RESULT_ERROR -> {
                    val status = AutoResolveHelper.getStatusFromIntent(data)
                    val statusMessage = status?.statusMessage ?: "unknown"
                    Toast.makeText(this@PayWithGoogleActivity,
                        "Got error: $statusMessage",
                        Toast.LENGTH_SHORT).show()
                }

                // Log the status for debugging
                // Generally there is no need to show an error to
                // the user as the Google Payment API will do that
                else -> {
                }
            }
        }
    }

    private fun handleGooglePayResult(data: Intent) {
        val paymentData = PaymentData.getFromIntent(data) ?: return
        val paymentDataJson = JSONObject(paymentData.toJson())

        viewBinding.googlePayResult.text = paymentDataJson.toString(2)

        val paymentMethodCreateParams =
            PaymentMethodCreateParams.createFromGooglePay(paymentDataJson)

        stripe.createPaymentMethod(
            paymentMethodCreateParams,
            callback = object : ApiResultCallback<PaymentMethod> {
                override fun onSuccess(result: PaymentMethod) {
                    showSnackbar("Created PaymentMethod ${result.id}")
                }

                override fun onError(e: Exception) {
                    Log.e("StripeExample", "Exception while creating PaymentMethod", e)
                    showSnackbar("Exception while creating PaymentMethod")
                }
            }
        )
    }

    private fun showSnackbar(message: String) {
        snackbarController.show(message)
    }

    private companion object {
        private const val LOAD_PAYMENT_DATA_REQUEST_CODE = 5000
    }
}
