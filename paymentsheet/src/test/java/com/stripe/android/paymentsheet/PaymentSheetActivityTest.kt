package com.stripe.android.paymentsheet

import android.animation.LayoutTransition
import android.content.Context
import android.os.Build
import android.view.Gravity
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
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
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContract
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.model.AccountStatus
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
import com.stripe.android.paymentsheet.databinding.ActivityPaymentSheetBinding
import com.stripe.android.paymentsheet.databinding.PrimaryButtonBinding
import com.stripe.android.paymentsheet.databinding.StripeGooglePayButtonBinding
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.forms.PaymentMethodRequirements
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.model.StripeIntentValidator
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddAnotherPaymentMethod
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.SelectSavedPaymentMethods
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.repositories.StripeIntentRepository
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.PrimaryButtonAnimator
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SaveForFutureUseSpec
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.forms.resources.StaticAddressResourceRepository
import com.stripe.android.ui.core.forms.resources.StaticLpmResourceRepository
import com.stripe.android.uicore.address.AddressRepository
import com.stripe.android.utils.FakeCustomerRepository
import com.stripe.android.utils.FakePaymentSheetLoader
import com.stripe.android.utils.InjectableActivityScenario
import com.stripe.android.utils.TestUtils.idleLooper
import com.stripe.android.utils.TestUtils.viewModelFactoryFor
import com.stripe.android.utils.injectableActivityScenario
import com.stripe.android.view.ActivityScenarioFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import javax.inject.Provider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class PaymentSheetActivityTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @get:Rule
    val composeTestRule = createAndroidComposeRule<PaymentSheetActivity>()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var injector: NonFallbackInjector

    private val eventReporter = mock<EventReporter>()
    private val googlePayPaymentMethodLauncherFactory =
        createGooglePayPaymentMethodLauncherFactory()

    private val paymentLauncherFactory = PaymentLauncherFactory(
        context = context,
        hostActivityLauncher = mock(),
    )

    private val paymentLauncher: StripePaymentLauncher by lazy {
        paymentLauncherFactory.create(
            publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
        ) as StripePaymentLauncher
    }

    private val stripePaymentLauncherAssistedFactory = mock<StripePaymentLauncherAssistedFactory> {
        on { create(any(), any(), any()) } doReturn paymentLauncher
    }

    private lateinit var viewModel: PaymentSheetViewModel

    private val contract = PaymentSheetContract()

    private val intent = contract.createIntent(
        context,
        PaymentSheetContract.Args(
            PaymentIntentClientSecret("client_secret"),
            PaymentSheetFixtures.CONFIG_CUSTOMER,
            statusBarColor = PaymentSheetFixtures.STATUS_BAR_COLOR
        )
    )

    private val primaryButtonUIState = PrimaryButton.UIState(
        label = "Test",
        onClick = {},
        enabled = true,
        visible = true
    )

    @BeforeTest
    fun before() {
        viewModel = createViewModel()
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

    private val ActivityPaymentSheetBinding.buyButton: PrimaryButton
        get() = root.findViewById(R.id.primary_button)

    @Test
    fun `disables primary button when editing`() {
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity { activity ->
            assertThat(activity.viewBinding.buyButton.isEnabled)
                .isTrue()

            viewModel.toggleEditing()

            assertThat(activity.viewBinding.buyButton.isEnabled)
                .isFalse()
        }
    }

    @Test
    fun `google pay button should not be enabled when editing`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            assertThat(activity.viewBinding.googlePayButton.isEnabled).isTrue()

            viewModel.toggleEditing()

            assertThat(activity.viewBinding.googlePayButton.isEnabled).isFalse()
        }
    }

    @Test
    fun `google pay button should not be enabled when processing`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            assertThat(activity.viewBinding.googlePayButton.isEnabled).isTrue()

            activity.viewBinding.buyButton.callOnClick()

            assertThat(activity.viewBinding.googlePayButton.isEnabled).isFalse()
        }
    }

    @Test
    fun `link button should not be enabled when editing`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            assertThat(activity.viewBinding.linkButton.isEnabled).isTrue()

            viewModel.toggleEditing()

            assertThat(activity.viewBinding.linkButton.isEnabled).isFalse()
        }
    }

    @Test
    fun `link button should not be enabled when processing`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            assertThat(activity.viewBinding.linkButton.isEnabled).isTrue()

            activity.viewBinding.buyButton.callOnClick()

            assertThat(activity.viewBinding.linkButton.isEnabled).isFalse()
        }
    }

    @Test
    fun `Errors are cleared when checking out with a generic payment method`() {
        val scenario = activityScenario()

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

    @Test
    fun `Errors are cleared when checking out with Google Pay`() {
        val scenario = activityScenario()

        scenario.launch(intent).onActivity { activity ->
            val error = "some error"

            composeTestRule
                .onNodeWithText(error)
                .assertDoesNotExist()

            viewModel.onError(error)

            composeTestRule
                .onNodeWithText(error)
                .assertExists()

            activity.viewBinding.googlePayButton.callOnClick()

            composeTestRule
                .onNodeWithText(error)
                .assertDoesNotExist()
        }
    }

    @Test
    fun `Errors are cleared when checking out with Link`() {
        val scenario = activityScenario()

        scenario.launch(intent).onActivity { activity ->
            val error = "some error"
            composeTestRule
                .onNodeWithText(error)
                .assertDoesNotExist()

            viewModel.onError(error)

            composeTestRule
                .onNodeWithText(error)
                .assertExists()

            activity.viewBinding.linkButton.onClick(mock())

            composeTestRule
                .onNodeWithText(error)
                .assertDoesNotExist()
        }
    }

    @Test
    fun `Errors are cleared when updating the payment selection`() {
        val paymentMethods = PAYMENT_METHODS
        val viewModel = createViewModel(paymentMethods = paymentMethods)
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

            val newSelection = PaymentSelection.Saved(paymentMethod = paymentMethods.last())
            viewModel.updateSelection(newSelection)

            activity.viewBinding.googlePayButton.performClick()
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

        scenario.launch(intent).onActivity { activity ->
            val error = "some error"

            composeTestRule
                .onNodeWithText(error)
                .assertDoesNotExist()

            viewModel.transitionToAddPaymentScreen()

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

        scenario.launch(intent).onActivity { activity ->
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
        val scenario = activityScenario()
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
    fun `google pay button state updated on start processing`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            activity.viewBinding.googlePayButton.callOnClick()

            val googlePayButton =
                StripeGooglePayButtonBinding.bind(activity.viewBinding.googlePayButton)
            assertThat(googlePayButton.googlePayPrimaryButton.isVisible).isTrue()
            assertThat(googlePayButton.googlePayButtonContent.isVisible).isFalse()
            assertThat(googlePayButton.googlePayPrimaryButton.externalLabel)
                .isEqualTo(activity.getString(R.string.stripe_paymentsheet_primary_button_processing))
        }
    }

    @Test
    fun `google pay button state updated on finish processing`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            activity.viewBinding.googlePayButton.callOnClick()

            var finishProcessingCalled = false
            viewModel.viewState.value = PaymentSheetViewState.FinishProcessing {
                finishProcessingCalled = true
            }

            idleLooper()

            val googlePayButton =
                StripeGooglePayButtonBinding.bind(activity.viewBinding.googlePayButton)
            assertThat(googlePayButton.googlePayPrimaryButton.isVisible).isTrue()
            assertThat(googlePayButton.googlePayButtonContent.isVisible).isFalse()
            assertThat(finishProcessingCalled).isTrue()
        }
    }

    @Test
    fun `Verify Ready state updates the buy button label`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            viewModel.viewState.value = PaymentSheetViewState.Reset(null)

            val buyBinding = PrimaryButtonBinding.bind(activity.viewBinding.buyButton)

            assertThat(buyBinding.confirmedIcon.isVisible)
                .isFalse()

            activity.finish()
        }
    }

    @Test
    fun `Verify processing state disables toolbar and buttons`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            viewModel.checkout()

            assertThat(activity.viewBinding.googlePayButton.isEnabled).isFalse()
            assertThat(activity.viewBinding.buyButton.isEnabled).isFalse()
        }
    }

    @Test
    fun `Verify StartProcessing state updates the buy button label`() {
        val scenario = activityScenario()
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
        val scenario = activityScenario()
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
    fun `Verify StartProcessing state updates the google button label`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            viewModel.checkoutIdentifier = CheckoutIdentifier.SheetTopGooglePay
            viewModel.viewState.value = PaymentSheetViewState.StartProcessing

            val googlePayButton =
                StripeGooglePayButtonBinding.bind(activity.viewBinding.googlePayButton)
            assertThat(googlePayButton.googlePayPrimaryButton.externalLabel)
                .isEqualTo(activity.getString(R.string.stripe_paymentsheet_primary_button_processing))
        }
    }

    @Test
    fun `Verify FinishProcessing state calls the callback on google pay view state observer`() {
        Dispatchers.setMain(testDispatcher)
        val scenario = activityScenario()
        scenario.launch(intent).onActivity {
            viewModel.checkoutIdentifier = CheckoutIdentifier.SheetTopGooglePay

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
    fun `google pay flow updates the scroll view before and after`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            viewModel.checkoutIdentifier = CheckoutIdentifier.SheetTopGooglePay

            activity.viewBinding.googlePayButton.performClick()

            assertThat(viewModel.contentVisible.value).isEqualTo(false)

            viewModel.onGooglePayResult(GooglePayPaymentMethodLauncher.Result.Canceled)
            assertThat(viewModel.contentVisible.value).isEqualTo(true)
        }
    }

    @Test
    fun `Verify ProcessResult state closes the sheet`() {
        Dispatchers.setMain(testDispatcher)
        val scenario = activityScenario()
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
    fun `buyButton and googlePayButton are enabled when not processing, transition target, and a selection has been made`() {
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity { activity ->
            assertThat(activity.viewBinding.buyButton.isEnabled)
                .isTrue()
            assertThat(activity.viewBinding.googlePayButton.isEnabled)
                .isTrue()

            viewModel.updateSelection(
                PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            )

            assertThat(activity.viewBinding.buyButton.isEnabled)
                .isTrue()
            assertThat(activity.viewBinding.googlePayButton.isEnabled)
                .isTrue()
        }
    }

    @Test
    fun `sets expected statusBarColor`() {
        val activityScenarioFactory = ActivityScenarioFactory(context)
        val activityScenario = activityScenarioFactory.createAddPaymentMethodActivity()
        activityScenario.moveToState(Lifecycle.State.CREATED)
        activityScenario.onActivity { activity ->
            activity.window.statusBarColor = PaymentSheetFixtures.STATUS_BAR_COLOR

            val intent = contract.createIntent(
                activity,
                PaymentSheetContract.Args(
                    PaymentIntentClientSecret("client_secret"),
                    PaymentSheetFixtures.CONFIG_CUSTOMER
                )
            )

            val args =
                intent.extras?.get(PaymentSheetContract.EXTRA_ARGS) as PaymentSheetContract.Args
            assertThat(args.statusBarColor)
                .isEqualTo(PaymentSheetFixtures.STATUS_BAR_COLOR)
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
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity { activity ->
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
    fun `when new payment method is selected then error message is cleared`() {
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity { activity ->
            val errorMessage = "Error message"

            composeTestRule
                .onNodeWithText(errorMessage)
                .assertDoesNotExist()

            viewModel.viewState.value =
                PaymentSheetViewState.Reset(BaseSheetViewModel.UserErrorMessage(errorMessage))

            composeTestRule
                .onNodeWithText(errorMessage)
                .assertExists()

            viewModel.updateSelection(PaymentSelection.GooglePay)

            composeTestRule
                .onNodeWithText(errorMessage)
                .assertDoesNotExist()

            viewModel.checkoutIdentifier = CheckoutIdentifier.SheetTopGooglePay
            viewModel.viewState.value =
                PaymentSheetViewState.Reset(BaseSheetViewModel.UserErrorMessage(errorMessage))

            composeTestRule
                .onNodeWithText(errorMessage)
                .assertExists()

            activity.viewBinding.googlePayButton.performClick()
            viewModel.onGooglePayResult(GooglePayPaymentMethodLauncher.Result.Canceled)

            composeTestRule
                .onNodeWithText(errorMessage)
                .assertDoesNotExist()
        }
    }

    @Test
    fun `when checkout starts then error message is cleared`() {
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity { activity ->
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
    fun `Buy button should be enabled when primaryButtonEnabled is true`() {
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity { activity ->
            viewModel.updatePrimaryButtonUIState(
                primaryButtonUIState.copy(
                    enabled = true
                )
            )
            assertThat(activity.viewBinding.buyButton.isEnabled).isTrue()
        }
    }

    @Test
    fun `Buy button should be disabled when primaryButtonEnabled is false`() {
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity { activity ->
            viewModel.updatePrimaryButtonUIState(
                primaryButtonUIState.copy(
                    enabled = false
                )
            )
            assertThat(activity.viewBinding.buyButton.isEnabled).isFalse()
        }
    }

    @Test
    fun `Buy button text should update when primaryButtonText updates`() {
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity { activity ->
            viewModel.updatePrimaryButtonUIState(
                primaryButtonUIState.copy(
                    label = "Some text"
                )
            )
            assertThat(activity.viewBinding.buyButton.externalLabel).isEqualTo("Some text")
        }
    }

    @Test
    fun `Buy button should go back to initial state after resetPrimaryButton called`() {
        Dispatchers.setMain(testDispatcher)

        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity { activity ->
            viewModel.viewState.value = PaymentSheetViewState.Reset(null)

            viewModel.updatePrimaryButtonUIState(
                primaryButtonUIState.copy(
                    label = "Some text",
                    enabled = false
                )
            )
            assertThat(activity.viewBinding.buyButton.externalLabel).isEqualTo("Some text")
            assertThat(activity.viewBinding.buyButton.isEnabled).isFalse()

            viewModel.updatePrimaryButtonUIState(null)
            assertThat(activity.viewBinding.buyButton.externalLabel)
                .isEqualTo(viewModel.amount.value?.buildPayButtonLabel(context.resources))
            assertThat(activity.viewBinding.buyButton.isEnabled).isTrue()
        }
    }

    @Test
    fun `notes visibility is visible`() {
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity { activity ->
            viewModel.updateBelowButtonText(
                context.getString(
                    R.string.stripe_paymentsheet_payment_method_us_bank_account
                )
            )
            assertThat(activity.viewBinding.notes.isVisible).isTrue()
        }
    }

    @Test
    fun `notes visibility is gone`() {
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity { activity ->
            viewModel.updateBelowButtonText(null)
            assertThat(activity.viewBinding.notes.isVisible).isFalse()
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

            assertThat(
                activity.viewBinding.fragmentContainerParent.layoutTransition
                    .isTransitionTypeEnabled(
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

        val args = PaymentSheetContract.Args(
            clientSecret = PaymentIntentClientSecret("abc"),
            config = PaymentSheet.Configuration(
                merchantDisplayName = "Some name",
                customer = invalidCustomerConfig,
            ),
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
        val args = PaymentSheetContract.Args(
            clientSecret = PaymentIntentClientSecret(""),
            config = null,
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
            assertThat(activity.viewBinding.buyButton.externalLabel).isNull()
            testDispatcher.scheduler.advanceTimeBy(250)
            assertThat(activity.viewBinding.buyButton.externalLabel).isEqualTo("Pay CA\$99.99")
        }
    }

    @Test
    @Config(qualifiers = "sw800dp-w1250dp-h800dp")
    fun `tablet launches payment sheet centered horizontally`() = runTest(testDispatcher) {
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

    private fun activityScenario(
        viewModel: PaymentSheetViewModel = this.viewModel
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
    ): PaymentSheetViewModel = runBlocking {
        val lpmRepository = mock<LpmRepository>()
        whenever(lpmRepository.fromCode(any())).thenReturn(LpmRepository.HardcodedCard)
        whenever(lpmRepository.serverSpecLoadingState).thenReturn(LpmRepository.ServerSpecState.Uninitialized)

        val linkPaymentLauncher = mock<LinkPaymentLauncher>().stub {
            onBlocking { getAccountStatusFlow(any()) }.thenReturn(flowOf(AccountStatus.SignedOut))
        }

        registerFormViewModelInjector()

        TestViewModelFactory.create(
            linkLauncher = linkPaymentLauncher,
        ) { linkHandler, savedStateHandle ->
            PaymentSheetViewModel(
                ApplicationProvider.getApplicationContext(),
                PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY,
                eventReporter,
                { PaymentConfiguration(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY) },
                StripeIntentRepository.Static(paymentIntent),
                StripeIntentValidator(),
                FakePaymentSheetLoader(
                    stripeIntent = paymentIntent,
                    customerPaymentMethods = paymentMethods,
                    delay = loadDelay,
                ),
                FakeCustomerRepository(paymentMethods),
                FakePrefsRepository(),
                StaticLpmResourceRepository(lpmRepository),
                mock(),
                stripePaymentLauncherAssistedFactory,
                googlePayPaymentMethodLauncherFactory,
                Logger.noop(),
                testDispatcher,
                DUMMY_INJECTOR_KEY,
                savedStateHandle = savedStateHandle,
                linkHandler = linkHandler
            ).also {
                it.injector = injector
            }
        }
    }

    private fun createGooglePayPaymentMethodLauncherFactory() =
        object : GooglePayPaymentMethodLauncherFactory {
            override fun create(
                lifecycleScope: CoroutineScope,
                config: GooglePayPaymentMethodLauncher.Config,
                readyCallback: GooglePayPaymentMethodLauncher.ReadyCallback,
                activityResultLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContract.Args>,
                skipReadyCheck: Boolean
            ): GooglePayPaymentMethodLauncher {
                val googlePayPaymentMethodLauncher = mock<GooglePayPaymentMethodLauncher>()
                readyCallback.onReady(true)
                return googlePayPaymentMethodLauncher
            }
        }

    fun registerFormViewModelInjector() {
        val lpmRepository = mock<LpmRepository>().apply {
            whenever(fromCode(any())).thenReturn(
                LpmRepository.SupportedPaymentMethod(
                    PaymentMethod.Type.Card.code,
                    false,
                    com.stripe.android.ui.core.R.string.stripe_paymentsheet_payment_method_card,
                    com.stripe.android.ui.core.R.drawable.stripe_ic_paymentsheet_pm_card,
                    null,
                    null,
                    true,
                    PaymentMethodRequirements(emptySet(), emptySet(), true),
                    LayoutSpec.create(
                        EmailSpec(),
                        SaveForFutureUseSpec()
                    )
                )
            )
        }

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
            lpmResourceRepository = StaticLpmResourceRepository(lpmRepository),
            addressResourceRepository = StaticAddressResourceRepository(AddressRepository(context.resources)),
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
                (injectable as FormViewModel.Factory).subComponentBuilderProvider =
                    mockFormSubComponentBuilderProvider
            }
        }
    }

    private companion object {
        private val PAYMENT_INTENT = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        private val PAYMENT_METHODS = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
    }
}
