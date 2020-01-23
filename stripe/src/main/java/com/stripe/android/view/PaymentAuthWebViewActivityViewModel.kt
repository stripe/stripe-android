package com.stripe.android.view

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.PaymentAuthWebViewStarter
import com.stripe.android.PaymentController
import com.stripe.android.StripeIntentResult
import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization

internal class PaymentAuthWebViewActivityViewModel(
    private val args: PaymentAuthWebViewStarter.Args
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

    internal val paymentResult: PaymentController.Result
        @JvmSynthetic
        get() {
            return PaymentController.Result(
                clientSecret = args.clientSecret,
                sourceId = Uri.parse(args.url).lastPathSegment.orEmpty()
            )
        }

    @JvmSynthetic
    internal fun cancelIntentSource(): LiveData<Intent> {
        val resultData = MutableLiveData<Intent>()
        resultData.value = Intent().putExtras(
            paymentResult.copy(
                flowOutcome = StripeIntentResult.Outcome.CANCELED,
                shouldCancelSource = true
            ).toBundle()
        )
        return resultData
    }

    internal data class ToolbarTitleData(
        internal val text: String,
        internal val toolbarCustomization: StripeToolbarCustomization
    )

    internal class Factory(
        private val args: PaymentAuthWebViewStarter.Args
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return PaymentAuthWebViewActivityViewModel(args) as T
        }
    }
}
