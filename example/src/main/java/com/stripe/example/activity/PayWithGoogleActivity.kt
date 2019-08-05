package com.stripe.example.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.*
import com.stripe.android.ApiResultCallback
import com.stripe.android.GooglePayConfig
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.Token
import com.stripe.example.R

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
            paymentsClient.loadPaymentData(createGooglePayRequest(false)),
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

        // Get a Stripe Token object
        val stripeToken = Token.fromString(paymentData.paymentMethodToken?.token)
        if (stripeToken != null) {
            // Create a PaymentMethod object using the token id
            val billingDetails: PaymentMethod.BillingDetails?

            // Get the billing address to include in the Payment Method creation params
            val address = paymentData.cardInfo.billingAddress
            if (address != null) {
                billingDetails = PaymentMethod.BillingDetails.Builder()
                    .setAddress(Address.Builder()
                        .setLine1(address.address1)
                        .setLine2(address.address2)
                        .setCity(address.locality)
                        .setState(address.administrativeArea)
                        .setPostalCode(address.postalCode)
                        .setCountry(address.countryCode)
                        .build())
                    .setEmail(address.emailAddress)
                    .setName(address.name)
                    .setPhone(address.phoneNumber)
                    .build()
            } else {
                billingDetails = null
            }
            val paymentMethodCreateParams =
                PaymentMethodCreateParams.create(
                    PaymentMethodCreateParams.Card.create(
                        stripeToken.id),
                    billingDetails)

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
    }

    private fun createTokenizationParameters(): PaymentMethodTokenizationParameters {
        val params = GooglePayConfig().tokenizationSpecification
            .getJSONObject("parameters")
        return PaymentMethodTokenizationParameters.newBuilder()
            .setPaymentMethodTokenizationType(
                WalletConstants.PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY)
            .addParameter("gateway", "stripe")
            .addParameter("stripe:publishableKey",
                params.getString("stripe:publishableKey"))
            .addParameter("stripe:version",
                params.getString("stripe:version"))
            .build()
    }

    /**
     * @param isBillingAddressRequired see `billingAddressRequired` and `billingAddressParameters`
     *                                 at https://developers.google.com/pay/api/android/reference/object#CardParameters
     */
    private fun createGooglePayRequest(
        isBillingAddressRequired: Boolean = true
    ): PaymentDataRequest {
        val billingAddressFormat = if (isBillingAddressRequired) {
            WalletConstants.BILLING_ADDRESS_FORMAT_FULL
        } else {
            WalletConstants.BILLING_ADDRESS_FORMAT_MIN
        }

        return PaymentDataRequest.newBuilder()
            .setTransactionInfo(
                TransactionInfo.newBuilder()
                    .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                    .setTotalPrice("10.00")
                    .setCurrencyCode("USD")
                    .build())
            .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
            .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
            .setCardRequirements(
                CardRequirements.newBuilder()
                    .addAllowedCardNetworks(listOf(
                        WalletConstants.CARD_NETWORK_AMEX,
                        WalletConstants.CARD_NETWORK_DISCOVER,
                        WalletConstants.CARD_NETWORK_VISA,
                        WalletConstants.CARD_NETWORK_MASTERCARD
                    ))
                    .setBillingAddressRequired(isBillingAddressRequired)
                    .setBillingAddressFormat(billingAddressFormat)
                    .build())
            .setPaymentMethodTokenizationParameters(createTokenizationParameters())
            .build()
    }

    companion object {
        private const val LOAD_PAYMENT_DATA_REQUEST_CODE = 53
    }
}
