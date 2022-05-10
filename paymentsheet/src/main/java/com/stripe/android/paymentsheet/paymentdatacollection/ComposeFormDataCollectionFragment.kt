package com.stripe.android.paymentsheet.paymentdatacollection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentOptionsActivity
import com.stripe.android.paymentsheet.PaymentOptionsViewModel
import com.stripe.android.paymentsheet.PaymentSheetActivity
import com.stripe.android.paymentsheet.PaymentSheetViewModel
import com.stripe.android.paymentsheet.forms.Form
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import com.stripe.android.ui.core.FieldValuesToParamsMapConverter
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.elements.IdentifierSpec
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch

/**
 * Fragment that displays a form for payment data collection based on the [SupportedPaymentMethod]
 * received in the arguments bundle.
 */
@OptIn(FlowPreview::class)
internal class ComposeFormDataCollectionFragment : Fragment() {
    private val formLayout by lazy {
        requireNotNull(
            requireArguments().getParcelable<FormFragmentArguments>(EXTRA_CONFIG)
                ?.paymentMethod?.formSpec
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
            ),
            contextSupplier = { requireContext() }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(inflater.context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        setPadding(0, 18, 0, 0)

        setContent {
            PaymentsTheme {
                Column(Modifier.fillMaxSize()) {
                    Form(formViewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sheetViewModel by when (activity) {
            is PaymentOptionsActivity -> {
                activityViewModels<PaymentOptionsViewModel>()
            }
            is PaymentSheetActivity -> {
                activityViewModels<PaymentSheetViewModel>()
            }
            else -> {
                return
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // The block passed to repeatOnLifecycle is executed when the lifecycle
            // is at least STARTED and is cancelled when the lifecycle is STOPPED.
            // It automatically restarts the block when the lifecycle is STARTED again.
            formViewModel.completeFormValues
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collect { formFieldValues ->
                    // if the formFieldValues is a change either null or new values for the
                    // newLpm then we should clear it out --- but what happens if we cancel -- selection should
                    // have the correct value
                    sheetViewModel.updateSelection(
                        transformToPaymentSelection(
                            formFieldValues,
                            sheetViewModel.getAddFragmentSelectedLpmValue()
                        )
                    )
                }
        }

        /**
         * Informs the fragment whether PaymentSheet is in a processing state, so the fragment knows it
         * should show its UI as enabled or disabled.
         */
        sheetViewModel.processing.observe(viewLifecycleOwner) { processing ->
            formViewModel.setEnabled(!processing)
        }
    }

    @VisibleForTesting
    internal fun transformToPaymentSelection(
        formFieldValues: FormFieldValues?,
        selectedPaymentMethodResources: SupportedPaymentMethod,
    ) = formFieldValues?.let {
        FieldValuesToParamsMapConverter.transformToPaymentMethodCreateParams(
            formFieldValues.fieldValuePairs
                .filterNot { entry ->
                    entry.key == IdentifierSpec.SaveForFutureUse ||
                        entry.key == IdentifierSpec.CardBrand
                },
            selectedPaymentMethodResources.type
        ).run {
            if (selectedPaymentMethodResources.type == PaymentMethod.Type.Card) {
                PaymentSelection.New.Card(
                    paymentMethodCreateParams = this,
                    brand = CardBrand.fromCode(
                        formFieldValues.fieldValuePairs[IdentifierSpec.CardBrand]?.value
                    ),
                    customerRequestedSave = formFieldValues.userRequestedReuse

                )
            } else {
                PaymentSelection.New.GenericPaymentMethod(
                    getString(selectedPaymentMethodResources.displayNameResource),
                    selectedPaymentMethodResources.iconResource,
                    this,
                    customerRequestedSave = formFieldValues.userRequestedReuse
                )
            }
        }
    }

    internal companion object {
        const val EXTRA_CONFIG = "com.stripe.android.paymentsheet.extra_config"
    }
}
