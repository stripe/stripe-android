package com.stripe.android.paymentsheet.paymentdatacollection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RestrictTo
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.stripe.android.paymentsheet.StripeTheme
import com.stripe.android.paymentsheet.forms.Form
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod

/**
 * Fragment that displays a form for payment data collection based on the [SupportedPaymentMethod]
 * received in the arguments bundle.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ComposeFormDataCollectionFragment : Fragment() {
    val formSpec by lazy {
        requireNotNull(
            requireArguments().getString(EXTRA_PAYMENT_METHOD)?.let {
                SupportedPaymentMethod.valueOf(it).formSpec
            }
        )
    }
    val formViewModel: FormViewModel by viewModels {
        FormViewModel.Factory(
            formSpec.layout,
            "Merchant Name, Inc." // TODO: Replace with argument.
        )
    }

    @ExperimentalAnimationApi
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(inflater.context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        setContent {
            StripeTheme {
                Column(Modifier.fillMaxSize()) {
                    Form(formViewModel)
                }
            }
        }
    }

    /**
     * Informs the fragment whether PaymentSheet is in a processing state, so the fragment knows it
     * should show its UI as enabled or disabled.
     */
    fun setProcessing(processing: Boolean) {
        // TODO: Enable or disable views accordingly
    }

    companion object {
        const val EXTRA_PAYMENT_METHOD = "com.stripe.android.paymentsheet.extra_payment_method"
    }
}
