package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.databinding.FragmentPaymentsheetAddPaymentMethodBinding
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import com.stripe.android.paymentsheet.paymentdatacollection.CardDataCollectionFragment
import com.stripe.android.paymentsheet.paymentdatacollection.ComposeFormDataCollectionFragment
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.paymentsheet.paymentdatacollection.TransformToPaymentMethodCreateParams
import com.stripe.android.paymentsheet.ui.AnimationConstants
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.Amount
import kotlinx.coroutines.launch

internal abstract class BaseAddPaymentMethodFragment : Fragment() {
    abstract val viewModelFactory: ViewModelProvider.Factory
    abstract val sheetViewModel: BaseSheetViewModel<*>

    protected lateinit var addPaymentMethodHeader: TextView

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
        addPaymentMethodHeader = viewBinding.addPaymentMethodHeader

        val paymentMethods = sheetViewModel.supportedPaymentMethods
        viewBinding.googlePayDivider.setText(
            if (paymentMethods.contains(SupportedPaymentMethod.Card) &&
                paymentMethods.size == 1
            ) {
                R.string.stripe_paymentsheet_or_pay_with_card
            } else {
                R.string.stripe_paymentsheet_or_pay_using
            }
        )

        val selectedPaymentMethodIndex = paymentMethods.indexOf(
            sheetViewModel.getAddFragmentSelectedLPM()
        ).takeUnless { it == -1 } ?: 0

        if (paymentMethods.size > 1) {
            setupRecyclerView(viewBinding, paymentMethods)
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
                    sheetViewModel.updateSelection(
                        transformToPaymentSelection(
                            formFieldValues,
                            formFragment.paramKeySpec,
                            sheetViewModel.getAddFragmentSelectedLPM()
                        )
                    )
                }
            }
        }
    }

    private fun setupRecyclerView(
        viewBinding: FragmentPaymentsheetAddPaymentMethodBinding,
        paymentMethods: List<SupportedPaymentMethod>
    ) {
//        viewBinding.paymentMethodsRecycler.isVisible = true
//        // The default item animator conflicts with `animateLayoutChanges`, causing a crash when
//        // quickly switching payment methods. Set to null since the items never change anyway.
//        viewBinding.paymentMethodsRecycler.itemAnimator = null
//
//        val layoutManager = object : LinearLayoutManager(
//            activity,
//            HORIZONTAL,
//            false
//        ) {
//            var canScroll = true
//
//            override fun canScrollHorizontally(): Boolean {
//                return canScroll && super.canScrollHorizontally()
//            }
//        }.also {
//            viewBinding.paymentMethodsRecycler.layoutManager = it
//        }
        val selectedPaymentMethodIndex = paymentMethods.indexOf(
            sheetViewModel.getAddFragmentSelectedLPM()
        ).takeUnless { it == -1 } ?: 0

        viewBinding.paymentMethodsRecycler.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                view?.rootView?.let {
                    val viewWidth = AddPaymentMethodsAdapter.calculateViewWidth(
                        it,
                        paymentMethods.size
                    )

                    PaymentMethodsUI(
                        viewWidth = viewWidth,
                        selectedIndex = selectedPaymentMethodIndex,
                        isEnabled = true,
                        lpms = paymentMethods,
                        onItemSelectedListener = { selectedLpm ->
                            onPaymentMethodSelected(selectedLpm)
                        }
                    )

                }
            }
        }

//        val adapter = AddPaymentMethodsAdapter(
//            paymentMethods,
//            selectedItemPosition,
//            ::onPaymentMethodSelected
//        ).also {
//            viewBinding.paymentMethodsRecycler.adapter = it
//        }
//
//        sheetViewModel.processing.observe(viewLifecycleOwner) { isProcessing ->
//            adapter.isEnabled = !isProcessing
//            layoutManager.canScroll = !isProcessing
//        }
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
                injectorKey = sheetViewModel.injectorKey
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

    private fun getFragment() =
        childFragmentManager.findFragmentById(R.id.payment_method_fragment_container)

    companion object {

        private fun fragmentForPaymentMethod(paymentMethod: SupportedPaymentMethod) =
            when (paymentMethod) {
                SupportedPaymentMethod.Card -> {
                    CardDataCollectionFragment::class.java
                }
                else -> {
                    ComposeFormDataCollectionFragment::class.java
                }
            }

        private val transformToPaymentMethodCreateParams = TransformToPaymentMethodCreateParams()

        @VisibleForTesting
        internal fun transformToPaymentSelection(
            formFieldValues: FormFieldValues?,
            paramKey: Map<String, Any?>,
            selectedPaymentMethodResources: SupportedPaymentMethod,
        ) = formFieldValues?.let {
            transformToPaymentMethodCreateParams.transform(formFieldValues, paramKey)
                ?.run {
                    PaymentSelection.New.GenericPaymentMethod(
                        selectedPaymentMethodResources.displayNameResource,
                        selectedPaymentMethodResources.iconResource,
                        this,
                        customerRequestedSave = formFieldValues.userRequestedReuse
                    )
                }
        }

        @VisibleForTesting
        internal fun getFormArguments(
            showPaymentMethod: SupportedPaymentMethod,
            stripeIntent: StripeIntent,
            config: PaymentSheet.Configuration?,
            merchantName: String,
            amount: Amount? = null,
            @InjectorKey injectorKey: String
        ): FormFragmentArguments {

            val layoutFormDescriptor = showPaymentMethod.getPMAddForm(stripeIntent, config)

            return FormFragmentArguments(
                paymentMethod = showPaymentMethod,
                showCheckbox = layoutFormDescriptor.showCheckbox,
                showCheckboxControlledFields = layoutFormDescriptor.showCheckboxControlledFields,
                merchantName = merchantName,
                amount = amount,
                billingDetails = config?.defaultBillingDetails,
                injectorKey = injectorKey
            )
        }
    }
}
