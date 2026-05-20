package com.stripe.example.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent
import com.stripe.example.StripeFactory
import com.stripe.example.activity.UpiPaymentActivity.Companion.EXTRA_CLIENT_SECRET
import com.stripe.example.databinding.UpiWaitingActivityBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val paymentIntent = stripe.retrievePaymentIntentSynchronous(clientSecret)
                if (paymentIntent?.status == StripeIntent.Status.Succeeded) {
                    onSuccess(paymentIntent)
                    cancel()
                } else {
                    delay(5000)
                }
            }
        }
    }

    private suspend fun onSuccess(
        paymentIntent: PaymentIntent
    ) = withContext(Dispatchers.Main) {
        viewBinding.paymentStatus.text = paymentIntent.status.toString()
    }
}
