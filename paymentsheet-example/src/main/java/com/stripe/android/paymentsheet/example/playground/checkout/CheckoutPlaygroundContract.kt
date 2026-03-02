package com.stripe.android.paymentsheet.example.playground.checkout

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.checkout.Checkout
import com.stripe.android.paymentelement.CheckoutSessionPreview

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutPlaygroundContract : ActivityResultContract<Checkout.State, Checkout.State?>() {
    override fun createIntent(context: Context, input: Checkout.State): Intent {
        return CheckoutPlaygroundActivity.create(context, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Checkout.State? {
        @Suppress("DEPRECATION")
        return intent?.getParcelableExtra(CheckoutPlaygroundActivity.CHECKOUT_STATE_KEY)
    }
}
