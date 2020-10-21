package com.stripe.example.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.Stripe
import com.stripe.android.model.StripeIntent
import com.stripe.example.databinding.UpiWaitingActivityBinding

class UPIWaitingActivity : AppCompatActivity() {
    private lateinit var stripe: Stripe

    private val viewBinding: UpiWaitingActivityBinding by lazy {
        UpiWaitingActivityBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        stripe = Stripe(applicationContext, "pk_live_51H7wmsBte6TMTRd45LsWj9e3Kfkw6Kzlf5KhSrewsRydlElo8VarJxoIalKr5ielPeKf8erWZmt0qihjlwNux03y00c1zZpnxb")

        Thread(Runnable {
            var paymentIntent = stripe.retrievePaymentIntentSynchronous("pi_1HeJaYBte6TMTRd4WQjmjvar_secret_Gl9DjMwTNqkp3Ga8g9W8gno46")
            while(paymentIntent != null && paymentIntent.status == StripeIntent.Status.RequiresAction) {
                Thread.sleep(5000)
                paymentIntent = stripe.retrievePaymentIntentSynchronous("pi_1HeJaYBte6TMTRd4WQjmjvar_secret_Gl9DjMwTNqkp3Ga8g9W8gno46")
            }

            if (paymentIntent != null) {
                print(paymentIntent.status)
                viewBinding.paymentStatus.text = paymentIntent.status.toString()
            }
        }).start()

    }
}