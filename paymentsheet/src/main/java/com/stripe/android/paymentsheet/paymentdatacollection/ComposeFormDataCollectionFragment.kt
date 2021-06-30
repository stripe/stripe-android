package com.stripe.android.paymentsheet.paymentdatacollection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import com.stripe.android.paymentsheet.StripeTheme
import com.stripe.android.paymentsheet.forms.Form
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.forms.TransformFormToPaymentMethod
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Fragment that displays a form for payment data collection based on the [SupportedPaymentMethod]
 * received in the arguments bundle.
 */
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

    /**
     * Provides to PaymentSheet a LiveData of the map to be used to create the payment method through
     * PaymentMethodCreateParams. If the form is currently invalid, the map is null.
     *
     * This must be lazy or we'll be reading from the ViewModel while the fragment is detached.
     */
    val paramMapLiveData by lazy {
        formViewModel.completeFormValues.map {
            it?.let {
                TransformFormToPaymentMethod().transform(formSpec.paramKey, it)
                    .filterOutNullValues()
                    .toMap()
            }
        }.distinctUntilChanged().asLiveData()
    }

    companion object {
        const val EXTRA_PAYMENT_METHOD = "com.stripe.android.paymentsheet.extra_payment_method"
    }
}

@Suppress("UNCHECKED_CAST")
private fun <K, V> Map<K, V?>.filterOutNullValues() = filterValues { it != null } as Map<K, V>
