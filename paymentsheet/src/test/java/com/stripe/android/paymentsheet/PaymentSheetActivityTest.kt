package com.stripe.android.paymentsheet

import android.animation.LayoutTransition
import android.content.Context
import android.os.Build
import android.view.Gravity
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContractV2
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.LinkButtonTestTag
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.payments.paymentlauncher.PaymentLauncherFactory
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncher
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.PaymentSheetViewModel.CheckoutIdentifier
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.databinding.StripeActivityPaymentSheetBinding
import com.stripe.android.paymentsheet.databinding.StripePrimaryButtonBinding
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddAnotherPaymentMethod
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.SelectSavedPaymentMethods
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.ui.GooglePayButton
import com.stripe.android.paymentsheet.ui.PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.PrimaryButtonAnimator
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.utils.FakeCustomerRepository
import com.stripe.android.utils.FakeIntentConfirmationInterceptor
import com.stripe.android.utils.FakePaymentSheetLoader
import com.stripe.android.utils.InjectableActivityScenario
import com.stripe.android.utils.TestUtils.idleLooper
import com.stripe.android.utils.TestUtils.viewModelFactoryFor
import com.stripe.android.utils.formViewModelSubcomponentBuilder
import com.stripe.android.utils.injectableActivityScenario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import com.stripe.android.ui.core.R as StripeUiCoreR

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class PaymentSheetActivityTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @get:Rule
    val composeTestRule = createAndroidComposeRule<PaymentSheetActivity>()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val eventReporter = mock<EventReporter>()
    private val googlePayPaymentMethodLauncherFactory =
        createGooglePayPaymentMethodLauncherFactory()

    private val paymentLauncherFactory = PaymentLauncherFactory(
        context = context,
        hostActivityLauncher = mock(),
        statusBarColor = null,
    )

    private val paymentLauncher: StripePaymentLauncher by lazy {
        paymentLauncherFactory.create(
            publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
        ) as StripePaymentLauncher
    }

    private val stripePaymentLauncherAssistedFactory = mock<StripePaymentLauncherAssistedFactory> {
        on { create(any(), any(), any(), any()) } doReturn paymentLauncher
    }

    private val fakeIntentConfirmationInterceptor = FakeIntentConfirmationInterceptor()

    private val contract = PaymentSheetContractV2()

    private val intent = contract.createIntent(
        context,
        PaymentSheetContractV2.Args(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = "pi_1234_secret_5678",
            ),
            config = PaymentSheetFixtures.CONFIG_CUSTOMER,
            statusBarColor = PaymentSheetFixtures.STATUS_BAR_COLOR
        )
    )

    @BeforeTest
    fun before() {
        PaymentConfiguration.init(
            context,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @AfterTest
    fun cleanup() {
        WeakMapInjectorRegistry.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `bottom sheet expands on start and handles click outside`() {
        val scenario = activityScenario()
        scenario.launchForResult(intent).onActivity { activity ->
            assertThat(activity.bottomSheetBehavior.state)
                .isEqualTo(STATE_EXPANDED)
            assertThat(activity.bottomSheetBehavior.isFitToContents)
                .isFalse()

            activity.viewBinding.root.performClick()
            idleLooper()

            assertThat(activity.bottomSheetBehavior.state)
                .isEqualTo(BottomSheetBehavior.STATE_HIDDEN)
        }

        assertThat(
            contract.parseResult(
                scenario.getResult().resultCode,
                scenario.getResult().resultData
            )
        ).isEqualTo(
            PaymentSheetResult.Canceled
        )
    }

    private val StripeActivityPaymentSheetBinding.buyButton: PrimaryButton
        get() = root.findViewById(R.id.primary_button)

    @Test
    fun `disables primary button when editing`() {
        val viewModel = createViewModel(
            initialPaymentSelection = PaymentSelection.Saved(PAYMENT_METHODS.last()),
        )

        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity { activity ->
            assertThat(activity.viewBinding.buyButton.isEnabled).isTrue()

            viewModel.toggleEditing()
            assertThat(activity.viewBinding.buyButton.isEnabled).isFalse()
        }
    }

    @Test
    fun `link button should not be enabled when editing`() {
        val viewModel = createViewModel(isLinkAvailable = true)
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity {
            composeTestRule
                .onNodeWithTag(LinkButtonTestTag)
                .assertIsEnabled()

            viewModel.toggleEditing()

            composeTestRule
                .onNodeWithTag(LinkButtonTestTag)
                .assertIsNotEnabled()
        }
    }

    @Test
    fun `link button should not be enabled when processing`() {
        val viewModel = createViewModel(isLinkAvailable = true)
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity { activity ->
            composeTestRule
                .onNodeWithTag(LinkButtonTestTag)
                .assertIsEnabled()

            activity.viewBinding.buyButton.callOnClick()

            composeTestRule
                .onNodeWithTag(LinkButtonTestTag)
                .assertIsNotEnabled()
        }
    }

    @Test
    fun `Errors are cleared when checking out with a generic payment method`() {
        val viewModel = createViewModel()
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity { activity ->
            val error = "some error"

            composeTestRule
                .onNodeWithText(error)
                .assertDoesNotExist()

            viewModel.onError(error)

            composeTestRule
                .onNodeWithText(error)
                .assertExists()

            activity.viewBinding.buyButton.callOnClick()

            composeTestRule
                .onNodeWithText(error)
                .assertDoesNotExist()
        }
    }

    @Ignore("Re-enable once we have refactored the Google Pay button handling")
    @Test
    fun `Errors are cleared when checking out with Google Pay`() {
        val viewModel = createViewModel(isGooglePayAvailable = true)
        val scenario = activityScenario(viewModel = viewModel)

        scenario.launch(intent).onActivity {
            val error = "some error"

            composeTestRule
                .onNodeWithText(error)
                .assertDoesNotExist()

            viewModel.onError(error)

            composeTestRule
                .onNodeWithText(error)
                .assertExists()

            composeTestRule
                .onNodeWithTag(GooglePayButton.TEST_TAG)
                .performClick()

            composeTestRule
                .onNodeWithText(error)
                .assertDoesNotExist()
        }
    }

    @Test
    fun `Errors are cleared when checking out with Link`() {
        val viewModel = createViewModel(isLinkAvailable = true)
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity {
            val error = "some error"
            composeTestRule
                .onNodeWithText(error)
                .assertDoesNotExist()

            viewModel.onError(error)

            composeTestRule
                .onNodeWithText(error)
                .assertExists()

            composeTestRule
                .onNodeWithTag(LinkButtonTestTag)
                .performClick()

            composeTestRule
                .onNodeWithText(error)
                .assertDoesNotExist()
        }
    }

    @Test
    fun `Errors are cleared when updating the payment selection`() {
        val paymentMethods = PAYMENT_METHODS

        val viewModel = createViewModel(
            paymentMethods = paymentMethods,
            isGooglePayAvailable = true,
        )

        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity {
            val error = "some error"

            composeTestRule
                .onNodeWithText(error)
                .assertDoesNotExist()

            viewModel.onError(error)

            composeTestRule
                .onNodeWithText(error)
                .assertExists()

            val newSelection = PaymentSelection.Saved(paymentMethod = paymentMethods.last())
            viewModel.updateSelection(newSelection)

            composeTestRule
                .onNodeWithTag(GooglePayButton.TEST_TAG)
                .performClick()

            viewModel.onGooglePayResult(GooglePayPaymentMethodLauncher.Result.Canceled)

            composeTestRule
                .onNodeWithText(error)
                .assertDoesNotExist()
        }
    }

    @Test
    fun `Errors are cleared when navigating back from payment form to saved payment methods`() {
        val paymentMethods = PAYMENT_METHODS
        val viewModel = createViewModel(paymentMethods = paymentMethods)
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity {
            val error = "some error"

            composeTestRule
                .onNodeWithText(error)
                .assertDoesNotExist()

            viewModel.transitionToAddPaymentScreen()
            composeTestRule.waitForIdle()

            viewModel.onError(error)

            composeTestRule
                .onNodeWithText(error)
                .assertExists()

            pressBack()

            composeTestRule
                .onNodeWithText(error)
                .assertDoesNotExist()
        }
    }

    @Test
    fun `Errors are cleared when transitioning to new screen`() {
        val paymentMethods = PAYMENT_METHODS
        val viewModel = createViewModel(paymentMethods = paymentMethods)
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity {
            val error = "some error"

            composeTestRule
                .onNodeWithText(error)
                .assertDoesNotExist()

            viewModel.onError(error)

            composeTestRule
                .onNodeWithText(error)
                .assertExists()

            viewModel.transitionToAddPaymentScreen()

            composeTestRule
                .onNodeWithText(error)
                .assertDoesNotExist()
        }
    }

    @Test
    fun `updates buy button state on add payment`() {
        Dispatchers.setMain(testDispatcher)

        val viewModel = createViewModel(paymentMethods = emptyList())
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity { activity ->
            // Initially empty card
            assertThat(activity.viewBinding.buyButton.isVisible).isTrue()
            assertThat(activity.viewBinding.buyButton.isEnabled).isFalse()

            // Update to Google Pay
            viewModel.checkoutWithGooglePay()
            assertThat(activity.viewBinding.buyButton.isVisible).isTrue()
            assertThat(activity.viewBinding.buyButton.isEnabled).isFalse()
            assertThat(viewModel.contentVisible.value).isFalse()

            viewModel.onGooglePayResult(GooglePayPaymentMethodLauncher.Result.Canceled)
            assertThat(viewModel.contentVisible.value).isTrue()

            // Update to saved card
            viewModel.updateSelection(
                PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            )
            assertThat(activity.viewBinding.buyButton.isVisible).isTrue()
            assertThat(activity.viewBinding.buyButton.isEnabled).isTrue()

            // Back to empty/invalid card
            viewModel.updateSelection(null)
            assertThat(activity.viewBinding.buyButton.isVisible).isTrue()
            assertThat(activity.viewBinding.buyButton.isEnabled).isFalse()

            // New valid card
            viewModel.updateSelection(
                PaymentSelection.New.Card(
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    CardBrand.Visa,
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse
                )
            )
            assertThat(activity.viewBinding.buyButton.isVisible).isTrue()
            assertThat(activity.viewBinding.buyButton.isEnabled).isTrue()
        }
    }

    @Test
    fun `Expands bottom sheet on first screen transition`() {
        activityScenario().launchForResult(intent).onActivity { activity ->
            assertThat(activity.bottomSheetBehavior.state).isEqualTo(STATE_EXPANDED)
        }
    }

    @Test
    fun `Reports canceled result when exiting sheet via back press`() {
        val scenario = activityScenario()

        scenario.launchForResult(intent).onActivity {
            pressBack()
        }

        assertThat(
            contract.parseResult(
                scenario.getResult().resultCode,
                scenario.getResult().resultData
            )
        ).isEqualTo(
            PaymentSheetResult.Canceled
        )
    }

    @Test
    fun `handles screen transitions correctly`() = runTest {
        val viewModel = createViewModel()
        val scenario = activityScenario(viewModel)

        viewModel.currentScreen.test {
            scenario.launch(intent)
            assertThat(awaitItem()).isEqualTo(SelectSavedPaymentMethods)

            viewModel.transitionToAddPaymentScreen()
            assertThat(awaitItem()).isEqualTo(AddAnotherPaymentMethod)

            pressBack()
            assertThat(awaitItem()).isEqualTo(SelectSavedPaymentMethods)

            pressBack()
            expectNoEvents()
        }
    }

    @Test
    fun `handles buy button clicks`() {
        val viewModel = createViewModel()
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity { activity ->
            viewModel.updateSelection(
                PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            )
            assertThat(activity.viewBinding.buyButton.isEnabled)
                .isTrue()

            activity.viewBinding.buyButton.performClick()

            assertThat(activity.viewBinding.buyButton.isEnabled)
                .isFalse()
        }
    }

    @Test
    fun `Verify Ready state updates the buy button label`() {
        val viewModel = createViewModel()
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity { activity ->
            viewModel.viewState.value = PaymentSheetViewState.Reset(null)

            val buyBinding = StripePrimaryButtonBinding.bind(activity.viewBinding.buyButton)

            assertThat(buyBinding.confirmedIcon.isVisible)
                .isFalse()

            activity.finish()
        }
    }

    @Test
    fun `Verify StartProcessing state updates the buy button label`() {
        val viewModel = createViewModel()
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity { activity ->
            viewModel.checkoutIdentifier = CheckoutIdentifier.SheetBottomBuy
            viewModel.viewState.value = PaymentSheetViewState.StartProcessing

            assertThat(activity.viewBinding.buyButton.externalLabel)
                .isEqualTo(activity.getString(R.string.stripe_paymentsheet_primary_button_processing))
        }
    }

    @Test
    fun `Verify FinishProcessing state calls the callback`() {
        Dispatchers.setMain(testDispatcher)
        val viewModel = createViewModel()
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity {
            viewModel.checkoutIdentifier = CheckoutIdentifier.SheetBottomBuy

            var finishProcessingCalled = false
            viewModel.viewState.value = PaymentSheetViewState.FinishProcessing {
                finishProcessingCalled = true
            }

            idleLooper()

            testDispatcher.scheduler.apply {
                advanceTimeBy(PrimaryButtonAnimator.HOLD_ANIMATION_ON_SLIDE_IN_COMPLETION)
                runCurrent()
            }

            assertThat(finishProcessingCalled).isTrue()
        }
    }

    @Test
    fun `Verify FinishProcessing state calls the callback on google pay view state observer`() {
        Dispatchers.setMain(testDispatcher)

        val viewModel = createViewModel(isGooglePayAvailable = true)
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity {
            viewModel.checkoutIdentifier = CheckoutIdentifier.SheetTopGooglePay

            var finishProcessingCalled = false
            viewModel.viewState.value = PaymentSheetViewState.FinishProcessing {
                finishProcessingCalled = true
            }

            composeTestRule.waitForIdle()

            testDispatcher.scheduler.apply {
                advanceTimeBy(PrimaryButtonAnimator.HOLD_ANIMATION_ON_SLIDE_IN_COMPLETION)
                runCurrent()
            }

            assertThat(finishProcessingCalled).isTrue()
        }
    }

    @Ignore("Re-enable once we have refactored the Google Pay button handling")
    @Test
    fun `google pay flow updates the scroll view before and after`() {
        val viewModel = createViewModel(isGooglePayAvailable = true)
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity {
            viewModel.checkoutIdentifier = CheckoutIdentifier.SheetTopGooglePay

            composeTestRule
                .onNodeWithTag(GooglePayButton.TEST_TAG)
                .performClick()

            composeTestRule.waitForIdle()

            assertThat(viewModel.contentVisible.value).isEqualTo(false)

            viewModel.onGooglePayResult(GooglePayPaymentMethodLauncher.Result.Canceled)
            assertThat(viewModel.contentVisible.value).isEqualTo(true)
        }
    }

    @Test
    fun `Verify ProcessResult state closes the sheet`() {
        Dispatchers.setMain(testDispatcher)

        val viewModel = createViewModel()
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity { activity ->
            viewModel.onFinish()

            idleLooper()

            // wait animate time...
            testDispatcher.scheduler.apply {
                advanceTimeBy(PrimaryButtonAnimator.HOLD_ANIMATION_ON_SLIDE_IN_COMPLETION)
                runCurrent()
            }

            assertThat(activity.bottomSheetBehavior.state)
                .isEqualTo(BottomSheetBehavior.STATE_HIDDEN)
        }
    }

    @Test
    fun `successful payment should dismiss bottom sheet`() {
        Dispatchers.setMain(testDispatcher)

        val viewModel = createViewModel()
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity { activity ->
            viewModel.checkoutIdentifier = CheckoutIdentifier.SheetBottomBuy
            viewModel.onPaymentResult(PaymentResult.Completed)

            idleLooper()

            assertThat(activity.bottomSheetBehavior.state)
                .isEqualTo(BottomSheetBehavior.STATE_HIDDEN)
        }
    }

    @Test
    fun `if fetched PaymentIntent is confirmed then should return Completed result`() {
        val scenario = activityScenario(
            createViewModel(
                paymentIntent = PaymentIntentFixtures.PI_SUCCEEDED
            )
        )
        scenario.launchForResult(intent).onActivity { activity ->
            activity.finish()
        }

        assertThat(
            contract.parseResult(
                scenario.getResult().resultCode,
                scenario.getResult().resultData
            )
        ).isInstanceOf(
            PaymentSheetResult.Failed::class.java
        )
    }

    @Test
    fun `GPay button error message is displayed`() {
        val viewModel = createViewModel(isGooglePayAvailable = true)
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity {
            val errorMessage = "Error message"

            composeTestRule
                .onNodeWithText(errorMessage)
                .assertDoesNotExist()

            viewModel.checkoutIdentifier = CheckoutIdentifier.SheetTopGooglePay
            viewModel.viewState.value =
                PaymentSheetViewState.Reset(BaseSheetViewModel.UserErrorMessage(errorMessage))

            composeTestRule
                .onNodeWithText(errorMessage)
                .assertExists()
        }
    }

    @Test
    fun `when checkout starts then error message is cleared`() {
        val viewModel = createViewModel(isGooglePayAvailable = true)
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity {
            val errorMessage = "Error message"

            composeTestRule
                .onNodeWithText(errorMessage)
                .assertDoesNotExist()

            viewModel.viewState.value =
                PaymentSheetViewState.Reset(BaseSheetViewModel.UserErrorMessage(errorMessage))

            composeTestRule
                .onNodeWithText(errorMessage)
                .assertExists()

            viewModel.checkout()

            composeTestRule
                .onNodeWithText(errorMessage)
                .assertDoesNotExist()

            viewModel.checkoutIdentifier = CheckoutIdentifier.SheetTopGooglePay
            viewModel.viewState.value =
                PaymentSheetViewState.Reset(BaseSheetViewModel.UserErrorMessage(errorMessage))

            composeTestRule
                .onNodeWithText(errorMessage)
                .assertExists()

            viewModel.checkout()

            composeTestRule
                .onNodeWithText(errorMessage)
                .assertDoesNotExist()
        }
    }

    @Test
    fun `notes visibility is visible`() {
        val viewModel = createViewModel()
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity {
            val text = context.getString(StripeUiCoreR.string.stripe_paymentsheet_payment_method_us_bank_account)
            viewModel.updateBelowButtonText(text)

            composeTestRule
                .onNodeWithText(text)
                .assertIsDisplayed()
        }
    }

    @Test
    fun `notes visibility is gone`() {
        val viewModel = createViewModel()
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity {
            val text = "some text"

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

    @Test
    fun `verify animation is enabled for layout transition changes`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            assertThat(
                activity.viewBinding.bottomSheet.layoutTransition.isTransitionTypeEnabled(
                    LayoutTransition.CHANGING
                )
            ).isTrue()
        }
    }

    @Test
    fun `Handles missing arguments correctly`() {
        val scenario = ActivityScenario.launchActivityForResult(PaymentSheetActivity::class.java)

        val result = contract.parseResult(
            scenario.result.resultCode,
            scenario.result.resultData,
        )

        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        assertThat(result).isInstanceOf(PaymentSheetResult.Failed::class.java)
    }

    @Test
    fun `Handles invalid arguments correctly`() {
        val invalidCustomerConfig = PaymentSheet.CustomerConfiguration(
            id = "",
            ephemeralKeySecret = "",
        )

        val args = PaymentSheetContractV2.Args(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = "abc",
            ),
            config = PaymentSheet.Configuration(
                merchantDisplayName = "Some name",
                customer = invalidCustomerConfig,
            ),
            statusBarColor = null,
        )

        val intent = contract.createIntent(context, args)

        val scenario = ActivityScenario.launchActivityForResult<PaymentSheetActivity>(intent)

        val result = contract.parseResult(
            scenario.result.resultCode,
            scenario.result.resultData,
        )

        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        assertThat(result).isInstanceOf(PaymentSheetResult.Failed::class.java)
    }

    @Test
    fun `Handles invalid client secret correctly`() {
        val args = PaymentSheetContractV2.Args(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(clientSecret = ""),
            config = null,
            statusBarColor = null,
        )

        val intent = contract.createIntent(context, args)

        val scenario = ActivityScenario.launchActivityForResult<PaymentSheetActivity>(intent)

        val result = contract.parseResult(
            scenario.result.resultCode,
            scenario.result.resultData,
        )

        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        assertThat(result).isInstanceOf(PaymentSheetResult.Failed::class.java)
    }

    @Test
    fun `processing should enable after checkout`() {
        val viewModel = createViewModel()
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity {
            runTest {
                viewModel.processing.test {
                    assertThat(awaitItem()).isFalse()
                    viewModel.checkout()
                    assertThat(awaitItem()).isTrue()
                }
            }
        }
    }

    @Test
    fun `amount label should be built from stripe intent`() {
        val viewModel = createViewModel(
            paymentIntent = PAYMENT_INTENT.copy(
                amount = 9999,
                currency = "CAD",
            ),
            paymentMethods = emptyList(),
        )
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity { activity ->
            assertThat(activity.viewBinding.buyButton.externalLabel).isEqualTo("Pay CA\$99.99")
        }
    }

    @Test
    fun `amount label should be built from stripe intent when response is delayed`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            paymentIntent = PAYMENT_INTENT.copy(
                amount = 9999,
                currency = "CAD",
            ),
            paymentMethods = emptyList(),
            loadDelay = 200.milliseconds,
        )

        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity { activity ->
            testDispatcher.scheduler.advanceTimeBy(50)
            assertThat(activity.viewBinding.buyButton.externalLabel).isEqualTo("Pay")
            testDispatcher.scheduler.advanceTimeBy(250)
            assertThat(activity.viewBinding.buyButton.externalLabel).isEqualTo("Pay CA\$99.99")
        }
    }

    @Test
    @Config(qualifiers = "sw800dp-w1250dp-h800dp")
    fun `tablet launches payment sheet centered horizontally`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity { activity ->
            assertThat(activity.bottomSheetBehavior.state)
                .isEqualTo(STATE_EXPANDED)
            assertThat(activity.bottomSheetBehavior.isFitToContents)
                .isFalse()
            idleLooper()
            val bottomSheet = activity.findViewById<ViewGroup>(R.id.bottom_sheet)
            val layoutParams = bottomSheet.layoutParams as CoordinatorLayout.LayoutParams
            assertThat(layoutParams.gravity).isEqualTo(Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM)
            assertThat(layoutParams.width).isEqualTo(750)
        }
    }

    @Test
    fun `Send confirm pressed event when pressing primary button`() = runTest(testDispatcher) {
        // Use only payment method type that doesn't require form input
        val paymentIntent = PAYMENT_INTENT.copy(
            amount = 9999,
            currency = "CAD",
            paymentMethodTypes = listOf("cashapp"),
        )

        val viewModel = createViewModel(
            paymentIntent = paymentIntent,
            paymentMethods = emptyList(),
        )

        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity {
            composeTestRule
                .onNodeWithTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG)
                .performClick()

            composeTestRule.waitForIdle()
        }

        verify(eventReporter).onPressConfirmButton(
            currency = "CAD",
            isDecoupling = false,
        )
    }

    private fun activityScenario(
        viewModel: PaymentSheetViewModel = createViewModel(),
    ): InjectableActivityScenario<PaymentSheetActivity> {
        return injectableActivityScenario {
            injectActivity {
                viewModelFactory = viewModelFactoryFor(viewModel)
            }
        }
    }

    private fun createViewModel(
        paymentIntent: PaymentIntent = PAYMENT_INTENT,
        paymentMethods: List<PaymentMethod> = PAYMENT_METHODS,
        loadDelay: Duration = Duration.ZERO,
        isGooglePayAvailable: Boolean = false,
        isLinkAvailable: Boolean = false,
        initialPaymentSelection: PaymentSelection? = null,
    ): PaymentSheetViewModel = runBlocking {
        val lpmRepository = LpmRepository(
            arguments = LpmRepository.LpmRepositoryArguments(resources = context.resources),
            lpmInitialFormData = LpmRepository.LpmInitialFormData(),
        ).apply {
            update(
                stripeIntent = paymentIntent,
                serverLpmSpecs = null,
            )
        }

        TestViewModelFactory.create(
            linkConfigurationCoordinator = mock<LinkConfigurationCoordinator>().stub {
                onBlocking { getAccountStatusFlow(any()) }.thenReturn(flowOf(AccountStatus.SignedOut))
                on { emailFlow } doReturn flowOf("email@email.com")
            },
        ) { linkHandler, linkInteractor, savedStateHandle ->
            PaymentSheetViewModel(
                ApplicationProvider.getApplicationContext(),
                PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY,
                eventReporter,
                { PaymentConfiguration(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY) },
                FakePaymentSheetLoader(
                    stripeIntent = paymentIntent,
                    customerPaymentMethods = paymentMethods,
                    isGooglePayAvailable = isGooglePayAvailable,
                    linkState = LinkState(
                        configuration = mock(),
                        loginState = LinkState.LoginState.LoggedOut,
                    ).takeIf { isLinkAvailable },
                    delay = loadDelay,
                    paymentSelection = initialPaymentSelection,
                ),
                FakeCustomerRepository(paymentMethods),
                FakePrefsRepository(),
                lpmRepository,
                stripePaymentLauncherAssistedFactory,
                googlePayPaymentMethodLauncherFactory,
                Logger.noop(),
                testDispatcher,
                savedStateHandle = savedStateHandle,
                linkHandler = linkHandler,
                linkConfigurationCoordinator = linkInteractor,
                intentConfirmationInterceptor = fakeIntentConfirmationInterceptor,
                formViewModelSubComponentBuilderProvider = formViewModelSubcomponentBuilder(
                    context = ApplicationProvider.getApplicationContext(),
                    lpmRepository = lpmRepository,
                ),
            )
        }
    }

    private fun createGooglePayPaymentMethodLauncherFactory() =
        object : GooglePayPaymentMethodLauncherFactory {
            override fun create(
                lifecycleScope: CoroutineScope,
                config: GooglePayPaymentMethodLauncher.Config,
                readyCallback: GooglePayPaymentMethodLauncher.ReadyCallback,
                activityResultLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>,
                skipReadyCheck: Boolean
            ): GooglePayPaymentMethodLauncher {
                val googlePayPaymentMethodLauncher = mock<GooglePayPaymentMethodLauncher>()
                readyCallback.onReady(true)
                return googlePayPaymentMethodLauncher
            }
        }

    private companion object {
        private val PAYMENT_INTENT = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        private val PAYMENT_METHODS = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
    }
}
