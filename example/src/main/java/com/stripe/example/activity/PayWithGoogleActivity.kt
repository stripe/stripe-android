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

    /**
     * Check that Google Pay is available and ready
     */
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

    /**
     * Launch the Google Pay sheet
     */
    private fun payWithGoogle() {
        AutoResolveHelper.resolveTask(
            paymentsClient.loadPaymentData(createGooglePayRequest()),
            this@PayWithGoogleActivity,
            LOAD_PAYMENT_DATA_REQUEST_CODE
        )
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
        val paymentData = PaymentData.getFromIntent(data) ?: return
        val paymentMethodCreateParams =
            PaymentMethodCreateParams.createFromGooglePay(JSONObject(paymentData.toJson()))

        stripe.createPaymentMethod(paymentMethodCreateParams,
            object : ApiResultCallback<PaymentMethod> {
                override fun onSuccess(result: PaymentMethod) {
                }

                override fun onError(e: Exception) {
                }
            })
    }

    /**
     * @param isBillingAddressRequired Set to `true` if you require a billing address.
     *                                 A billing address should only be requested if it's required to process the transaction.
     *                                 Additional data requests can increase friction in the checkout process and lead to a lower conversion rate.
     *
     *                                 If `true`, `billingAddressParameters` will be set to `FULL`.
     *
     *                                 See [CardParameters#billingAddressRequired](https://developers.google.com/pay/api/android/reference/object#CardParameters) and
     *                                 [CardParameters#billingAddressParameters](https://developers.google.com/pay/api/android/reference/object#CardParameters)
     *
     * @param isPhoneNumberRequired Set to `true` if a phone number is required to process the transaction.
     *                              See [BillingAddressParameters.phoneNumberRequired](https://developers.google.com/pay/api/android/reference/object#BillingAddressParameters)
     *
     * @param isEmailRequired Set to `true` to request an email address.
     *                        See [PaymentDataRequest#emailRequired](https://developers.google.com/pay/api/android/reference/object#PaymentDataRequest)
     */
    private fun createGooglePayRequest(
        isBillingAddressRequired: Boolean = true,
        isPhoneNumberRequired: Boolean = true,
        isEmailRequired: Boolean = true
    ): PaymentDataRequest {
        /**
         * Billing address format required to complete the transaction.
         *
         * `MIN`: Name, country code, and postal code (default).
         * `FULL`: Name, street address, locality, region, country code, and postal code.
         */
        val billingAddressFormat = if (isBillingAddressRequired) {
            "FULL"
        } else {
            "MIN"
        }

        val billingAddressParams = JSONObject()
            .put("phoneNumberRequired", isPhoneNumberRequired)
            .put("format", billingAddressFormat)

        val cardPaymentMethodParams = JSONObject()
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
            .put("billingAddressParameters", billingAddressParams)

        val cardPaymentMethod = JSONObject()
            .put("type", "CARD")
            .put(
                "parameters", cardPaymentMethodParams
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
