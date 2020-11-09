package com.stripe.android.paymentsheet

import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.databinding.StripeActivityPaymentOptionsBinding

/**
 * An `Activity` for selecting a payment option.
 */
class PaymentOptionsActivity : AppCompatActivity() {
    @VisibleForTesting
    internal val viewBinding by lazy {
        StripeActivityPaymentOptionsBinding.inflate(layoutInflater)
    }

    private val viewModel: PaymentOptionsViewModel by viewModels()

    private val starterArgs: PaymentOptionsActivityStarter.Args? by lazy {
        PaymentOptionsActivityStarter.Args.fromIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val starterArgs = this.starterArgs
        if (starterArgs == null) {
            finish()
            return
        }

        setContentView(viewBinding.root)
    }
}
