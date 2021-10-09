package com.stripe.example.activity

import android.os.Bundle
import android.util.Log
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.example.databinding.IdealTestBinding

class IDealTestActivity : StripeIntentActivity() {
    private val viewBinding: IdealTestBinding by lazy {
        IdealTestBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        // create a PI with
// curl 'https://api.stripe.com/v1/payment_intents' -u sk_test_51IO8h3EAjaOkiuGMBjmLGlt7m78YDKc7vcKWhBeufW3gTirSj9hH9HfbISG3m3wUu7k8uVlYnATx14GdfsDgtVnp00f0lwIC5r: -d payment_method_types[]=ideal -d payment_method_data[type]=ideal -d confirm=true -d currency=eur -d amount=1099 -d return_url=http://stripe.com
        val idealConfirmParam = ConfirmPaymentIntentParams.create(
            clientSecret = "pi_3JiTg2EAjaOkiuGM193JH9cR_secret_DsU3JBOdlvwRYASefsaokQfVt"
        )
        viewBinding.idealButton.setOnClickListener {
            stripe.confirmPayment(this, idealConfirmParam)
        }

        viewModel.status.observe(this) {
            Log.d("IDEALTest", it)
        }
    }
}