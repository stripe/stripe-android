package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.databinding.FragmentPaymentsheetAddPaymentMethodBinding
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.Amount
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import com.stripe.android.paymentsheet.paymentdatacollection.CardDataCollectionFragment
import com.stripe.android.paymentsheet.paymentdatacollection.ComposeFormDataCollectionFragment
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.paymentsheet.paymentdatacollection.TransformToPaymentMethodCreateParams
import com.stripe.android.paymentsheet.elements.IdentifierSpec
import com.stripe.android.paymentsheet.ui.AddPaymentMethodsFragmentFactory
import com.stripe.android.paymentsheet.ui.AnimationConstants
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

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

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewBinding = FragmentPaymentsheetAddPaymentMethodBinding.bind(view)
        addPaymentMethodHeader = viewBinding.addPaymentMethodHeader

        val paymentMethods = sheetViewModel.getSupportedPaymentMethods()

        viewBinding.googlePayDivider.setText(
            if (paymentMethods.contains(SupportedPaymentMethod.Card) && paymentMethods.size == 1) {
                R.string.stripe_paymentsheet_or_pay_with_card
            } else {
                R.string.stripe_paymentsheet_or_pay_using
            }
        )

        if (paymentMethods.size > 1) {
            setupRecyclerView(viewBinding, paymentMethods)
        }

        replacePaymentMethodFragment(paymentMethods[0])

        sheetViewModel.processing.observe(viewLifecycleOwner) { isProcessing ->
            (getFragment() as? ComposeFormDataCollectionFragment)?.setProcessing(isProcessing)
        }

        childFragmentManager.addFragmentOnAttachListener { _, fragment ->
            (fragment as? ComposeFormDataCollectionFragment)?.let { formFragment ->
                formFragment.formViewModel.completeFormValues.asLiveData()
                    .observe(viewLifecycleOwner) { formFieldValues ->
                        sheetViewModel.updateSelection(
                            transformToPaymentSelection(
                                formFieldValues,
                                formFragment.formSpec.paramKey,
                                selectedPaymentMethod
                            )
                        )
                    }
            }
        }

        eventReporter.onShowNewPaymentOptionForm()
    }

    private fun setupRecyclerView(
        viewBinding: FragmentPaymentsheetAddPaymentMethodBinding,
        paymentMethods: List<SupportedPaymentMethod>
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
        replacePaymentMethodFragment(paymentMethod)
    }

    private fun replacePaymentMethodFragment(paymentMethod: SupportedPaymentMethod) {
        selectedPaymentMethod = paymentMethod

        val args = requireArguments()
        args.putParcelable(
            ComposeFormDataCollectionFragment.EXTRA_CONFIG,
            getFormArguments(
                hasCustomer = sheetViewModel.customerConfig != null,
                saveForFutureUse = (
                    sheetViewModel.stripeIntent.value is SetupIntent ||
                        (
                            (sheetViewModel.stripeIntent.value as? PaymentIntent)
                                ?.setupFutureUsage == StripeIntent.Usage.OffSession
                            )
                    ),
                supportedPaymentMethodName = paymentMethod.name,
                merchantName = sheetViewModel.merchantName,
                amount = sheetViewModel.amount.value,
                billingAddress = sheetViewModel.config?.defaultBillingDetails
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
                        formFieldValues
                            .fieldValuePairs[IdentifierSpec.SaveForFutureUse]
                            ?.value
                            .toBoolean()
                    )
                }
        }

        @VisibleForTesting
        internal fun getFormArguments(
            hasCustomer: Boolean,
            saveForFutureUse: Boolean,
            supportedPaymentMethodName: String,
            merchantName: String,
            amount: Amount? = null,
            billingAddress: PaymentSheet.BillingDetails? = null
        ): FormFragmentArguments {
            var saveForFutureUseValue = true
            var saveForFutureUseVisible = true
            if (!hasCustomer) {
                saveForFutureUseValue = false
                saveForFutureUseVisible = false
            }

            // The order is important here, even if there is a customer the save for future
            // use value should be true to collect all the details
            if (saveForFutureUse) {
                saveForFutureUseVisible = false
                saveForFutureUseValue = true
            }

            return FormFragmentArguments(
                supportedPaymentMethodName = supportedPaymentMethodName,
                saveForFutureUseInitialVisibility = saveForFutureUseVisible,
                saveForFutureUseInitialValue = saveForFutureUseValue,
                merchantName = merchantName,
                amount = amount,
                billingDetails = billingAddress?.let {
                    PaymentSheet.BillingDetails(
                        name = billingAddress.name,
                        email = billingAddress.email,
                        phone = billingAddress.phone,
                        address = billingAddress.address?.let {
                            PaymentSheet.Address(
                                city = it.city,
                                state = it.state,
                                country = it.country,
                                line1 = it.line1,
                                line2 = it.line2,
                                postalCode = it.postalCode
                            )
                        }
                    )
                }
            )
        }
    }
}
