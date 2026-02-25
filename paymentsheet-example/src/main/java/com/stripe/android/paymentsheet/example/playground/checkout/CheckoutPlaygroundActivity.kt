package com.stripe.android.paymentsheet.example.playground.checkout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.stripe.android.checkout.Checkout
import com.stripe.android.paymentelement.CheckoutSessionPreview

@CheckoutSessionPreview
class CheckoutPlaygroundActivity : AppCompatActivity() {
    companion object {
        const val CHECKOUT_STATE_KEY = "CHECKOUT_STATE_KEY"

        fun create(
            context: Context,
            checkoutState: Checkout.State,
        ): Intent {
            return Intent(context, CheckoutPlaygroundActivity::class.java).apply {
                putExtra(CHECKOUT_STATE_KEY, checkoutState)
            }
        }
    }

    private lateinit var checkout: Checkout

    @Suppress("DEPRECATION")
    private fun getCheckoutState(savedInstanceState: Bundle?): Checkout.State? {
        return savedInstanceState?.getParcelable<Checkout.State?>(CHECKOUT_STATE_KEY)
            ?: intent.getParcelableExtra<Checkout.State?>(CHECKOUT_STATE_KEY)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val checkoutState = getCheckoutState(savedInstanceState)
        if (checkoutState == null) {
            finish()
            return
        }

        checkout = Checkout.createWithState(checkoutState)

        setContent {
            CheckoutScreen()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(CHECKOUT_STATE_KEY, checkout.state)
    }

    override fun finish() {
        super.finish()

        setResult(RESULT_OK, Intent().putExtra(CHECKOUT_STATE_KEY, checkout.state))
    }
}

@Composable
fun CheckoutScreen() {
    Text("Added in a follow up.")
}
