package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.payments.PaymentFlowResult

/**
 * An `Activity` that relays the intent extras that it received as a result and finishes.
 */
internal class PaymentRelayActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val paymentFlowResult = PaymentFlowResult.Unvalidated.fromIntent(intent)

        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtras(paymentFlowResult.toBundle())
        )
    }

    override fun onResume() {
        super.onResume()
        finish()
    }
}
