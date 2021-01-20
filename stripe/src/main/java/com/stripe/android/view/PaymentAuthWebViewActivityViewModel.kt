package com.stripe.android.view

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.StripeIntentResult
import com.stripe.android.auth.PaymentAuthWebViewContract
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization

internal class PaymentAuthWebViewActivityViewModel(
    private val args: PaymentAuthWebViewContract.Args
) : ViewModel() {
    @JvmSynthetic
    internal val buttonText = args.toolbarCustomization?.let { toolbarCustomization ->
        toolbarCustomization.buttonText.takeUnless { it.isNullOrBlank() }
    }

    @JvmSynthetic
    internal val toolbarTitle = args.toolbarCustomization?.let { toolbarCustomization ->
        toolbarCustomization.headerText.takeUnless { it.isNullOrBlank() }?.let {
            ToolbarTitleData(it, toolbarCustomization)
        }
    }

    @JvmSynthetic
    internal val toolbarBackgroundColor = args.toolbarCustomization?.backgroundColor

    internal val paymentResult: PaymentFlowResult.Unvalidated
        @JvmSynthetic
        get() {
            return PaymentFlowResult.Unvalidated(
                clientSecret = args.clientSecret,
                sourceId = Uri.parse(args.url).lastPathSegment.orEmpty(),
                stripeAccountId = args.stripeAccountId
            )
        }

    internal val cancellationResult: Intent
        @JvmSynthetic
        get() {
            return Intent().putExtras(
                paymentResult.copy(
                    flowOutcome = if (args.shouldCancelIntentOnUserNavigation) {
                        StripeIntentResult.Outcome.CANCELED
                    } else {
                        StripeIntentResult.Outcome.SUCCEEDED
                    },
                    shouldCancelSource = args.shouldCancelSource
                ).toBundle()
            )
        }

    internal data class ToolbarTitleData(
        internal val text: String,
        internal val toolbarCustomization: StripeToolbarCustomization
    )

    internal class Factory(
        private val args: PaymentAuthWebViewContract.Args
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return PaymentAuthWebViewActivityViewModel(args) as T
        }
    }
}
