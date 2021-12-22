package com.stripe.android.paymentsheet

import android.content.Context
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentIntentFixtures.PI_OFF_SESSION
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheetFixtures.COMPOSE_FRAGMENT_ARGS
import com.stripe.android.paymentsheet.PaymentSheetViewModel.CheckoutIdentifier
import com.stripe.android.paymentsheet.databinding.FragmentPaymentsheetAddPaymentMethodBinding
import com.stripe.android.paymentsheet.databinding.PrimaryButtonBinding
import com.stripe.android.paymentsheet.databinding.StripeGooglePayButtonBinding
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.FragmentConfigFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import com.stripe.android.paymentsheet.paymentdatacollection.CardDataCollectionFragment
import com.stripe.android.paymentsheet.paymentdatacollection.ComposeFormDataCollectionFragment
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.forms.FormFieldEntry
import com.stripe.android.utils.TestUtils.idleLooper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class PaymentSheetAddPaymentMethodFragmentTest : PaymentSheetViewModelTestInjection() {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        PaymentConfiguration.init(
            context,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @After
    override fun after() {
        super.after()
    }

    @Test
    fun `when processing google pay should be disabled`() {
        createFragment { fragment, viewBinding, _ ->
            fragment.sheetViewModel._processing.value = true
            assertThat(viewBinding.googlePayButton.isEnabled).isFalse()
        }
    }

    @Test
    fun `when processing then payment methods UI should be disabled`() {
        val paymentIntent = mock<PaymentIntent>().also {
            whenever(it.paymentMethodTypes).thenReturn(listOf("card", "bancontact"))
        }
        createFragment(stripeIntent = paymentIntent) { fragment, viewBinding, _ ->
            idleLooper()
            fragment.sheetViewModel._processing.value = true
            val adapter =
                viewBinding.paymentMethodsRecycler.adapter as AddPaymentMethodsAdapter
            assertThat(adapter.isEnabled).isFalse()
        }
    }

    @Test
    @Config(qualifiers = "w320dp")
    fun `when screen is 320dp wide, adapter should show 2 and a half items with 104dp width`() {
        val paymentIntent = mock<PaymentIntent>().also {
            whenever(it.paymentMethodTypes).thenReturn(listOf("card", "bancontact", "sofort", "ideal"))
        }
        createFragment(stripeIntent = paymentIntent) { _, viewBinding, _ ->
            val item = viewBinding.paymentMethodsRecycler.layoutManager!!.findViewByPosition(0)
            assertThat(item!!.measuredWidth).isEqualTo(104)
        }
    }

    @Test
    @Config(qualifiers = "w475dp")
    fun `when screen is 475dp wide, adapter should show 2 items evenly spread out`() {
        val paymentIntent = mock<PaymentIntent>().also {
            whenever(it.paymentMethodTypes).thenReturn(listOf("card", "bancontact"))
        }
        createFragment(stripeIntent = paymentIntent) { _, viewBinding, _ ->
            val item = viewBinding.paymentMethodsRecycler.layoutManager!!.findViewByPosition(0)
            assertThat(item!!.measuredWidth).isEqualTo(211)
        }
    }

    @Test
    fun `when isGooglePayEnabled=true should configure Google Pay button`() {
        createFragment { fragment, viewBinding, _ ->
            val paymentSelections = mutableListOf<PaymentSelection>()
            fragment.sheetViewModel.selection.observeForever { paymentSelection ->
                if (paymentSelection != null) {
                    paymentSelections.add(paymentSelection)
                }
            }

            assertThat(viewBinding.googlePayButton.isVisible)
                .isTrue()

            viewBinding.googlePayButton.performClick()

            assertThat(paymentSelections)
                .containsExactly(PaymentSelection.GooglePay)
        }
    }

    @Test
    fun `when back to Ready state should update PaymentSelection`() {
        createFragment(
            paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        ) { fragment, viewBinding, _ ->
            fragment.sheetViewModel.savedStateHandle.set(BaseSheetViewModel.SAVE_PROCESSING, true)

            idleLooper()
            val paymentSelections = mutableListOf<PaymentSelection?>()
            fragment.sheetViewModel.selection.observeForever { paymentSelection ->
                paymentSelections.add(paymentSelection)
            }

            assertThat(viewBinding.googlePayButton.isVisible)
                .isFalse()

            idleLooper()
            // Start with null PaymentSelection because the card entered is invalid
            assertThat(paymentSelections.size)
                .isEqualTo(1)
            assertThat(paymentSelections[0])
                .isNull()

            viewBinding.googlePayButton.performClick()

            // Updates PaymentSelection to Google Pay
            assertThat(paymentSelections.size)
                .isEqualTo(2)
            assertThat(paymentSelections[1])
                .isEqualTo(PaymentSelection.GooglePay)

            fragment.sheetViewModel._viewState.value = PaymentSheetViewState.Reset(null)

            // Back to Ready state, should return to null PaymentSelection
            assertThat(paymentSelections.size)
                .isEqualTo(3)
            assertThat(paymentSelections[2])
                .isNull()
        }
    }

    @Test
    fun `started fragment should report onShowNewPaymentOptionForm() event`() {
        createFragment { _, _, _ ->
            idleLooper()
            verify(eventReporter).onShowNewPaymentOptionForm()
        }
    }

    @Test
    fun `google pay button state updated on start processing`() {
        createFragment(PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY) { fragment, viewBinding, _ ->
            fragment.sheetViewModel.checkoutIdentifier = CheckoutIdentifier.AddFragmentTopGooglePay
            fragment.sheetViewModel._viewState.value = PaymentSheetViewState.StartProcessing

            val googlePayButton =
                StripeGooglePayButtonBinding.bind(viewBinding.googlePayButton)
            val googlePayPrimaryComponent =
                PrimaryButtonBinding.bind(googlePayButton.primaryButton)
            val googlePayIconComponent = googlePayButton.googlePayButtonIcon
            assertThat(googlePayButton.primaryButton.isVisible).isTrue()
            assertThat(googlePayIconComponent.isVisible).isFalse()
            assertThat(googlePayPrimaryComponent.label.text).isEqualTo(
                fragment.getString(R.string.stripe_paymentsheet_primary_button_processing)
            )
        }
    }

    @Test
    fun `google pay button error message displayed`() {
        createFragment(PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY) { fragment, viewBinding, _ ->
            fragment.sheetViewModel.checkoutIdentifier = CheckoutIdentifier.AddFragmentTopGooglePay
            fragment.sheetViewModel._viewState.value =
                PaymentSheetViewState.Reset(BaseSheetViewModel.UserErrorMessage("This is my test error message"))

            assertThat(viewBinding.message.text.toString()).isEqualTo("This is my test error message")

            fragment.sheetViewModel._viewState.value = PaymentSheetViewState.Reset(null)

            assertThat(viewBinding.message.text.toString()).isEqualTo("")
        }
    }

    @Test
    fun `google pay flow updates the scroll view before and after`() {

        createFragment(PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY) { fragment, viewBinding, _ ->

            viewBinding.googlePayButton.performClick()
            assertThat(fragment.sheetViewModel._contentVisible.value).isEqualTo(false)

            fragment.sheetViewModel.onGooglePayResult(GooglePayPaymentMethodLauncher.Result.Canceled)
            assertThat(fragment.sheetViewModel._contentVisible.value).isEqualTo(true)
        }
    }

    @Test
    fun `google pay button state updated on finish processing`() {
        createFragment(PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY) { fragment, viewBinding, _ ->
            fragment.sheetViewModel.checkoutIdentifier = CheckoutIdentifier.AddFragmentTopGooglePay

            var finishProcessingCalled = false
            fragment.sheetViewModel._viewState.value =
                PaymentSheetViewState.FinishProcessing {
                    finishProcessingCalled = true
                }

            idleLooper()

            val googlePayButton =
                StripeGooglePayButtonBinding.bind(viewBinding.googlePayButton)
            val googlePayIconComponent = googlePayButton.googlePayButtonIcon
            assertThat(googlePayButton.primaryButton.isVisible).isTrue()
            assertThat(googlePayIconComponent.isVisible).isFalse()
            assertThat(finishProcessingCalled).isTrue()
        }
    }

    @Test
    fun `when Google Pay is cancelled then previously selected payment method is selected again`() {
        createFragment(PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY) { fragment, viewBinding, _ ->
            val lastPaymentMethod =
                PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            fragment.sheetViewModel.updateSelection(lastPaymentMethod)

            viewBinding.googlePayButton.performClick()

            fragment.sheetViewModel._viewState.value = PaymentSheetViewState.Reset()

            idleLooper()

            assertThat(fragment.sheetViewModel.selection.value).isEqualTo(lastPaymentMethod)
        }
    }

    @Test
    fun `when new payment method is selected then error message is cleared`() {
        createFragment(PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY) { fragment, viewBinding, _ ->
            viewBinding.googlePayButton.performClick()

            val errorMessage = "Error message"
            fragment.sheetViewModel._viewState.value =
                PaymentSheetViewState.Reset(BaseSheetViewModel.UserErrorMessage(errorMessage))

            idleLooper()

            assertThat(viewBinding.message.isVisible).isTrue()
            assertThat(viewBinding.message.text).isEqualTo(errorMessage)

            fragment.sheetViewModel.updateSelection(
                PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            )

            assertThat(viewBinding.message.isVisible).isFalse()
            assertThat(viewBinding.message.text.isNullOrEmpty()).isTrue()
        }
    }

    @Test
    fun `when checkout starts then error message is cleared`() {
        createFragment(PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY) { fragment, viewBinding, _ ->
            viewBinding.googlePayButton.performClick()

            val errorMessage = "Error message"
            fragment.sheetViewModel._viewState.value =
                PaymentSheetViewState.Reset(BaseSheetViewModel.UserErrorMessage(errorMessage))

            idleLooper()

            assertThat(viewBinding.message.isVisible).isTrue()
            assertThat(viewBinding.message.text).isEqualTo(errorMessage)

            viewBinding.googlePayButton.performClick()

            assertThat(viewBinding.message.isVisible).isFalse()
            assertThat(viewBinding.message.text.isNullOrEmpty()).isTrue()
        }
    }

    @Test
    fun `when PaymentIntent only supports card it should not show payment method selector`() {
        val paymentIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
            paymentMethodTypes = listOf("card")
        )
        createFragment(stripeIntent = paymentIntent) { _, viewBinding, _ ->
            assertThat(viewBinding.paymentMethodsRecycler.isVisible).isFalse()
            assertThat(viewBinding.googlePayDivider.viewBinding.dividerText.text)
                .isEqualTo("Or pay with a card")
        }
    }

    @Test
    fun `when multiple supported payment methods and configuration changes it should restore selected payment method`() {
        val paymentIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
            paymentMethodTypes = listOf("card", "bancontact")
        )
        createFragment(stripeIntent = paymentIntent) { fragment, viewBinding, _ ->
            idleLooper()
            assertThat(
                fragment.childFragmentManager.findFragmentById(
                    viewBinding.paymentMethodFragmentContainer.id
                )
            ).isInstanceOf(CardDataCollectionFragment::class.java)

            fragment.onPaymentMethodSelected(SupportedPaymentMethod.Bancontact)
            idleLooper()

            val addedFragment = fragment.childFragmentManager.findFragmentById(
                viewBinding.paymentMethodFragmentContainer.id
            )

            assertThat(addedFragment).isInstanceOf(ComposeFormDataCollectionFragment::class.java)
            assertThat(
                addedFragment?.arguments?.getParcelable<FormFragmentArguments>(
                    ComposeFormDataCollectionFragment.EXTRA_CONFIG
                )
            )
                .isEqualTo(
                    FormFragmentArguments(
                        SupportedPaymentMethod.Bancontact,
                        showCheckbox = false,
                        showCheckboxControlledFields = false,
                        merchantName = PaymentSheetFixtures.MERCHANT_DISPLAY_NAME,
                        amount = createAmount(),
                        injectorKey = "testInjectorKeyAddFragmentTest"
                    )
                )
        }.recreate().onFragment { fragment ->
            val addedFragment = fragment.childFragmentManager.findFragmentById(
                FragmentPaymentsheetAddPaymentMethodBinding.bind(
                    requireNotNull(fragment.view)
                ).paymentMethodFragmentContainer.id
            )

            assertThat(addedFragment).isInstanceOf(ComposeFormDataCollectionFragment::class.java)
            assertThat(
                addedFragment?.arguments?.getParcelable<FormFragmentArguments>(
                    ComposeFormDataCollectionFragment.EXTRA_CONFIG
                )
            )
                .isEqualTo(
                    FormFragmentArguments(
                        SupportedPaymentMethod.Bancontact,
                        showCheckbox = false,
                        showCheckboxControlledFields = false,
                        merchantName = PaymentSheetFixtures.MERCHANT_DISPLAY_NAME,
                        amount = createAmount(),
                        injectorKey = "testInjectorKeyAddFragmentTest"
                    )
                )
        }
    }

    @Test
    fun `when PaymentIntent allows multiple supported payment methods it should show payment method selector`() {
        val paymentIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
            paymentMethodTypes = listOf("card", "bancontact")
        )
        createFragment(stripeIntent = paymentIntent) { _, viewBinding, _ ->
            assertThat(viewBinding.paymentMethodsRecycler.isVisible).isTrue()
            assertThat(viewBinding.googlePayDivider.viewBinding.dividerText.text)
                .isEqualTo("Or pay using")
        }
    }

    @Test
    fun `when payment method is selected then transitions to correct fragment`() {
        val args = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY
        val stripeIntent = PaymentIntentFixtures.PI_WITH_SHIPPING
        createFragment(
            stripeIntent = stripeIntent,
            args = args
        ) { fragment, viewBinding, _ ->
            assertThat(
                fragment.childFragmentManager.findFragmentById(
                    viewBinding.paymentMethodFragmentContainer.id
                )
            ).isInstanceOf(CardDataCollectionFragment::class.java)

            fragment.onPaymentMethodSelected(SupportedPaymentMethod.Card)

            idleLooper()

            val addedFragment = fragment.childFragmentManager.findFragmentById(
                viewBinding.paymentMethodFragmentContainer.id
            )

            assertThat(addedFragment).isInstanceOf(CardDataCollectionFragment::class.java)
            assertThat(
                addedFragment?.arguments?.getParcelable<FormFragmentArguments>(
                    ComposeFormDataCollectionFragment.EXTRA_CONFIG
                )
            )
                .isEqualTo(
                    COMPOSE_FRAGMENT_ARGS.copy(
                        paymentMethod = SupportedPaymentMethod.Card,
                        amount = createAmount(),
                        showCheckbox = true,
                        showCheckboxControlledFields = true,
                        billingDetails = null
                    ),
                )
        }
    }

    @Test
    fun `when payment method is selected then merchant name is passed in fragment arguments`() {
        val args = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY
        val stripeIntent = PaymentIntentFixtures.PI_WITH_SHIPPING
        createFragment(
            stripeIntent = stripeIntent,
            args = args
        ) { fragment, viewBinding, _ ->
            fragment.onPaymentMethodSelected(SupportedPaymentMethod.Card)

            idleLooper()

            val addedFragment = fragment.childFragmentManager.findFragmentById(
                viewBinding.paymentMethodFragmentContainer.id
            )

            assertThat(addedFragment).isInstanceOf(CardDataCollectionFragment::class.java)

            assertThat(
                addedFragment?.arguments?.getParcelable<FormFragmentArguments>(
                    ComposeFormDataCollectionFragment.EXTRA_CONFIG
                )
            )
                .isEqualTo(
                    COMPOSE_FRAGMENT_ARGS.copy(
                        paymentMethod = SupportedPaymentMethod.Card,
                        amount = createAmount(),
                        showCheckbox = true,
                        showCheckboxControlledFields = true,
                        billingDetails = null
                    ),
                )
        }
    }

    @Test
    fun `when payment method selection changes then it's updated in ViewModel`() {
        createFragment { fragment, viewBinding, _ ->
            assertThat(
                fragment.childFragmentManager.findFragmentById(
                    viewBinding.paymentMethodFragmentContainer.id
                )
            ).isInstanceOf(CardDataCollectionFragment::class.java)

            var paymentSelection: PaymentSelection? = null
            fragment.sheetViewModel.selection.observeForever {
                paymentSelection = it
            }

            fragment.sheetViewModel.updateSelection(
                PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            )
            assertThat(paymentSelection).isInstanceOf(PaymentSelection.Saved::class.java)

            fragment.onPaymentMethodSelected(SupportedPaymentMethod.Card)
            idleLooper()
            assertThat(paymentSelection).isNull()
        }
    }

    @Test
    fun `when payment intent off session fragment parameters set correctly`() {
        val args = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY
        val stripeIntent = PI_OFF_SESSION
        createFragment(stripeIntent = stripeIntent, args = args) { fragment, viewBinding, _ ->
            idleLooper()
            assertThat(
                fragment.childFragmentManager.findFragmentById(
                    viewBinding.paymentMethodFragmentContainer.id
                )
            ).isInstanceOf(CardDataCollectionFragment::class.java)

            fragment.onPaymentMethodSelected(SupportedPaymentMethod.Card)

            idleLooper()

            val addedFragment = fragment.childFragmentManager.findFragmentById(
                viewBinding.paymentMethodFragmentContainer.id
            )

            assertThat(addedFragment).isInstanceOf(CardDataCollectionFragment::class.java)
            assertThat(
                addedFragment?.arguments?.getParcelable<FormFragmentArguments>(
                    ComposeFormDataCollectionFragment.EXTRA_CONFIG
                )
            )
                .isEqualTo(
                    COMPOSE_FRAGMENT_ARGS.copy(
                        paymentMethod = SupportedPaymentMethod.Card,
                        amount = createAmount(PI_OFF_SESSION),
                        showCheckbox = false,
                        showCheckboxControlledFields = true,
                        billingDetails = null
                    )
                )
        }
    }

    @Test
    fun `payment method selection has the fields from formFieldValues`() {
        val formFieldValues = FormFieldValues(
            fieldValuePairs = mapOf(
                IdentifierSpec.SaveForFutureUse to FormFieldEntry("true", true)
            ),
            showsMandate = false,
            userRequestedReuse = PaymentSelection.CustomerRequestedSave.RequestReuse
        )
        val selection =
            BaseAddPaymentMethodFragment.transformToPaymentSelection(
                formFieldValues,
                mapOf(
                    "type" to "sofort"
                ),
                SupportedPaymentMethod.Sofort
            )
        assertThat(selection?.customerRequestedSave).isEqualTo(
            PaymentSelection.CustomerRequestedSave.RequestReuse
        )
        assertThat(selection?.labelResource).isEqualTo(
            R.string.stripe_paymentsheet_payment_method_sofort
        )
        assertThat(selection?.iconResource).isEqualTo(
            R.drawable.stripe_ic_paymentsheet_pm_klarna
        )
    }

    @Test
    fun `Factory gets initialized by Injector when Injector is available`() {
        createFragment(registerInjector = true) { fragment, _, viewModel ->
            assertThat(fragment.sheetViewModel).isEqualTo(viewModel)
        }
    }

    @Test
    fun `Factory gets initialized with fallback when no Injector is available`() = runBlockingTest {
        createFragment(registerInjector = false) { fragment, _, viewModel ->
            assertThat(fragment.sheetViewModel).isNotEqualTo(viewModel)
        }
    }

    private fun createAmount(paymentIntent: PaymentIntent = PaymentIntentFixtures.PI_WITH_SHIPPING) =
        Amount(paymentIntent.amount!!, paymentIntent.currency!!)

    private fun createFragment(
        args: PaymentSheetContract.Args = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
            injectorKey = "testInjectorKeyAddFragmentTest"
        ),
        fragmentConfig: FragmentConfig? = FragmentConfigFixtures.DEFAULT,
        paymentMethods: List<PaymentMethod> = emptyList(),
        stripeIntent: StripeIntent? = PaymentIntentFixtures.PI_WITH_SHIPPING,
        registerInjector: Boolean = true,
        onReady: (PaymentSheetAddPaymentMethodFragment, FragmentPaymentsheetAddPaymentMethodBinding, PaymentSheetViewModel) -> Unit
    ): FragmentScenario<PaymentSheetAddPaymentMethodFragment> {
        assertThat(WeakMapInjectorRegistry.staticCacheMap.size).isEqualTo(0)
        val viewModel = createViewModel(
            stripeIntent as PaymentIntent,
            customerRepositoryPMs = paymentMethods,
            injectorKey = args.injectorKey
        )

        return launchFragmentInContainer<PaymentSheetAddPaymentMethodFragment>(
            bundleOf(
                PaymentSheetActivity.EXTRA_FRAGMENT_CONFIG to fragmentConfig,
                PaymentSheetActivity.EXTRA_STARTER_ARGS to args
            ),
            R.style.StripePaymentSheetDefaultTheme,
            initialState = Lifecycle.State.INITIALIZED
        ).moveToState(Lifecycle.State.CREATED).onFragment { fragment ->
            viewModel.updatePaymentMethods(stripeIntent)
            viewModel.setStripeIntent(stripeIntent)
            idleLooper()
            if (registerInjector) {
                registerViewModel(args.injectorKey, viewModel)
            }
        }.moveToState(Lifecycle.State.STARTED).onFragment { fragment ->
            onReady(
                fragment,
                FragmentPaymentsheetAddPaymentMethodBinding.bind(
                    requireNotNull(fragment.view)
                ),
                viewModel
            )
        }
    }
}
