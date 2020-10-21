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


class UPIPaymentActivity : AppCompatActivity() {
    private lateinit var stripe: Stripe
    private val viewBinding: UpiPaymentActivityBinding by lazy {
        UpiPaymentActivityBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewBinding.submit.setOnClickListener {
            val params = PaymentMethodCreateParams.create(
                upi = PaymentMethodCreateParams.Upi.Builder()
                    .setVpa(viewBinding.vpa.text.toString())
                    .build()
                , billingDetails = PaymentMethod.BillingDetails(
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

            stripe = Stripe(applicationContext, "pk_live_51H7wmsBte6TMTRd45LsWj9e3Kfkw6Kzlf5KhSrewsRydlElo8VarJxoIalKr5ielPeKf8erWZmt0qihjlwNux03y00c1zZpnxb")
//            stripe = Stripe(applicationContext, "pk_test_51H7wmsBte6TMTRd4gph9Wm7gnQOKJwdVTCj30AhtB8MhWtlYj6v9xDn1vdCtKYGAE7cybr6fQdbQQtgvzBihE9cl00tOnrTpL9")

            val confirmParams = ConfirmPaymentIntentParams
                .createWithPaymentMethodCreateParams(params, "pi_1HeJaYBte6TMTRd4WQjmjvar_secret_Gl9DjMwTNqkp3Ga8g9W8gno46")

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
                        val intent = Intent(applicationContext, UPIWaitingActivity::class.java)
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