package com.stripe.example.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import com.stripe.android.ApiResultCallback
import com.stripe.android.GooglePayConfig
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.example.R
import org.json.JSONArray
import org.json.JSONObject

class PayWithGoogleActivity : AppCompatActivity() {

    private lateinit var stripe: Stripe
    private lateinit var payWithGoogleButton: View
    private lateinit var paymentsClient: PaymentsClient
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay_with_google)

        stripe = Stripe(this, PaymentConfiguration.getInstance().publishableKey)

        paymentsClient = Wallet.getPaymentsClient(this,
            Wallet.WalletOptions.Builder()
                .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                .build())

        progressBar = findViewById(R.id.progress_bar)
        payWithGoogleButton = findViewById(R.id.btn_buy_pwg)
        payWithGoogleButton.isEnabled = false
        payWithGoogleButton.setOnClickListener { payWithGoogle() }

        isReadyToPay()
    }

    private fun payWithGoogle() {
        AutoResolveHelper.resolveTask(
            paymentsClient.loadPaymentData(createGooglePayRequest()),
            this@PayWithGoogleActivity,
            LOAD_PAYMENT_DATA_REQUEST_CODE
        )
    }

    private fun isReadyToPay() {
        progressBar.visibility = View.VISIBLE
        val request = IsReadyToPayRequest.newBuilder()
            .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
            .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
            .build()
        paymentsClient.isReadyToPay(request)
            .addOnCompleteListener { task ->
                progressBar.visibility = View.INVISIBLE

                try {
                    val result = task.getResult(ApiException::class.java)!!
                    if (result) {
                        Toast.makeText(this@PayWithGoogleActivity,
                            "Google Pay is ready",
                            Toast.LENGTH_SHORT).show()
                        payWithGoogleButton.isEnabled = true
                    } else {
                        Toast.makeText(this@PayWithGoogleActivity,
                            "Google Pay is unavailable",
                            Toast.LENGTH_SHORT).show()
                    }
                } catch (exception: ApiException) {
                    Toast.makeText(this@PayWithGoogleActivity,
                        "Exception: " + exception.localizedMessage,
                        Toast.LENGTH_LONG).show()
                }
            }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == LOAD_PAYMENT_DATA_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    if (data != null) {
                        handleGooglePayResult(data)
                    }
                }
                Activity.RESULT_CANCELED -> {
                    Toast.makeText(this@PayWithGoogleActivity,
                        "Canceled", Toast.LENGTH_LONG).show()
                }
                AutoResolveHelper.RESULT_ERROR -> {
                    val status = AutoResolveHelper.getStatusFromIntent(data)
                    val statusMessage = if (status != null) status.statusMessage else ""
                    Toast.makeText(this@PayWithGoogleActivity,
                        "Got error " + statusMessage!!,
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
        progressBar.visibility = View.VISIBLE

        val paymentData = PaymentData.getFromIntent(data) ?: return
        val paymentMethodCreateParams =
            PaymentMethodCreateParams.createFromGooglePay(JSONObject(paymentData.toJson()))

        stripe.createPaymentMethod(paymentMethodCreateParams,
            object : ApiResultCallback<PaymentMethod> {
                override fun onSuccess(result: PaymentMethod) {
                    progressBar.visibility = View.INVISIBLE
                    Toast.makeText(this@PayWithGoogleActivity,
                        "Created PaymentMethod ${result.id}", Toast.LENGTH_LONG)
                        .show()
                }

                override fun onError(e: Exception) {
                    progressBar.visibility = View.INVISIBLE
                    Toast.makeText(this@PayWithGoogleActivity,
                        "Exception: " + e.localizedMessage, Toast.LENGTH_LONG)
                        .show()
                }
            })
    }

    /**
     * @param isBillingAddressRequired see `billingAddressRequired` and `billingAddressParameters`
     *                                 at https://developers.google.com/pay/api/android/reference/object#CardParameters
     */
    private fun createGooglePayRequest(
        isBillingAddressRequired: Boolean = true,
        isPhoneNumberRequired: Boolean = true,
        isEmailRequired: Boolean = true
    ): PaymentDataRequest {
        val billingAddressFormat = if (isBillingAddressRequired) {
            "FULL"
        } else {
            "MIN"
        }

        val cardPaymentMethod = JSONObject()
            .put("type", "CARD")
            .put(
                "parameters",
                JSONObject()
                    .put("allowedAuthMethods", JSONArray()
                        .put("PAN_ONLY")
                        .put("CRYPTOGRAM_3DS"))
                    .put("allowedCardNetworks",
                        JSONArray()
                            .put("AMEX")
                            .put("DISCOVER")
                            .put("JCB")
                            .put("MASTERCARD")
                            .put("VISA"))
                    .put("billingAddressRequired", isBillingAddressRequired)
                    .put(
                        "billingAddressParameters",
                        JSONObject()
                            .put("format", billingAddressFormat)
                            .put("phoneNumberRequired", isPhoneNumberRequired)
                    )
            )
            .put("tokenizationSpecification", GooglePayConfig().tokenizationSpecification)

        val paymentDataRequest = JSONObject()
            .put("apiVersion", 2)
            .put("apiVersionMinor", 0)
            .put("allowedPaymentMethods", JSONArray().put(cardPaymentMethod))
            .put("transactionInfo", JSONObject()
                .put("totalPrice", "10.00")
                .put("totalPriceStatus", "FINAL")
                .put("currencyCode", "USD")
            )
            .put("merchantInfo", JSONObject()
                .put("merchantName", "Example Merchant"))
            .put("emailRequired", isEmailRequired)
            .toString()
        return PaymentDataRequest.fromJson(paymentDataRequest)
    }

    companion object {
        private const val LOAD_PAYMENT_DATA_REQUEST_CODE = 53
    }
}
