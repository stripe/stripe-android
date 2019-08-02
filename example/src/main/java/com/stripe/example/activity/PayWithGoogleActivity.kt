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
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.Token
import com.stripe.example.R

class PayWithGoogleActivity : AppCompatActivity() {

    private var mPayWithGoogleButton: View? = null
    private var mPaymentsClient: PaymentsClient? = null
    private var mProgressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay_with_google)
        mPaymentsClient = Wallet.getPaymentsClient(this,
            Wallet.WalletOptions.Builder()
                .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                .build())

        mProgressBar = findViewById(R.id.pwg_progress_bar)
        mPayWithGoogleButton = findViewById(R.id.btn_buy_pwg)
        mPayWithGoogleButton!!.isEnabled = false
        mPayWithGoogleButton!!.setOnClickListener { payWithGoogle() }

        isReadyToPay()
    }

    private fun payWithGoogle() {
        AutoResolveHelper.resolveTask(
            mPaymentsClient!!.loadPaymentData(createPaymentDataRequest()),
            this@PayWithGoogleActivity,
            LOAD_PAYMENT_DATA_REQUEST_CODE)
    }

    private fun isReadyToPay() {
        mProgressBar!!.visibility = View.VISIBLE
        val request = IsReadyToPayRequest.newBuilder()
            .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
            .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
            .build()
        mPaymentsClient!!.isReadyToPay(request)
            .addOnCompleteListener { task ->
                try {
                    val result = task.getResult(ApiException::class.java)!!
                    mProgressBar!!.visibility = View.INVISIBLE
                    if (result) {
                        Toast.makeText(this@PayWithGoogleActivity, "Ready",
                            Toast.LENGTH_SHORT).show()
                        mPayWithGoogleButton!!.isEnabled = true
                    } else {
                        Toast.makeText(this@PayWithGoogleActivity, "No PWG",
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
        val paymentData = PaymentData.getFromIntent(data) ?: return

        // You can get some data on the user's card, such as the brand and last 4 digits
        val info = paymentData.cardInfo
        // You can also pull the user address from the PaymentData object.
        val address = paymentData.shippingAddress
        val paymentMethodToken = paymentData.paymentMethodToken
        // This is the raw string version of your Stripe token.
        val rawToken = paymentMethodToken?.token

        val stripeToken = Token.fromString(rawToken)
        if (stripeToken != null) {
            // Create a PaymentMethod object using the token id
            val billingDetails: PaymentMethod.BillingDetails?
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
            val paymentMethodCreateParams = PaymentMethodCreateParams.create(
                PaymentMethodCreateParams.Card.create(
                    stripeToken.id),
                billingDetails)

            // Now create PaymentMethod using
            // Stripe#createPaymentMethod(paymentMethodCreateParams, callback)

            Toast.makeText(this@PayWithGoogleActivity,
                "Got token $stripeToken", Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun createTokenizationParameters(): PaymentMethodTokenizationParameters {
        return PaymentMethodTokenizationParameters.newBuilder()
            .setPaymentMethodTokenizationType(
                WalletConstants.PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY)
            .addParameter("gateway", "stripe")
            .addParameter("stripe:publishableKey",
                PaymentConfiguration.getInstance().publishableKey)
            .addParameter("stripe:version", "2018-11-08")
            .build()
    }

    private fun createPaymentDataRequest(): PaymentDataRequest {
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
                    .build())
            .setPaymentMethodTokenizationParameters(createTokenizationParameters())
            .build()
    }

    companion object {
        private const val LOAD_PAYMENT_DATA_REQUEST_CODE = 53
    }
}
