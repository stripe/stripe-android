package com.stripe.android.paymentsheet

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
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
import com.stripe.android.paymentsheet.databinding.ActivityPaymentOptionsBinding
import com.stripe.android.paymentsheet.databinding.PrimaryButtonBinding
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.forms.MandateRequirement
import com.stripe.android.paymentsheet.forms.PaymentMethodRequirements
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SaveForFutureUseSpec
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.uicore.address.AddressRepository
import com.stripe.android.utils.FakeCustomerRepository
import com.stripe.android.utils.InjectableActivityScenario
import com.stripe.android.utils.TestUtils.idleLooper
import com.stripe.android.utils.TestUtils.viewModelFactoryFor
import com.stripe.android.utils.injectableActivityScenario
import com.stripe.android.view.ActivityStarter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import javax.inject.Provider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class PaymentOptionsActivityTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val eventReporter = mock<EventReporter>()
    private lateinit var injector: NonFallbackInjector
    private val lpmRepository = mock<LpmRepository>().apply {
        whenever(fromCode(any())).thenReturn(
            LpmRepository.SupportedPaymentMethod(
                code = PaymentMethod.Type.Card.code,
                requiresMandate = false,
                mandateRequirement = MandateRequirement.Never,
                displayNameResource = com.stripe.android.ui.core.R.string.stripe_paymentsheet_payment_method_card,
                iconResource = com.stripe.android.ui.core.R.drawable.stripe_ic_paymentsheet_pm_card,
                lightThemeIconUrl = null,
                darkThemeIconUrl = null,
                tintIconOnSelection = true,
                requirement = PaymentMethodRequirements(emptySet(), emptySet(), true),
                formSpec = LayoutSpec.create(
                    EmailSpec(),
                    SaveForFutureUseSpec()
                )
            )
        )
        whenever(serverSpecLoadingState).thenReturn(LpmRepository.ServerSpecState.Uninitialized)
    }

    private val viewModel = createViewModel()

    private val ActivityPaymentOptionsBinding.continueButton: PrimaryButton
        get() = root.findViewById(R.id.primary_button)

    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
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
                PaymentOptionResult.Canceled(null, null, listOf())
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
                PaymentOptionResult.Canceled(null, PaymentSelection.GooglePay, listOf())
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

                assertThat(activity.viewBinding.continueButton.isVisible)
                    .isTrue()

                // Navigate back to payment options list
                pressBack()

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
    fun `Verify bottom sheet expands on start`() {
        val scenario = activityScenario()
        scenario.launch(
            createIntent()
        ).use {
            it.onActivity { activity ->
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
                val paymentSelection = PaymentSelection.GooglePay
                viewModel.updateSelection(paymentSelection)
                viewModel.onUserSelection()
                idleLooper()

                assertThat(activity.bottomSheetBehavior.state)
                    .isEqualTo(BottomSheetBehavior.STATE_HIDDEN)
            }
        }
    }

    @Test
    fun `notes visibility is set correctly`() {
        val scenario = activityScenario()
        scenario.launch(
            createIntent()
        ).use {
            it.onActivity {
                val text = "a below-button text"

                viewModel.updateBelowButtonText(text)

                composeTestRule
                    .onNodeWithText(text)
                    .assertIsDisplayed()

                viewModel.updateBelowButtonText(null)

                composeTestRule
                    .onNodeWithText(text)
                    .assertDoesNotExist()
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

    @Test
    fun `Clears error on user selection`() {
        val scenario = activityScenario()
        scenario.launch(createIntent()).onActivity { activity ->
            val text = "some error"
            viewModel.onError(text)

            composeTestRule
                .onNodeWithText(text)
                .assertIsDisplayed()

            viewModel.onUserSelection()

            composeTestRule
                .onNodeWithText(text)
                .assertDoesNotExist()
        }
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
        return TestViewModelFactory.create(
            linkLauncher = linkPaymentLauncher,
        ) { linkHandler, savedStateHandle ->
            registerFormViewModelInjector()
            PaymentOptionsViewModel(
                args = args,
                prefsRepositoryFactory = { FakePrefsRepository() },
                eventReporter = eventReporter,
                customerRepository = FakeCustomerRepository(),
                workContext = testDispatcher,
                application = ApplicationProvider.getApplicationContext(),
                logger = Logger.noop(),
                lpmRepository = lpmRepository,
                savedStateHandle = savedStateHandle,
                linkHandler = linkHandler,
            ).also {
                it.injector = injector
            }
        }
    }

    fun registerFormViewModelInjector() {
        val formViewModel = FormViewModel(
            context = context,
            formArguments = FormArguments(
                PaymentMethod.Type.Card.code,
                showCheckbox = true,
                showCheckboxControlledFields = true,
                merchantName = "Merchant, Inc.",
                amount = Amount(50, "USD"),
                initialPaymentMethodCreateParams = null
            ),
            lpmRepository = lpmRepository,
            addressRepository = AddressRepository(context.resources, Dispatchers.Unconfined),
            showCheckboxFlow = mock()
        )

        val mockFormBuilder = mock<FormViewModelSubcomponent.Builder>()
        val mockFormSubcomponent = mock<FormViewModelSubcomponent>()
        val mockFormSubComponentBuilderProvider =
            mock<Provider<FormViewModelSubcomponent.Builder>>()
        whenever(mockFormBuilder.build()).thenReturn(mockFormSubcomponent)
        whenever(mockFormBuilder.formArguments(any())).thenReturn(mockFormBuilder)
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
