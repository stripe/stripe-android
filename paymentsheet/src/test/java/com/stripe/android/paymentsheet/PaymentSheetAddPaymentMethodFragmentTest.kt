package com.stripe.android.paymentsheet

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentIntentFixtures.PI_OFF_SESSION
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.paymentsheet.PaymentSheetFixtures.COMPOSE_FRAGMENT_ARGS
import com.stripe.android.paymentsheet.PaymentSheetViewModel.CheckoutIdentifier
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.databinding.FragmentPaymentsheetAddPaymentMethodBinding
import com.stripe.android.paymentsheet.databinding.PrimaryButtonBinding
import com.stripe.android.paymentsheet.databinding.StripeGooglePayButtonBinding
import com.stripe.android.paymentsheet.elements.IdentifierSpec
import com.stripe.android.paymentsheet.forms.FormFieldEntry
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.Amount
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.FragmentConfigFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import com.stripe.android.paymentsheet.paymentdatacollection.CardDataCollectionFragment
import com.stripe.android.paymentsheet.paymentdatacollection.ComposeFormDataCollectionFragment
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.paymentsheet.ui.PaymentSheetFragmentFactory
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.utils.TestUtils.idleLooper
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class PaymentSheetAddPaymentMethodFragmentTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val eventReporter = mock<EventReporter>()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        PaymentConfiguration.init(
            context,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @Test
    fun `when processing google pay should be disabled`() {
        createFragment { fragment, viewBinding ->
            fragment.sheetViewModel._processing.value = true
            assertThat(viewBinding.googlePayButton.isEnabled).isFalse()
        }
    }

    @Test
    fun `when processing then payment methods UI should be disabled`() {
        val paymentIntent = mock<PaymentIntent>().also {
            whenever(it.paymentMethodTypes).thenReturn(listOf("card", "bancontact"))
        }
        createFragment(stripeIntent = paymentIntent) { fragment, viewBinding ->
            fragment.sheetViewModel._processing.value = true
            val adapter =
                viewBinding.paymentMethodsRecycler.adapter as AddPaymentMethodsAdapter
            assertThat(adapter.isEnabled).isFalse()
        }
    }

    @Test @Config(qualifiers = "w320dp")
    fun `when screen is 320dp wide, adapter should show 2 and a half items with 104dp width`() {
        val paymentIntent = mock<PaymentIntent>().also {
            whenever(it.paymentMethodTypes).thenReturn(listOf("card", "bancontact", "sofort", "ideal"))
        }
        createFragment(stripeIntent = paymentIntent) { fragment, viewBinding ->
            val item = viewBinding.paymentMethodsRecycler.layoutManager!!.findViewByPosition(0)
            assertThat(item!!.measuredWidth).isEqualTo(104)
        }
    }

    @Test @Config(qualifiers = "w475dp")
    fun `when screen is 475dp wide, adapter should show 3 and a half items with 115dp width`() {
        val paymentIntent = mock<PaymentIntent>().also {
            whenever(it.paymentMethodTypes).thenReturn(listOf("card", "bancontact", "sofort", "ideal"))
        }
        createFragment(stripeIntent = paymentIntent) { fragment, viewBinding ->
            val item = viewBinding.paymentMethodsRecycler.layoutManager!!.findViewByPosition(0)
            assertThat(item!!.measuredWidth).isEqualTo(115)
        }
    }

    @Test @Config(qualifiers = "w476dp")
    fun `when screen is 476dp wide, adapter should show 4 items with 100dp width`() {
        val paymentIntent = mock<PaymentIntent>().also {
            whenever(it.paymentMethodTypes).thenReturn(listOf("card", "bancontact", "sofort", "ideal"))
        }
        createFragment(stripeIntent = paymentIntent) { fragment, viewBinding ->
            val item = viewBinding.paymentMethodsRecycler.layoutManager!!.findViewByPosition(0)
            assertThat(item!!.measuredWidth).isEqualTo(100)
        }
    }

    @Test
    fun `when isGooglePayEnabled=true should configure Google Pay button`() {
        createFragment { fragment, viewBinding ->
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
        createFragment { fragment, viewBinding ->
            val paymentSelections = mutableListOf<PaymentSelection?>()
            fragment.sheetViewModel.selection.observeForever { paymentSelection ->
                paymentSelections.add(paymentSelection)
            }

            assertThat(viewBinding.googlePayButton.isVisible)
                .isTrue()

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
        createFragment { _, _ ->
            verify(eventReporter).onShowNewPaymentOptionForm()
        }
    }

    @Test
    fun `google pay button state updated on start processing`() {
        createFragment(PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY) { fragment, viewBinding ->
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
        createFragment(PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY) { fragment, viewBinding ->
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

        createFragment(PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY) { fragment, viewBinding ->

            viewBinding.googlePayButton.performClick()
            assertThat(fragment.sheetViewModel._contentVisible.value).isEqualTo(false)

            fragment.sheetViewModel.onGooglePayResult(GooglePayPaymentMethodLauncher.Result.Canceled)
            assertThat(fragment.sheetViewModel._contentVisible.value).isEqualTo(true)
        }
    }

    @Test
    fun `google pay button state updated on finish processing`() {
        createFragment(PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY) { fragment, viewBinding ->
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
        createFragment(PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY) { fragment, viewBinding ->
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
        createFragment(PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY) { fragment, viewBinding ->
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
        createFragment(PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY) { fragment, viewBinding ->
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
        createFragment(stripeIntent = paymentIntent) { _, viewBinding ->
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
        createFragment(stripeIntent = paymentIntent) { fragment, viewBinding ->
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
                        injectorKey = DUMMY_INJECTOR_KEY
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
                        injectorKey = DUMMY_INJECTOR_KEY
                    )
                )
        }
    }

    @Test
    fun `when PaymentIntent allows multiple supported payment methods it should show payment method selector`() {
        val paymentIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
            paymentMethodTypes = listOf("card", "bancontact")
        )
        createFragment(stripeIntent = paymentIntent) { _, viewBinding ->
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
        ) { fragment, viewBinding ->
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
        ) { fragment, viewBinding ->
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
        createFragment { fragment, viewBinding ->
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
        createFragment(stripeIntent = stripeIntent, args = args) { fragment, viewBinding ->
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

    private fun createAmount(paymentIntent: PaymentIntent = PaymentIntentFixtures.PI_WITH_SHIPPING) =
        Amount(paymentIntent.amount!!, paymentIntent.currency!!)

    private fun createFragment(
        args: PaymentSheetContract.Args = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY,
        fragmentConfig: FragmentConfig? = FragmentConfigFixtures.DEFAULT,
        stripeIntent: StripeIntent? = PaymentIntentFixtures.PI_WITH_SHIPPING,
        onReady: (PaymentSheetAddPaymentMethodFragment, FragmentPaymentsheetAddPaymentMethodBinding) -> Unit
    ) = launchFragmentInContainer<PaymentSheetAddPaymentMethodFragment>(
        bundleOf(
            PaymentSheetActivity.EXTRA_FRAGMENT_CONFIG to fragmentConfig,
            PaymentSheetActivity.EXTRA_STARTER_ARGS to args
        ),
        R.style.StripePaymentSheetDefaultTheme,
        factory = PaymentSheetFragmentFactory(eventReporter),
        initialState = Lifecycle.State.INITIALIZED
    ).onFragment { fragment ->
        // Mock sheetViewModel loading the StripeIntent before the Fragment is created
        fragment.sheetViewModel.setStripeIntent(stripeIntent)
    }.moveToState(Lifecycle.State.STARTED)
        .onFragment { fragment ->
            onReady(
                fragment,
                FragmentPaymentsheetAddPaymentMethodBinding.bind(
                    requireNotNull(fragment.view)
                )
            )
        }
}
