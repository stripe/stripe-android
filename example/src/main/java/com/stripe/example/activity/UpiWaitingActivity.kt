package com.stripe.example.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.model.StripeIntent
import com.stripe.example.StripeFactory
import com.stripe.example.activity.UpiPaymentActivity.Companion.EXTRA_CLIENT_SECRET
import com.stripe.example.databinding.UpiWaitingActivityBinding

class UpiWaitingActivity : AppCompatActivity() {
    private val stripe by lazy { StripeFactory(application).create() }

    private val viewBinding: UpiWaitingActivityBinding by lazy {
        UpiWaitingActivityBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        val clientSecret = intent.extras?.getString(EXTRA_CLIENT_SECRET)
        if (clientSecret == null) {
            finish()
            return
        }

        Thread(
            Runnable {
                var paymentIntent = stripe.retrievePaymentIntentSynchronous(clientSecret)
                while (paymentIntent != null && paymentIntent.status == StripeIntent.Status.RequiresAction) {
                    Thread.sleep(5000)
                    paymentIntent = stripe.retrievePaymentIntentSynchronous(clientSecret)
                }

                if (paymentIntent != null) {
                    print(paymentIntent.status)
                    viewBinding.paymentStatus.text = paymentIntent.status.toString()
                }
            }
        ).start()
    }
}
