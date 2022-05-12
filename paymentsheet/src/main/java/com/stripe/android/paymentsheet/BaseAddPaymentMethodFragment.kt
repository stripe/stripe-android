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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.databinding.FragmentPaymentsheetAddPaymentMethodBinding
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import com.stripe.android.paymentsheet.paymentdatacollection.ComposeFormDataCollectionFragment
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormFragment
import com.stripe.android.paymentsheet.ui.AnimationConstants
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.Amount
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

internal abstract class BaseAddPaymentMethodFragment : Fragment() {
    abstract val viewModelFactory: ViewModelProvider.Factory
    abstract val sheetViewModel: BaseSheetViewModel<*>

    private lateinit var viewBinding: FragmentPaymentsheetAddPaymentMethodBinding
    private var showLinkInlineSignup = false

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
        viewBinding = FragmentPaymentsheetAddPaymentMethodBinding.bind(view)

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

        sheetViewModel.processing.observe(viewLifecycleOwner) { isProcessing ->
            viewBinding.linkInlineSignup.isEnabled = !isProcessing
        }

        viewBinding.linkInlineSignup.apply {
            linkLauncher = sheetViewModel.linkLauncher
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    viewBinding.linkInlineSignup.isSelected,
                    viewBinding.linkInlineSignup.isReady
                ) { isSelected, isReady ->
                    if (isSelected) {
                        PrimaryButton.UIState(
                            label = null,
                            onClick = sheetViewModel::payWithLink,
                            enabled = isReady,
                            visible = true
                        )
                    } else {
                        null
                    }
                }.collect {
                    sheetViewModel.updatePrimaryButtonUIState(it)
                }
            }
        }

        if (paymentMethods.isNotEmpty()) {
            // If the activity is destroyed and recreated, then the fragment is already present
            // and doesn't need to be replaced, only the selected payment method needs to be set
            if (savedInstanceState == null) {
                replacePaymentMethodFragment(paymentMethods[selectedPaymentMethodIndex])
            }
            updateLinkInlineSignupVisibility(paymentMethods[selectedPaymentMethodIndex])
        }

        sheetViewModel.eventReporter.onShowNewPaymentOptionForm()
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
                        if (sheetViewModel.getAddFragmentSelectedLpmValue() != selectedLpm) {
                            onPaymentMethodSelected(selectedLpm)
                        }
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

        updateLinkInlineSignupVisibility(paymentMethod)
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
                newLpm = sheetViewModel.newLpm,
                isShowingLinkInlineSignup = showLinkInlineSignup
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
                fragmentForPaymentMethod(paymentMethod),
                args
            )
        }
    }

    private fun updateLinkInlineSignupVisibility(selectedPaymentMethod: SupportedPaymentMethod) {
        showLinkInlineSignup = sheetViewModel.isLinkEnabled.value == true &&
            selectedPaymentMethod.type == PaymentMethod.Type.Card &&
            sheetViewModel.linkLauncher.accountStatus.value == AccountStatus.SignedOut

        viewBinding.linkInlineSignup.isVisible = showLinkInlineSignup
    }

    private fun fragmentForPaymentMethod(paymentMethod: SupportedPaymentMethod) =
        when (paymentMethod.type) {
            PaymentMethod.Type.USBankAccount -> USBankAccountFormFragment::class.java
            else -> ComposeFormDataCollectionFragment::class.java
        }

    companion object {

        @VisibleForTesting
        fun getFormArguments(
            showPaymentMethod: SupportedPaymentMethod,
            stripeIntent: StripeIntent,
            config: PaymentSheet.Configuration?,
            merchantName: String,
            amount: Amount? = null,
            @InjectorKey injectorKey: String,
            newLpm: PaymentSelection.New?,
            isShowingLinkInlineSignup: Boolean = false
        ): FormFragmentArguments {

            val layoutFormDescriptor = showPaymentMethod.getPMAddForm(stripeIntent, config)

            return FormFragmentArguments(
                paymentMethod = showPaymentMethod,
                showCheckbox = layoutFormDescriptor.showCheckbox && !isShowingLinkInlineSignup,
                showCheckboxControlledFields = newLpm?.let {
                    newLpm.customerRequestedSave ==
                        PaymentSelection.CustomerRequestedSave.RequestReuse
                } ?: layoutFormDescriptor.showCheckboxControlledFields,
                merchantName = merchantName,
                amount = amount,
                billingDetails = config?.defaultBillingDetails,
                injectorKey = injectorKey,
                initialPaymentMethodCreateParams =
                newLpm?.paymentMethodCreateParams?.typeCode?.takeIf {
                    it == showPaymentMethod.type.code
                }?.let {
                    when (newLpm) {
                        is PaymentSelection.New.GenericPaymentMethod ->
                            newLpm.paymentMethodCreateParams
                        is PaymentSelection.New.Card ->
                            newLpm.paymentMethodCreateParams
                        else -> null
                    }
                }
            )
        }
    }
}
