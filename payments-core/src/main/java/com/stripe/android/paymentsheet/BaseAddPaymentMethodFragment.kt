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
import com.stripe.android.R
import com.stripe.android.databinding.FragmentPaymentsheetAddPaymentMethodBinding
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.forms.TransformFormToPaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import com.stripe.android.paymentsheet.paymentdatacollection.CardDataCollectionFragment
import com.stripe.android.paymentsheet.paymentdatacollection.ComposeFormDataCollectionFragment
import com.stripe.android.paymentsheet.ui.AddPaymentMethodsFragmentFactory
import com.stripe.android.paymentsheet.ui.AnimationConstants
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

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

                // I think we need to setup the shared formViewModel in another way.
                val formViewModel = formFragment.formViewModel
                val formSpec = formFragment.formSpec

                formViewModel.completeFormValues.map { formFieldValues ->
                    formFieldValues?.let { it ->
                        selectedPaymentMethod.paymentMethodCreateParams(
                            TransformFormToPaymentMethod().transform(
                                formSpec.paramKey,
                                it
                            )
                                .filterOutNullValues()
                                .toMap()
                        )?.let { paymentMethodCreateParams ->
                            sheetViewModel.updateSelection(
                                paymentMethodCreateParams.let {
                                    PaymentSelection.New.GenericPaymentMethod(
                                        selectedPaymentMethod.displayNameResource,
                                        selectedPaymentMethod.iconResource,
                                        it,
                                        formFieldValues.saveForFutureUse
                                    )
                                }
                            )
                        }
                    }
                }.distinctUntilChanged().asLiveData()
            }
        }

        eventReporter.onShowNewPaymentOptionForm()
    }

    private fun setupRecyclerView(
        viewBinding: FragmentPaymentsheetAddPaymentMethodBinding,
        paymentMethods: List<SupportedPaymentMethod>
    ) {
        viewBinding.paymentMethodsRecycler.isVisible = true
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

        val adapter = AddPaymentMethodsAdapter(paymentMethods, ::onPaymentMethodSelected).also {
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
            ComposeFormDataCollectionFragment.EXTRA_FORM_TYPE,
            paymentMethod.formType
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
    }
}

@Suppress("UNCHECKED_CAST")
private fun <K, V> Map<K, V?>.filterOutNullValues() = filterValues { it != null } as Map<K, V>
