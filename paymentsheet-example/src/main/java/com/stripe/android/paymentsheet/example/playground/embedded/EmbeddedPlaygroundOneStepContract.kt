@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.embedded

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.checkout.Checkout
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import kotlinx.parcelize.Parcelize

internal class EmbeddedPlaygroundOneStepContract :
    ActivityResultContract<EmbeddedPlaygroundOneStepContract.Args, Boolean>() {
    override fun createIntent(context: Context, input: Args): Intent {
        return EmbeddedPlaygroundActivity.create(
            context = context,
            playgroundState = input.playgroundState,
            checkoutState = input.checkoutState,
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?) = resultCode == Activity.RESULT_OK

    @Parcelize
    class Args(
        val playgroundState: PlaygroundState.Payment,
        val checkoutState: Checkout.State?,
    ) : Parcelable
}
