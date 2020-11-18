package com.stripe.android.paymentsheet

import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import com.stripe.android.databinding.StripeActivityPaymentOptionsBinding
import com.stripe.android.paymentsheet.ui.BasePaymentSheetActivity

/**
 * An `Activity` for selecting a payment option.
 */
internal class PaymentOptionsActivity : BasePaymentSheetActivity() {
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

    override fun onUserCancel() {
        // TODO(mshafrir-stripe): implement
    }
}
