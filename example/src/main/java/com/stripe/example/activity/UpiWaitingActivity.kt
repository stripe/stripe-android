package com.stripe.example.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.Stripe
import com.stripe.android.model.StripeIntent
import com.stripe.example.databinding.UpiWaitingActivityBinding

class UpiWaitingActivity : AppCompatActivity() {
    private lateinit var stripe: Stripe

    private val viewBinding: UpiWaitingActivityBinding by lazy {
        UpiWaitingActivityBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        stripe = Stripe(applicationContext, "pk_live_51H7wmsBte6TMTRd45LsWj9e3Kfkw6Kzlf5KhSrewsRydlElo8VarJxoIalKr5ielPeKf8erWZmt0qihjlwNux03y00c1zZpnxb")

        Thread(Runnable {
            val paymentIntentClientSecret = "pi_1HekzlBte6TMTRd4ndk9lqQv_secret_gYrLPEgcRO3PMg7E1wEC1CJRC"
            var paymentIntent = stripe.retrievePaymentIntentSynchronous(paymentIntentClientSecret)
            while(paymentIntent != null && paymentIntent.status == StripeIntent.Status.RequiresAction) {
                Thread.sleep(5000)
                paymentIntent = stripe.retrievePaymentIntentSynchronous(paymentIntentClientSecret)
            }

            if (paymentIntent != null) {
                print(paymentIntent.status)
                viewBinding.paymentStatus.text = paymentIntent.status.toString()
            }
        }).start()

    }
}