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
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.databinding.FragmentPaymentsheetAddPaymentMethodBinding
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.FragmentConfigFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
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
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class PaymentSheetAddPaymentMethodFragmentLpmTest : PaymentSheetViewModelTestInjection() {
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
                mapOf(
                    "type" to "card",
                    "card" to mapOf(
                        "number" to null,
                        "exp_month" to null,
                        "exp_year" to null,
                        "cvc" to null,
                    )
                ),
                SupportedPaymentMethod.Card
            )
        assertThat(selection?.paymentMethodCreateParams?.toParamMap()).isEqualTo(
            mapOf(
                "type" to "card",

            )
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
    fun `test bancontact`() {
        createFragment(
            paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        ) { fragment, viewBinding, _ ->
        }
    }

    @Test
    fun `payment method selection has the fields from formFieldValues`() {
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
