package com.stripe.android.paymentsheet

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentSheetFixtures.PAYMENT_OPTIONS_CONTRACT_ARGS
import com.stripe.android.paymentsheet.PaymentSheetFixtures.updateState
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.databinding.PrimaryButtonBinding
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.forms.PaymentMethodRequirements
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel.TransitionTarget
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.address.AddressRepository
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SaveForFutureUseSpec
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.forms.resources.StaticAddressResourceRepository
import com.stripe.android.ui.core.forms.resources.StaticLpmResourceRepository
import com.stripe.android.utils.FakeCustomerRepository
import com.stripe.android.utils.InjectableActivityScenario
import com.stripe.android.utils.TestUtils.idleLooper
import com.stripe.android.utils.TestUtils.viewModelFactoryFor
import com.stripe.android.utils.injectableActivityScenario
import com.stripe.android.view.ActivityStarter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@ExperimentalCoroutinesApi
@FlowPreview
@RunWith(RobolectricTestRunner::class)
internal class PaymentOptionsActivityTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val eventReporter = mock<EventReporter>()
    private lateinit var injector: NonFallbackInjector
    private val addressRepository = AddressRepository(context.resources)
    private val lpmRepository = mock<LpmRepository>().apply {
        whenever(fromCode(any())).thenReturn(
            LpmRepository.SupportedPaymentMethod(
                PaymentMethod.Type.Card.code,
                false,
                com.stripe.android.ui.core.R.string.stripe_paymentsheet_payment_method_card,
                com.stripe.android.ui.core.R.drawable.stripe_ic_paymentsheet_pm_card,
                true,
                PaymentMethodRequirements(emptySet(), emptySet(), true),
                LayoutSpec.create(
                    EmailSpec(),
                    SaveForFutureUseSpec()
                )
            )
        )
    }

    private val viewModel = createViewModel()

    private val primaryButtonUIState = PrimaryButton.UIState(
        label = "Test",
        onClick = {},
        enabled = true,
        visible = true
    )

    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @AfterTest
    fun cleanup() {
        WeakMapInjectorRegistry.clear()
    }

    @Test
    fun `click outside of bottom sheet should return cancel result`() {
        val scenario = activityScenario()
        scenario.launchForResult(
            createIntent()
        ).use {
            it.onActivity { activity ->
                activity.viewBinding.root.performClick()
                activity.finish()
            }

            assertThat(
                PaymentOptionResult.fromIntent(scenario.getResult().resultData)
            ).isEqualTo(
                PaymentOptionResult.Canceled(null, listOf())
            )
        }
    }

    @Test
    fun `click outside of bottom sheet should return cancel result even if there is a selection`() {
        val scenario = activityScenario()
        scenario.launchForResult(
            createIntent()
        ).use {
            it.onActivity { activity ->
                viewModel.updateSelection(PaymentSelection.GooglePay)

                activity.viewBinding.root.performClick()
                activity.finish()
            }

            assertThat(
                PaymentOptionResult.fromIntent(scenario.getResult().resultData)
            ).isEqualTo(
                PaymentOptionResult.Canceled(null, listOf())
            )
        }
    }

    @Test
    fun `ContinueButton should be hidden when showing payment options`() {
        val args = PAYMENT_OPTIONS_CONTRACT_ARGS.updateState(
            paymentMethods = PaymentMethodFixtures.createCards(5)
        )

        val scenario = activityScenario(
            viewModel = createViewModel(args)
        )

        scenario.launch(createIntent(args)).use {
            it.onActivity { activity ->
                assertThat(activity.viewBinding.continueButton.isVisible)
                    .isFalse()
            }
        }
    }

    @Test
    fun `ContinueButton should be visible when showing add payment method form`() {
        val scenario = activityScenario(
            createViewModel(
                args = PAYMENT_OPTIONS_CONTRACT_ARGS.updateState(
                    stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK
                )
            )
        )
        scenario.launch(
            createIntent(
                args = PAYMENT_OPTIONS_CONTRACT_ARGS.updateState(
                    stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK
                )
            )
        ).use {
            it.onActivity { activity ->
                assertThat(activity.viewBinding.continueButton.isVisible)
                    .isTrue()
            }
        }
    }

    @Test
    fun `ContinueButton should be hidden when returning to payment options`() {
        val args = PAYMENT_OPTIONS_CONTRACT_ARGS.updateState(
            paymentMethods = PaymentMethodFixtures.createCards(5)
        )

        val viewModel = createViewModel(args)
        val scenario = activityScenario(viewModel)

        scenario.launch(createIntent(args)).use {
            it.onActivity { activity ->
                assertThat(activity.viewBinding.continueButton.isVisible)
                    .isFalse()

                // Navigate to "Add Payment Method" fragment
                viewModel.transitionToAddPaymentScreen()
                idleLooper()

                assertThat(activity.viewBinding.continueButton.isVisible)
                    .isTrue()

                // Navigate back to payment options list
                activity.onBackPressed()
                idleLooper()

                assertThat(activity.viewBinding.continueButton.isVisible)
                    .isFalse()
            }
        }
    }

    @Test
    fun `Verify Ready state updates the add button label`() {
        val scenario = activityScenario()
        scenario.launch(
            createIntent()
        ).use {
            it.onActivity { activity ->
                idleLooper()

                val addBinding = PrimaryButtonBinding.bind(activity.viewBinding.continueButton)

                assertThat(addBinding.confirmedIcon.isVisible)
                    .isFalse()

                assertThat(activity.viewBinding.continueButton.externalLabel)
                    .isEqualTo("Continue")

                activity.finish()
            }
        }
    }

    @Test
    fun `Verify if google pay is ready, stay on the select saved payment method`() {
        val args = PAYMENT_OPTIONS_CONTRACT_ARGS.updateState(isGooglePayReady = true)
        val viewModel = createViewModel(args)

        val transitionTarget = mutableListOf<BaseSheetViewModel.Event<TransitionTarget?>>()
        viewModel.transition.observeForever {
            transitionTarget.add(it)
        }
        val scenario = activityScenario(viewModel)
        scenario.launch(
            createIntent(args)
        ).use {
            idleLooper()
            assertThat(transitionTarget[1].peekContent())
                .isInstanceOf(TransitionTarget.SelectSavedPaymentMethod::class.java)
        }
    }

    @Test
    fun `Verify if payment methods is not empty select, saved payment method`() {
        val args = PAYMENT_OPTIONS_CONTRACT_ARGS.updateState(
            isGooglePayReady = false,
            paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )

        val viewModel = createViewModel(args)
        val transitionTarget =
            mutableListOf<BaseSheetViewModel.Event<TransitionTarget?>>()
        viewModel.transition.observeForever {
            transitionTarget.add(it)
        }

        val scenario = activityScenario(viewModel)
        scenario.launch(
            createIntent(args)
        ).use {
            idleLooper()
            assertThat(transitionTarget[1].peekContent())
                .isInstanceOf(TransitionTarget.SelectSavedPaymentMethod::class.java)
        }
    }

    @Test
    fun `Verify bottom sheet expands on start`() {
        val scenario = activityScenario()
        scenario.launch(
            createIntent()
        ).use {
            it.onActivity { activity ->
                idleLooper()

                assertThat(activity.bottomSheetBehavior.state)
                    .isEqualTo(BottomSheetBehavior.STATE_EXPANDED)
                assertThat(activity.bottomSheetBehavior.isFitToContents)
                    .isFalse()
            }
        }
    }

    @Test
    fun `Verify ProcessResult state closes the sheet`() {
        val scenario = activityScenario()
        scenario.launch(
            createIntent()
        ).use {
            it.onActivity { activity ->
                val paymentSelectionMock: PaymentSelection = PaymentSelection.GooglePay
                viewModel._paymentOptionResult.value = PaymentOptionResult.Succeeded(
                    paymentSelectionMock
                )
                idleLooper()

                assertThat(activity.bottomSheetBehavior.state)
                    .isEqualTo(BottomSheetBehavior.STATE_HIDDEN)
            }
        }
    }

    @Test
    fun `ContinueButton should be enabled when primaryButtonEnabled is true`() {
        val scenario = activityScenario()
        scenario.launch(
            createIntent()
        ).use {
            it.onActivity { activity ->
                viewModel.updatePrimaryButtonUIState(
                    primaryButtonUIState.copy(
                        enabled = true
                    )
                )
                assertThat(activity.viewBinding.continueButton.isEnabled).isTrue()
            }
        }
    }

    @Test
    fun `ContinueButton should be disabled when primaryButtonEnabled is false`() {
        val scenario = activityScenario()
        scenario.launch(
            createIntent()
        ).use {
            it.onActivity { activity ->
                viewModel.updatePrimaryButtonUIState(
                    primaryButtonUIState.copy(
                        enabled = false
                    )
                )
                assertThat(activity.viewBinding.continueButton.isEnabled).isFalse()
            }
        }
    }

    @Test
    fun `ContinueButton text should update when primaryButtonText updates`() {
        val scenario = activityScenario()
        scenario.launch(
            createIntent()
        ).use {
            it.onActivity { activity ->
                viewModel.updatePrimaryButtonUIState(
                    primaryButtonUIState.copy(
                        label = "Some text"
                    )
                )
                assertThat(activity.viewBinding.continueButton.externalLabel).isEqualTo("Some text")
            }
        }
    }

    @Test
    fun `ContinueButton should go back to initial state after updating selection`() {
        val scenario = activityScenario()
        scenario.launch(
            createIntent()
        ).use {
            it.onActivity { activity ->
                viewModel.updatePrimaryButtonUIState(
                    primaryButtonUIState.copy(
                        label = "Some text",
                        enabled = false
                    )
                )
                assertThat(activity.viewBinding.continueButton.externalLabel).isEqualTo("Some text")
                assertThat(activity.viewBinding.continueButton.isEnabled).isFalse()

                viewModel.updateSelection(mock<PaymentSelection.New.Card>())
                assertThat(activity.viewBinding.continueButton.externalLabel).isEqualTo("Continue")
                assertThat(activity.viewBinding.continueButton.isEnabled).isTrue()
            }
        }
    }

    @Test
    fun `notes visibility is visible`() {
        val scenario = activityScenario()
        scenario.launch(
            createIntent()
        ).use {
            it.onActivity { activity ->
                viewModel.updateBelowButtonText(
                    ApplicationProvider.getApplicationContext<Context>().getString(
                        com.stripe.android.paymentsheet.R.string.stripe_paymentsheet_payment_method_us_bank_account
                    )
                )
                assertThat(activity.viewBinding.notes.isVisible).isTrue()
            }
        }
    }

    @Test
    fun `notes visibility is gone`() {
        val scenario = activityScenario()
        scenario.launch(
            createIntent()
        ).use {
            idleLooper()
            it.onActivity { activity ->
                viewModel.updateBelowButtonText(null)
                assertThat(activity.viewBinding.notes.isVisible).isFalse()
            }
        }
    }

    @Test
    fun `primary button appearance is set`() {
        val scenario = activityScenario(
            createViewModel(
                args = PAYMENT_OPTIONS_CONTRACT_ARGS.updateState(
                    stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK
                )
            )
        )
        scenario.launch(
            createIntent(
                args = PAYMENT_OPTIONS_CONTRACT_ARGS.updateState(
                    stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
                    config = PaymentSheetFixtures.CONFIG_MINIMUM.copy(
                        appearance = PaymentSheet.Appearance(
                            primaryButton = PaymentSheet.PrimaryButton(
                                colorsLight = PaymentSheet.PrimaryButtonColors(
                                    background = Color.Magenta,
                                    onBackground = Color.Magenta,
                                    border = Color.Magenta
                                ),
                                shape = PaymentSheet.PrimaryButtonShape(),
                                typography = PaymentSheet.PrimaryButtonTypography()
                            )
                        )
                    )
                )
            )
        ).use {
            idleLooper()
            it.onActivity { activity ->
                assertThat(activity.viewBinding.continueButton.isVisible).isTrue()
                assertThat(activity.viewBinding.continueButton.defaultTintList).isEqualTo(
                    ColorStateList.valueOf(Color.Magenta.toArgb())
                )
            }
        }
    }

    @Test
    fun `Handles missing args correctly`() {
        val emptyIntent = Intent(context, PaymentOptionsActivity::class.java)
        val scenario = ActivityScenario.launchActivityForResult<PaymentOptionsActivity>(emptyIntent)
        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
    }

    private fun createIntent(
        args: PaymentOptionContract.Args = PAYMENT_OPTIONS_CONTRACT_ARGS
    ): Intent {
        return Intent(
            ApplicationProvider.getApplicationContext(),
            PaymentOptionsActivity::class.java
        ).putExtras(
            bundleOf(ActivityStarter.Args.EXTRA to args)
        )
    }

    private fun activityScenario(
        viewModel: PaymentOptionsViewModel = this.viewModel
    ): InjectableActivityScenario<PaymentOptionsActivity> {
        return injectableActivityScenario {
            injectActivity {
                viewModelFactory = viewModelFactoryFor(viewModel)
            }
        }
    }

    private fun createViewModel(
        args: PaymentOptionContract.Args = PAYMENT_OPTIONS_CONTRACT_ARGS
    ): PaymentOptionsViewModel {
        val linkPaymentLauncher = mock<LinkPaymentLauncher>().stub {
            onBlocking { getAccountStatusFlow(any()) }.thenReturn(flowOf(AccountStatus.SignedOut))
        }
        registerFormViewModelInjector()
        return PaymentOptionsViewModel(
            args = args,
            prefsRepositoryFactory = { FakePrefsRepository() },
            eventReporter = eventReporter,
            customerRepository = FakeCustomerRepository(),
            workContext = testDispatcher,
            application = ApplicationProvider.getApplicationContext(),
            logger = Logger.noop(),
            injectorKey = DUMMY_INJECTOR_KEY,
            lpmResourceRepository = StaticLpmResourceRepository(lpmRepository),
            addressResourceRepository = StaticAddressResourceRepository(addressRepository),
            savedStateHandle = SavedStateHandle(),
            linkLauncher = linkPaymentLauncher
        ).also {
            it.injector = injector
        }
    }

    fun registerFormViewModelInjector() {
        val formViewModel = FormViewModel(
            context = context,
            formFragmentArguments = FormFragmentArguments(
                PaymentMethod.Type.Card.code,
                showCheckbox = true,
                showCheckboxControlledFields = true,
                merchantName = "Merchant, Inc.",
                amount = Amount(50, "USD"),
                initialPaymentMethodCreateParams = null
            ),
            lpmResourceRepository = StaticLpmResourceRepository(lpmRepository),
            addressResourceRepository = StaticAddressResourceRepository(addressRepository),
            showCheckboxFlow = mock()
        )

        val mockFormBuilder = mock<FormViewModelSubcomponent.Builder>()
        val mockFormSubcomponent = mock<FormViewModelSubcomponent>()
        val mockFormSubComponentBuilderProvider =
            mock<Provider<FormViewModelSubcomponent.Builder>>()
        whenever(mockFormBuilder.build()).thenReturn(mockFormSubcomponent)
        whenever(mockFormBuilder.formFragmentArguments(any())).thenReturn(mockFormBuilder)
        whenever(mockFormBuilder.showCheckboxFlow(any())).thenReturn(mockFormBuilder)
        whenever(mockFormSubcomponent.viewModel).thenReturn(formViewModel)
        whenever(mockFormSubComponentBuilderProvider.get()).thenReturn(mockFormBuilder)

        injector = object : NonFallbackInjector {
            override fun inject(injectable: Injectable<*>) {
                (injectable as? FormViewModel.Factory)?.let {
                    injectable.subComponentBuilderProvider = mockFormSubComponentBuilderProvider
                }
            }
        }
    }
}
