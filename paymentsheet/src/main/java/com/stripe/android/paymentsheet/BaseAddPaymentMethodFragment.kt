package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.databinding.FragmentPaymentsheetAddPaymentMethodBinding
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import com.stripe.android.paymentsheet.paymentdatacollection.CardDataCollectionFragment
import com.stripe.android.paymentsheet.paymentdatacollection.ComposeFormDataCollectionFragment
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.paymentsheet.paymentdatacollection.TransformToPaymentMethodCreateParams
import com.stripe.android.paymentsheet.ui.AddPaymentMethodsFragmentFactory
import com.stripe.android.paymentsheet.ui.AnimationConstants
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.Amount
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal abstract class BaseAddPaymentMethodFragment(
    private val eventReporter: EventReporter
) : Fragment() {
    abstract val viewModelFactory: ViewModelProvider.Factory
    abstract val sheetViewModel: BaseSheetViewModel<*>

    protected lateinit var addPaymentMethodHeader: TextView

    private lateinit var selectedPaymentMethod: SupportedPaymentMethod

    override fun onCreate(savedInstanceState: Bundle?) {
        // When the fragment is destroyed and recreated, the child fragment is re-instantiated
        // during onCreate, so the factory must be set before calling super.
        childFragmentManager.fragmentFactory = AddPaymentMethodsFragmentFactory(
            sheetViewModel::class.java, viewModelFactory
        )
        super.onCreate(savedInstanceState)
    }

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
            SupportedPaymentMethod.fromCode(savedInstanceState?.getString(SELECTED_PAYMENT_METHOD))
        ).takeUnless { it == -1 } ?: 0

        if (paymentMethods.size > 1) {
            setupRecyclerView(viewBinding, paymentMethods, selectedPaymentMethodIndex)
        }

        if (paymentMethods.isNotEmpty()) {
            // If the activity is destroyed and recreated, then the fragment is already present
            // and doesn't need to be replaced, only the selected payment method needs to be set
            if (savedInstanceState == null) {
                replacePaymentMethodFragment(paymentMethods[selectedPaymentMethodIndex])
            } else {
                selectedPaymentMethod = paymentMethods[selectedPaymentMethodIndex]
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

        eventReporter.onShowNewPaymentOptionForm()
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
                            selectedPaymentMethod
                        )
                    )
                }
            }
        }
    }

    private fun setupRecyclerView(
        viewBinding: FragmentPaymentsheetAddPaymentMethodBinding,
        paymentMethods: List<SupportedPaymentMethod>,
        selectedItemPosition: Int
    ) {
        viewBinding.paymentMethodsRecycler.isVisible = true
        // The default item animator conflicts with `animateLayoutChanges`, causing a crash when
        // quickly switching payment methods. Set to null since the items never change anyway.
        viewBinding.paymentMethodsRecycler.itemAnimator = null

        val layoutManager = object : LinearLayoutManager(
            activity,
            HORIZONTAL,
            false
        ) {
            var canScroll = true

            override fun canScrollHorizontally(): Boolean {
                return canScroll && super.canScrollHorizontally()
            }
        }.also {
            viewBinding.paymentMethodsRecycler.layoutManager = it
        }

        val adapter = AddPaymentMethodsAdapter(
            paymentMethods,
            selectedItemPosition,
            ::onPaymentMethodSelected
        ).also {
            viewBinding.paymentMethodsRecycler.adapter = it
        }

        sheetViewModel.processing.observe(viewLifecycleOwner) { isProcessing ->
            adapter.isEnabled = !isProcessing
            layoutManager.canScroll = !isProcessing
        }
    }

    @VisibleForTesting
    internal fun onPaymentMethodSelected(paymentMethod: SupportedPaymentMethod) {
        // hide the soft keyboard.
        ViewCompat.getWindowInsetsController(requireView())
            ?.hide(WindowInsetsCompat.Type.ime())

        replacePaymentMethodFragment(paymentMethod)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(SELECTED_PAYMENT_METHOD, selectedPaymentMethod.type.code)
        super.onSaveInstanceState(outState)
    }

    private fun replacePaymentMethodFragment(paymentMethod: SupportedPaymentMethod) {
        selectedPaymentMethod = paymentMethod

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
        private const val SELECTED_PAYMENT_METHOD = "selected_pm"

        private fun fragmentForPaymentMethod(paymentMethod: SupportedPaymentMethod) =
            when (paymentMethod) {
                SupportedPaymentMethod.Card -> CardDataCollectionFragment::class.java
                else -> ComposeFormDataCollectionFragment::class.java
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
