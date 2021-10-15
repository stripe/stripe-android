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
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.stripe.android.paymentsheet.StripeTheme
import com.stripe.android.paymentsheet.elements.Form
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod

/**
 * Fragment that displays a form for payment data collection based on the [SupportedPaymentMethod]
 * received in the arguments bundle.
 */
internal class ComposeFormDataCollectionFragment : Fragment() {
    private val formLayout by lazy {
        requireNotNull(
            requireArguments().getParcelable<FormFragmentArguments>(EXTRA_CONFIG)?.let {
                it.paymentMethod.formSpec
            }
        )
    }

    private val showCheckbox by lazy {
        val requireNotNull = requireNotNull(
            requireArguments().getParcelable<FormFragmentArguments>(EXTRA_CONFIG)?.let {
                it.showCheckbox
            }
        )
        requireNotNull
    }

    private val showCheckboxControlledFields by lazy {
        val requireNotNull = requireNotNull(
            requireArguments().getParcelable<FormFragmentArguments>(EXTRA_CONFIG)?.let {
                it.showCheckboxControlledFields
            }
        )
        requireNotNull
    }

    val paramKeySpec by lazy {
        requireNotNull(
            requireArguments().getParcelable<FormFragmentArguments>(EXTRA_CONFIG)?.let {
                it.paymentMethod.paramKey
            }
        )
    }

    val formViewModel: FormViewModel by viewModels {
        FormViewModel.Factory(
            resource = resources,
            layout = formLayout,
            config = requireNotNull(
                requireArguments().getParcelable(
                    EXTRA_CONFIG
                )
            )
        )
    }

    @ExperimentalUnitApi
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
        formViewModel.setEnabled(!processing)
    }

    internal companion object {
        const val EXTRA_CONFIG = "com.stripe.android.paymentsheet.extra_config"
    }
}
