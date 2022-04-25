package com.stripe.android.paymentsheet

import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentIntentFixtures.PI_OFF_SESSION
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheetFixtures.COMPOSE_FRAGMENT_ARGS
import com.stripe.android.paymentsheet.databinding.FragmentPaymentsheetAddPaymentMethodBinding
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.FragmentConfigFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import com.stripe.android.paymentsheet.paymentdatacollection.ComposeFormDataCollectionFragment
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.forms.FormFieldEntry
import com.stripe.android.utils.TestUtils.idleLooper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
    @Config(qualifiers = "w320dp")
    fun `when screen is 320dp wide, adapter should show 2 and a half items`() {
        val paymentIntent = mock<PaymentIntent>().also {
            whenever(it.paymentMethodTypes).thenReturn(listOf("card", "bancontact", "sofort", "ideal"))
        }
        createFragment(stripeIntent = paymentIntent) { fragment, viewBinding, _ ->
            assertThat(
                calculateViewWidth(
                    convertPixelsToDp(
                        viewBinding.paymentMethodFragmentContainer.measuredWidth,
                        fragment.resources
                    ),
                    paymentIntent.paymentMethodTypes.size
                )
            ).isEqualTo(143.0.dp)
        }
    }

    @Test
    @Config(qualifiers = "w475dp")
    fun `when screen is 475dp wide, adapter should show 2 items evenly spread out`() {
        val paymentIntent = mock<PaymentIntent>().also {
            whenever(it.paymentMethodTypes).thenReturn(listOf("card", "bancontact"))
        }
        createFragment(stripeIntent = paymentIntent) { fragment, viewBinding, _ ->
            assertThat(
                calculateViewWidth(
                    convertPixelsToDp(
                        viewBinding.paymentMethodFragmentContainer.measuredWidth,
                        fragment.resources
                    ),
                    paymentIntent.paymentMethodTypes.size
                )
            ).isEqualTo(220.5.dp)
        }
    }

    fun convertPixelsToDp(px: Int, resources: Resources): Dp {
        return (px / (resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)).dp
    }

    @Test
    fun `started fragment should report onShowNewPaymentOptionForm() event`() {
        createFragment { _, _, _ ->
            idleLooper()
            verify(eventReporter).onShowNewPaymentOptionForm()
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
            ).isInstanceOf(ComposeFormDataCollectionFragment::class.java)

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
                        injectorKey = "testInjectorKeyAddFragmentTest",
                        initialPaymentMethodCreateParams = null
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
    fun `getFormArgumentTests with newLpm set and not set`(){
        assertTrue(false)
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
            ).isInstanceOf(ComposeFormDataCollectionFragment::class.java)

            fragment.onPaymentMethodSelected(SupportedPaymentMethod.Card)

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

            assertThat(addedFragment).isInstanceOf(ComposeFormDataCollectionFragment::class.java)

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
            ).isInstanceOf(ComposeFormDataCollectionFragment::class.java)

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
            assertThat(paymentSelection).isInstanceOf(PaymentSelection.Saved::class.java)
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
            ).isInstanceOf(ComposeFormDataCollectionFragment::class.java)

            fragment.onPaymentMethodSelected(SupportedPaymentMethod.Card)

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
    fun `card payment method selection has the fields from formFieldValues`() {
        val formFieldValues = FormFieldValues(
            fieldValuePairs = mapOf(
                IdentifierSpec.SaveForFutureUse to FormFieldEntry("true", true),
                IdentifierSpec.CardNumber to FormFieldEntry("4242424242421234", true),
                IdentifierSpec.CardBrand to FormFieldEntry(CardBrand.Visa.code, true)
            ),
            showsMandate = false,
            userRequestedReuse = PaymentSelection.CustomerRequestedSave.RequestReuse
        )
        val selection =
            BaseAddPaymentMethodFragment.transformToPaymentSelection(
                formFieldValues,
                SupportedPaymentMethod.Card
            )
        assertThat(selection?.customerRequestedSave).isEqualTo(
            PaymentSelection.CustomerRequestedSave.RequestReuse
        )
        assertThat((selection as? PaymentSelection.New.Card)?.last4).isEqualTo(
            "1234"
        )
        assertThat((selection as? PaymentSelection.New.Card)?.brand).isEqualTo(
            CardBrand.Visa
        )
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
                SupportedPaymentMethod.Sofort
            )
        assertThat(selection?.customerRequestedSave).isEqualTo(
            PaymentSelection.CustomerRequestedSave.RequestReuse
        )
        assertThat((selection as? PaymentSelection.New.GenericPaymentMethod)?.labelResource).isEqualTo(
            R.string.stripe_paymentsheet_payment_method_sofort
        )
        assertThat((selection as? PaymentSelection.New.GenericPaymentMethod)?.iconResource).isEqualTo(
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
    fun `Factory gets initialized with fallback when no Injector is available`() =
        kotlinx.coroutines.test.runTest(UnconfinedTestDispatcher()) {
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
        ).moveToState(Lifecycle.State.CREATED).onFragment {
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
