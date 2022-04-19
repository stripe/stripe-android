package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.databinding.FragmentPaymentsheetAddPaymentMethodBinding
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import com.stripe.android.paymentsheet.paymentdatacollection.ComposeFormDataCollectionFragment
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.paymentsheet.paymentdatacollection.TransformToPaymentMethodCreateParams
import com.stripe.android.paymentsheet.ui.AnimationConstants
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.elements.IdentifierSpec
import kotlinx.coroutines.launch

internal abstract class BaseAddPaymentMethodFragment : Fragment() {
    abstract val viewModelFactory: ViewModelProvider.Factory
    abstract val sheetViewModel: BaseSheetViewModel<*>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val themedInflater = inflater.cloneInContext(
            ContextThemeWrapper(requireActivity(), R.style.StripePaymentSheetAddPaymentMethodTheme)
        )
        return themedInflater.inflate(
            R.layout.fragment_paymentsheet_add_payment_method,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewBinding = FragmentPaymentsheetAddPaymentMethodBinding.bind(view)

        val paymentMethods = sheetViewModel.supportedPaymentMethods

        sheetViewModel.headerText.value =
            getString(R.string.stripe_paymentsheet_add_payment_method_title)

        val selectedPaymentMethodIndex = paymentMethods.indexOf(
            sheetViewModel.getAddFragmentSelectedLpm().value
        ).takeUnless { it == -1 } ?: 0

        if (paymentMethods.size > 1) {
            setupRecyclerView(
                viewBinding,
                paymentMethods,
                sheetViewModel.getAddFragmentSelectedLpmValue()
            )
        }

        if (paymentMethods.isNotEmpty()) {
            // If the activity is destroyed and recreated, then the fragment is already present
            // and doesn't need to be replaced, only the selected payment method needs to be set
            if (savedInstanceState == null) {
                replacePaymentMethodFragment(paymentMethods[selectedPaymentMethodIndex])
            }
        }

        sheetViewModel.processing.observe(viewLifecycleOwner) { isProcessing ->
            (getFragment() as? ComposeFormDataCollectionFragment)?.setProcessing(isProcessing)
        }

        // If the activity was destroyed and recreated then we need to re-attach the fragment,
        // as attach will not be called again.
        childFragmentManager.fragments.forEach { fragment ->
            attachComposeFragmentViewModel(fragment)
        }

        childFragmentManager.addFragmentOnAttachListener { _, fragment ->
            attachComposeFragmentViewModel(fragment)
        }

        sheetViewModel.eventReporter.onShowNewPaymentOptionForm()
    }

    private fun attachComposeFragmentViewModel(fragment: Fragment) {
        (fragment as? ComposeFormDataCollectionFragment)?.let { formFragment ->
            // Need to access the formViewModel so it is constructed.
            val formViewModel = formFragment.formViewModel
            viewLifecycleOwner.lifecycleScope.launch {
                formViewModel.completeFormValues.collect { formFieldValues ->
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
        }
    }

    private fun setupRecyclerView(
        viewBinding: FragmentPaymentsheetAddPaymentMethodBinding,
        paymentMethods: List<SupportedPaymentMethod>,
        initialSelectedItem: SupportedPaymentMethod
    ) {
        viewBinding.paymentMethodsRecycler.isVisible = true
        viewBinding.paymentMethodsRecycler.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val processing by sheetViewModel.processing
                    .asFlow()
                    .collectAsState(initial = false)
                val selectedItem by sheetViewModel.getAddFragmentSelectedLpm()
                    .asFlow()
                    .collectAsState(initial = initialSelectedItem)
                PaymentMethodsUI(
                    selectedIndex = paymentMethods.indexOf(selectedItem),
                    isEnabled = !processing,
                    paymentMethods = paymentMethods,
                    onItemSelectedListener = { selectedLpm ->
                        onPaymentMethodSelected(selectedLpm)
                    }
                )
            }
        }
    }

    @VisibleForTesting
    internal fun onPaymentMethodSelected(paymentMethod: SupportedPaymentMethod) {
        // hide the soft keyboard.
        ViewCompat.getWindowInsetsController(requireView())
            ?.hide(WindowInsetsCompat.Type.ime())

        replacePaymentMethodFragment(paymentMethod)
    }

    private fun replacePaymentMethodFragment(paymentMethod: SupportedPaymentMethod) {
        sheetViewModel.setAddFragmentSelectedLPM(paymentMethod)

        val args = requireArguments()
        args.putParcelable(
            ComposeFormDataCollectionFragment.EXTRA_CONFIG,
            getFormArguments(
                stripeIntent = requireNotNull(sheetViewModel.stripeIntent.value),
                config = sheetViewModel.config,
                showPaymentMethod = paymentMethod,
                merchantName = sheetViewModel.merchantName,
                amount = sheetViewModel.amount.value,
                injectorKey = sheetViewModel.injectorKey,
                newLpm = sheetViewModel.newLpm
            )
        )

        childFragmentManager.commit {
            setCustomAnimations(
                AnimationConstants.FADE_IN,
                AnimationConstants.FADE_OUT,
                AnimationConstants.FADE_IN,
                AnimationConstants.FADE_OUT
            )
            replace(
                R.id.payment_method_fragment_container,
                ComposeFormDataCollectionFragment::class.java,
                args
            )
        }
    }

    private fun getFragment() =
        childFragmentManager.findFragmentById(R.id.payment_method_fragment_container)

    companion object {
        private val transformToPaymentMethodCreateParams = TransformToPaymentMethodCreateParams()

        @VisibleForTesting
        internal fun transformToPaymentSelection(
            formFieldValues: FormFieldValues?,
            selectedPaymentMethodResources: SupportedPaymentMethod,
        ) = formFieldValues?.let {
            transformToPaymentMethodCreateParams.transform(
                formFieldValues.copy(
                    fieldValuePairs = it.fieldValuePairs
                        .filterNot { entry ->
                            entry.key == IdentifierSpec.SaveForFutureUse ||
                                entry.key == IdentifierSpec.CardBrand
                        }
                ),
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
                        selectedPaymentMethodResources.displayNameResource,
                        selectedPaymentMethodResources.iconResource,
                        this,
                        customerRequestedSave = formFieldValues.userRequestedReuse
                    )
                }
            }
        }

        @VisibleForTesting
        internal fun getFormArguments(
            showPaymentMethod: SupportedPaymentMethod,
            stripeIntent: StripeIntent,
            config: PaymentSheet.Configuration?,
            merchantName: String,
            amount: Amount? = null,
            @InjectorKey injectorKey: String,
            newLpm: PaymentSelection.New?
        ): FormFragmentArguments {

            val layoutFormDescriptor = showPaymentMethod.getPMAddForm(stripeIntent, config)

            return FormFragmentArguments(
                paymentMethod = showPaymentMethod,
                showCheckbox = layoutFormDescriptor.showCheckbox,
                showCheckboxControlledFields = newLpm?.let {
                    newLpm.customerRequestedSave == PaymentSelection.CustomerRequestedSave.RequestReuse
                } ?: layoutFormDescriptor.showCheckboxControlledFields,
                merchantName = merchantName,
                amount = amount,
                billingDetails = config?.defaultBillingDetails,
                injectorKey = injectorKey,
                initialPaymentMethodCreateParams = if (newLpm?.paymentMethodCreateParams?.typeCode == showPaymentMethod.type.code) {
                    when (newLpm) {
                        is PaymentSelection.New.GenericPaymentMethod -> {
                            newLpm.paymentMethodCreateParams
                        }
                        is PaymentSelection.New.Card -> {
                            newLpm.paymentMethodCreateParams
                        }
                    }
                } else {
                    null
                }
            )
        }
    }
}
