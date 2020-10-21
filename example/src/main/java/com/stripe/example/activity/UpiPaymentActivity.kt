package com.stripe.example.activity;

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentIntentResult
import com.stripe.android.Stripe
import com.stripe.android.model.Address
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent
import com.stripe.example.databinding.UpiPaymentActivityBinding


class UpiPaymentActivity : AppCompatActivity() {
    private val stripe by lazy {
        Stripe(applicationContext, "pk_live_51H7wmsBte6TMTRd45LsWj9e3Kfkw6Kzlf5KhSrewsRydlElo8VarJxoIalKr5ielPeKf8erWZmt0qihjlwNux03y00c1zZpnxb")
    }
    private val viewBinding: UpiPaymentActivityBinding by lazy {
        UpiPaymentActivityBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewBinding.submit.setOnClickListener {
            val params = PaymentMethodCreateParams.create(
                upi = PaymentMethodCreateParams.Upi(
                    vpa = viewBinding.vpa.text.toString()
                ), billingDetails = PaymentMethod.BillingDetails(
                    name = "Anirudh Bhargava",
                    phone = "8960464240",
                    email = "anirudhb08@gmail.com",
                    address = Address.Builder()
                        .setCity("Jaipur")
                        .setCountry("IN")
                        .setLine1("182/93")
                        .setPostalCode("302033")
                        .setState("RJ")
                        .build()
                )
            )

            val confirmParams = ConfirmPaymentIntentParams
                .createWithPaymentMethodCreateParams(params, "pi_1HekzlBte6TMTRd4ndk9lqQv_secret_gYrLPEgcRO3PMg7E1wEC1CJRC")

            stripe.confirmPayment(this, confirmParams)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Handle the result of stripe.confirmPayment
        stripe.onPaymentResult(requestCode, data, object : ApiResultCallback<PaymentIntentResult> {
            override fun onSuccess(result: PaymentIntentResult) {
                val paymentIntent = result.intent
                val status = paymentIntent.status
                if (status == StripeIntent.Status.Succeeded) {
                    print("Payment Successful")
                } else if (status == StripeIntent.Status.RequiresAction) {
                    if (!(paymentIntent.clientSecret.isNullOrBlank())) {
                        val intent = Intent(applicationContext, UpiWaitingActivity::class.java)
                        startActivity(intent)
                    }
                }
            }

            override fun onError(e: Exception) {
                print("Payment failed")
            }
        })
    }


}