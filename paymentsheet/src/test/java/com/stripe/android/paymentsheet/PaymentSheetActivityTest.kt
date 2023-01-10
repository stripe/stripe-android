package com.stripe.android.paymentsheet

import android.animation.LayoutTransition
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.android.material.bottomsheet.BottomSheetBehavior
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
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.payments.paymentlauncher.PaymentLauncherFactory
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncher
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.PaymentSheetViewModel.CheckoutIdentifier
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.databinding.PrimaryButtonBinding
import com.stripe.android.paymentsheet.databinding.StripeGooglePayButtonBinding
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.forms.PaymentMethodRequirements
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.model.StripeIntentValidator
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.paymentsheet.repositories.StripeIntentRepository
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.PrimaryButtonAnimator
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
import com.stripe.android.utils.FakePaymentSheetLoader
import com.stripe.android.utils.InjectableActivityScenario
import com.stripe.android.utils.TestUtils.getOrAwaitValue
import com.stripe.android.utils.TestUtils.idleLooper
import com.stripe.android.utils.TestUtils.observeEventsForever
import com.stripe.android.utils.TestUtils.viewModelFactoryFor
import com.stripe.android.utils.injectableActivityScenario
import com.stripe.android.view.ActivityScenarioFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
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
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@ExperimentalCoroutinesApi
@FlowPreview
@RunWith(RobolectricTestRunner::class)
internal class PaymentSheetActivityTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

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
            // wait for bottom sheet to animate in
            idleLooper()
            assertThat(activity.bottomSheetBehavior.state)
                .isEqualTo(BottomSheetBehavior.STATE_EXPANDED)
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

    @Test
    fun `disables primary button when editing`() {
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()

            assertThat(activity.viewBinding.buyButton.isEnabled)
                .isTrue()

            viewModel.setEditing(true)

            assertThat(activity.viewBinding.buyButton.isEnabled)
                .isFalse()
        }
    }

    @Test
    fun `Errors are cleared when checking out with a generic payment method`() {
        val scenario = activityScenario()

        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()
            assertThat(activity.viewBinding.message.isVisible).isFalse()

            viewModel.onError("some error")
            assertThat(activity.viewBinding.message.isVisible).isTrue()
            assertThat(activity.viewBinding.message.text.toString()).isEqualTo("some error")

            activity.viewBinding.buyButton.callOnClick()
            assertThat(activity.viewBinding.message.isVisible).isFalse()
        }
    }

    @Test
    fun `Errors are cleared when checking out with Google Pay`() {
        val scenario = activityScenario()

        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()
            assertThat(activity.viewBinding.message.isVisible).isFalse()

            viewModel.onError("some error")
            assertThat(activity.viewBinding.message.isVisible).isTrue()
            assertThat(activity.viewBinding.message.text.toString()).isEqualTo("some error")

            activity.viewBinding.googlePayButton.callOnClick()
            assertThat(activity.viewBinding.message.isVisible).isFalse()
        }
    }

    @Test
    fun `Errors are cleared when checking out with Link`() {
        val scenario = activityScenario()

        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()
            assertThat(activity.viewBinding.message.isVisible).isFalse()

            viewModel.onError("some error")
            assertThat(activity.viewBinding.message.isVisible).isTrue()
            assertThat(activity.viewBinding.message.text.toString()).isEqualTo("some error")

            val linkConfig = viewModel.linkConfiguration.getOrAwaitValue()
            activity.viewBinding.linkButton.onClick(linkConfig)
            assertThat(activity.viewBinding.message.isVisible).isFalse()
        }
    }

    @Test
    fun `Errors are cleared when updating the payment selection`() {
        val paymentMethods = PAYMENT_METHODS
        val viewModel = createViewModel(paymentMethods = paymentMethods)
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()
            assertThat(activity.viewBinding.message.isVisible).isFalse()

            viewModel.onError("some error")
            assertThat(activity.viewBinding.message.isVisible).isTrue()
            assertThat(activity.viewBinding.message.text.toString()).isEqualTo("some error")

            val newSelection = PaymentSelection.Saved(paymentMethod = paymentMethods.last())
            viewModel.updateSelection(newSelection)

            assertThat(activity.viewBinding.message.isVisible).isFalse()
        }
    }

    @Test
    fun `Errors are cleared when navigating back from payment form to saved payment methods`() {
        val paymentMethods = PAYMENT_METHODS
        val viewModel = createViewModel(paymentMethods = paymentMethods)
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()
            assertThat(activity.viewBinding.message.isVisible).isFalse()

            viewModel.transitionToAddPaymentScreen()
            idleLooper()

            viewModel.onError("some error")
            assertThat(activity.viewBinding.message.isVisible).isTrue()
            assertThat(activity.viewBinding.message.text.toString()).isEqualTo("some error")

            activity.onBackPressed()

            assertThat(activity.viewBinding.message.isVisible).isFalse()
        }
    }

    @Test
    fun `Errors are cleared when transitioning to new screen`() {
        val paymentMethods = PAYMENT_METHODS
        val viewModel = createViewModel(paymentMethods = paymentMethods)
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()
            assertThat(activity.viewBinding.message.isVisible).isFalse()

            viewModel.onError("some error")
            assertThat(activity.viewBinding.message.isVisible).isTrue()
            assertThat(activity.viewBinding.message.text.toString()).isEqualTo("some error")

            viewModel.transitionToAddPaymentScreen()
            idleLooper()
            assertThat(activity.viewBinding.message.isVisible).isFalse()
        }
    }

    @Test
    fun `updates buy button state on add payment`() {
        val viewModel = createViewModel(paymentMethods = emptyList())
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()

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
    fun `when back to Ready state should update PaymentSelection`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()

            // New valid card
            val initialSelection = PaymentSelection.New.Card(
                PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                CardBrand.Visa,
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse
            )
            viewModel.updateSelection(initialSelection)

            viewModel.transitionToAddPaymentScreen()

            assertThat(viewModel.selection.getOrAwaitValue()).isEqualTo(initialSelection)

            activity.viewBinding.googlePayButton.callOnClick()
            viewModel.onGooglePayResult(GooglePayPaymentMethodLauncher.Result.Canceled)
            assertThat(viewModel.contentVisible.value).isTrue()

            // Still using the initial PaymentSelection
            assertThat(viewModel.selection.getOrAwaitValue()).isEqualTo(initialSelection)
        }
    }

    @Test
    fun `handles fragment transitions`() {
        val scenario = activityScenario()
        scenario.launchForResult(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()

            assertThat(currentFragment(activity))
                .isInstanceOf(PaymentSheetListFragment::class.java)
            assertThat(activity.bottomSheetBehavior.state)
                .isEqualTo(BottomSheetBehavior.STATE_EXPANDED)

            viewModel.transitionToAddPaymentScreen()
            idleLooper()
            assertThat(currentFragment(activity))
                .isInstanceOf(PaymentSheetAddPaymentMethodFragment::class.java)
            assertThat(activity.bottomSheetBehavior.state)
                .isEqualTo(BottomSheetBehavior.STATE_EXPANDED)

            activity.onBackPressed()
            idleLooper()
            assertThat(currentFragment(activity))
                .isInstanceOf(PaymentSheetListFragment::class.java)
            assertThat(activity.bottomSheetBehavior.state)
                .isEqualTo(BottomSheetBehavior.STATE_EXPANDED)

            activity.onBackPressed()
            idleLooper()
            // animating out
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

    @Test
    fun `updates navigation button`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()

            assertThat(activity.toolbar.navigationContentDescription)
                .isEqualTo(context.getString(R.string.stripe_paymentsheet_close))

            viewModel.transitionToAddPaymentScreen()
            idleLooper()

            assertThat(activity.toolbar.navigationContentDescription)
                .isEqualTo(context.getString(R.string.back))

            activity.onBackPressed()
            idleLooper()

            assertThat(activity.toolbar.navigationContentDescription)
                .isEqualTo(context.getString(R.string.stripe_paymentsheet_close))

            activity.onBackPressed()
            idleLooper()
            // animating out
            assertThat(activity.bottomSheetBehavior.state)
                .isEqualTo(BottomSheetBehavior.STATE_HIDDEN)
        }
    }

    @Test
    fun `handles buy button clicks`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()

            viewModel.updateSelection(
                PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            )
            assertThat(activity.viewBinding.buyButton.isEnabled)
                .isTrue()

            activity.viewBinding.buyButton.performClick()
            idleLooper()

            assertThat(activity.viewBinding.buyButton.isEnabled)
                .isFalse()

            assertThat(viewModel.startConfirm.value?.peekContent())
                .isEqualTo(
                    ConfirmPaymentIntentParams.createWithPaymentMethodId(
                        paymentMethodId = "pm_123456789",
                        clientSecret = "client_secret",
                        paymentMethodOptions = PaymentMethodOptionsParams.Card(
                            setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.Blank
                        )
                    )
                )
        }
    }

    @Test
    fun `google pay button state updated on start processing`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()

            activity.viewBinding.googlePayButton.callOnClick()

            idleLooper()

            val googlePayButton =
                StripeGooglePayButtonBinding.bind(activity.viewBinding.googlePayButton)
            assertThat(googlePayButton.primaryButton.isVisible).isTrue()
            assertThat(googlePayButton.googlePayButtonContent.isVisible).isFalse()
            assertThat(googlePayButton.primaryButton.externalLabel)
                .isEqualTo(activity.getString(R.string.stripe_paymentsheet_primary_button_processing))
        }
    }

    @Test
    fun `google pay button state updated on finish processing`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()

            activity.viewBinding.googlePayButton.callOnClick()

            idleLooper()

            var finishProcessingCalled = false
            viewModel._viewState.value = PaymentSheetViewState.FinishProcessing {
                finishProcessingCalled = true
            }

            idleLooper()

            val googlePayButton =
                StripeGooglePayButtonBinding.bind(activity.viewBinding.googlePayButton)
            assertThat(googlePayButton.primaryButton.isVisible).isTrue()
            assertThat(googlePayButton.googlePayButtonContent.isVisible).isFalse()
            assertThat(finishProcessingCalled).isTrue()
        }
    }

    @Test
    fun `Verify Ready state updates the buy button label`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()

            viewModel._viewState.value = PaymentSheetViewState.Reset(null)

            idleLooper()

            val buyBinding = PrimaryButtonBinding.bind(activity.viewBinding.buyButton)

            assertThat(buyBinding.confirmedIcon.isVisible)
                .isFalse()

            idleLooper()

            activity.finish()
        }
    }

    @Test
    fun `Verify processing state disables toolbar and buttons`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()
            viewModel.checkout()
            idleLooper()

            assertThat(activity.toolbar.isEnabled).isFalse()
            assertThat(activity.viewBinding.googlePayButton.isEnabled).isFalse()
            assertThat(activity.viewBinding.buyButton.isEnabled).isFalse()
        }
    }

    @Test
    fun `Verify StartProcessing state updates the buy button label`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()

            viewModel.checkoutIdentifier = CheckoutIdentifier.SheetBottomBuy
            viewModel._viewState.value = PaymentSheetViewState.StartProcessing

            idleLooper()

            assertThat(activity.viewBinding.buyButton.externalLabel)
                .isEqualTo(activity.getString(R.string.stripe_paymentsheet_primary_button_processing))
        }
    }

    @Test
    fun `Verify FinishProcessing state calls the callback`() {
        Dispatchers.setMain(testDispatcher)
        val scenario = activityScenario()
        scenario.launch(intent).onActivity {
            // wait for bottom sheet to animate in
            idleLooper()

            viewModel.checkoutIdentifier = CheckoutIdentifier.SheetBottomBuy

            var finishProcessingCalled = false
            viewModel._viewState.value = PaymentSheetViewState.FinishProcessing {
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

            // wait for bottom sheet to animate in
            idleLooper()

            viewModel.checkoutIdentifier = CheckoutIdentifier.SheetTopGooglePay
            viewModel._viewState.value = PaymentSheetViewState.StartProcessing

            idleLooper()

            val googlePayButton =
                StripeGooglePayButtonBinding.bind(activity.viewBinding.googlePayButton)
            assertThat(googlePayButton.primaryButton.externalLabel)
                .isEqualTo(activity.getString(R.string.stripe_paymentsheet_primary_button_processing))
        }
    }

    @Test
    fun `Verify FinishProcessing state calls the callback on google pay view state observer`() {
        Dispatchers.setMain(testDispatcher)
        val scenario = activityScenario()
        scenario.launch(intent).onActivity {
            viewModel.checkoutIdentifier = CheckoutIdentifier.SheetTopGooglePay

            // wait for bottom sheet to animate in
            idleLooper()

            var finishProcessingCalled = false
            viewModel._viewState.value = PaymentSheetViewState.FinishProcessing {
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
            // wait for bottom sheet to animate in
            idleLooper()

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
            // wait for bottom sheet to animate in
            testDispatcher.scheduler.apply {
                advanceTimeBy(500)
                runCurrent()
            }
            idleLooper()

            viewModel.checkoutIdentifier = CheckoutIdentifier.SheetBottomBuy
            viewModel.onPaymentResult(PaymentResult.Completed)

            idleLooper()

            assertThat(activity.bottomSheetBehavior.state)
                .isEqualTo(BottomSheetBehavior.STATE_HIDDEN)
        }
    }

    @Test
    fun `shows add card fragment when no saved payment methods available`() {
        val scenario = activityScenario(
            createViewModel(
                paymentMethods = emptyList()
            )
        )
        scenario.launchForResult(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()

            assertThat(currentFragment(activity))
                .isInstanceOf(PaymentSheetAddPaymentMethodFragment::class.java)
            assertThat(activity.bottomSheetBehavior.state)
                .isEqualTo(BottomSheetBehavior.STATE_EXPANDED)

            // make sure loading fragment isn't in back stack
            activity.onBackPressed()
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

    @Test
    fun `buyButton and googlePayButton are enabled when not processing, transition target, and a selection has been made`() {
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity { activity ->
            assertThat(activity.viewBinding.buyButton.isEnabled)
                .isTrue()
            assertThat(activity.viewBinding.googlePayButton.isEnabled)
                .isTrue()
            // wait for bottom sheet to animate in
            idleLooper()

            assertThat(activity.viewBinding.buyButton.isEnabled)
                .isTrue()
            assertThat(activity.viewBinding.googlePayButton.isEnabled)
                .isTrue()

            viewModel.updateSelection(
                PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            )
            idleLooper()

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
    fun `Complete fragment transactions prior to setting the sheet mode and thus the back button`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()

            assertThat(currentFragment(activity))
                .isInstanceOf(PaymentSheetListFragment::class.java)
            assertThat(activity.bottomSheetBehavior.state)
                .isEqualTo(BottomSheetBehavior.STATE_EXPANDED)

            viewModel.transitionToAddPaymentScreen()

            idleLooper()

            assertThat(currentFragment(activity))
                .isInstanceOf(PaymentSheetAddPaymentMethodFragment::class.java)
            assertThat(activity.bottomSheetBehavior.state)
                .isEqualTo(BottomSheetBehavior.STATE_EXPANDED)
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
            // wait for bottom sheet to animate in
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
            // wait for bottom sheet to animate in
            idleLooper()

            assertThat(activity.viewBinding.topMessage.isVisible).isFalse()
            assertThat(activity.viewBinding.topMessage.text.isNullOrEmpty()).isTrue()

            viewModel.checkoutIdentifier = CheckoutIdentifier.SheetTopGooglePay
            val errorMessage = "Error message"
            viewModel._viewState.value =
                PaymentSheetViewState.Reset(BaseSheetViewModel.UserErrorMessage(errorMessage))

            assertThat(activity.viewBinding.topMessage.isVisible).isTrue()
            assertThat(activity.viewBinding.topMessage.text.toString()).isEqualTo(errorMessage)
        }
    }

    @Test
    fun `when new payment method is selected then error message is cleared`() {
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()

            assertThat(activity.viewBinding.message.isVisible).isFalse()
            assertThat(activity.viewBinding.message.text.isNullOrEmpty()).isTrue()
            assertThat(activity.viewBinding.topMessage.isVisible).isFalse()
            assertThat(activity.viewBinding.topMessage.text.isNullOrEmpty()).isTrue()

            val errorMessage = "Error message"
            viewModel._viewState.value =
                PaymentSheetViewState.Reset(BaseSheetViewModel.UserErrorMessage(errorMessage))

            assertThat(activity.viewBinding.message.isVisible).isTrue()
            assertThat(activity.viewBinding.message.text.toString()).isEqualTo(errorMessage)
            assertThat(activity.viewBinding.topMessage.isVisible).isFalse()
            assertThat(activity.viewBinding.topMessage.text.isNullOrEmpty()).isTrue()

            viewModel.updateSelection(PaymentSelection.GooglePay)

            assertThat(activity.viewBinding.message.isVisible).isFalse()
            assertThat(activity.viewBinding.message.text.isNullOrEmpty()).isTrue()
            assertThat(activity.viewBinding.topMessage.isVisible).isFalse()
            assertThat(activity.viewBinding.topMessage.text.isNullOrEmpty()).isTrue()

            viewModel.checkoutIdentifier = CheckoutIdentifier.SheetTopGooglePay
            viewModel._viewState.value =
                PaymentSheetViewState.Reset(BaseSheetViewModel.UserErrorMessage(errorMessage))

            assertThat(activity.viewBinding.message.isVisible).isFalse()
            assertThat(activity.viewBinding.message.text.isNullOrEmpty()).isTrue()
            assertThat(activity.viewBinding.topMessage.isVisible).isTrue()
            assertThat(activity.viewBinding.topMessage.text.toString()).isEqualTo(errorMessage)

            viewModel.updateSelection(PaymentSelection.GooglePay)

            assertThat(activity.viewBinding.message.isVisible).isFalse()
            assertThat(activity.viewBinding.message.text.isNullOrEmpty()).isTrue()
            assertThat(activity.viewBinding.topMessage.isVisible).isFalse()
            assertThat(activity.viewBinding.topMessage.text.isNullOrEmpty()).isTrue()
        }
    }

    @Test
    fun `when checkout starts then error message is cleared`() {
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()

            assertThat(activity.viewBinding.message.isVisible).isFalse()
            assertThat(activity.viewBinding.message.text.isNullOrEmpty()).isTrue()
            assertThat(activity.viewBinding.topMessage.isVisible).isFalse()
            assertThat(activity.viewBinding.topMessage.text.isNullOrEmpty()).isTrue()

            val errorMessage = "Error message"
            viewModel._viewState.value =
                PaymentSheetViewState.Reset(BaseSheetViewModel.UserErrorMessage(errorMessage))

            assertThat(activity.viewBinding.message.isVisible).isTrue()
            assertThat(activity.viewBinding.message.text.toString()).isEqualTo(errorMessage)
            assertThat(activity.viewBinding.topMessage.isVisible).isFalse()
            assertThat(activity.viewBinding.topMessage.text.isNullOrEmpty()).isTrue()

            viewModel.checkout()

            assertThat(activity.viewBinding.message.isVisible).isFalse()
            assertThat(activity.viewBinding.message.text.isNullOrEmpty()).isTrue()
            assertThat(activity.viewBinding.topMessage.isVisible).isFalse()
            assertThat(activity.viewBinding.topMessage.text.isNullOrEmpty()).isTrue()

            viewModel.checkoutIdentifier = CheckoutIdentifier.SheetTopGooglePay
            viewModel._viewState.value =
                PaymentSheetViewState.Reset(BaseSheetViewModel.UserErrorMessage(errorMessage))

            assertThat(activity.viewBinding.message.isVisible).isFalse()
            assertThat(activity.viewBinding.message.text.isNullOrEmpty()).isTrue()
            assertThat(activity.viewBinding.topMessage.isVisible).isTrue()
            assertThat(activity.viewBinding.topMessage.text.toString()).isEqualTo(errorMessage)

            viewModel.checkout()

            assertThat(activity.viewBinding.message.isVisible).isFalse()
            assertThat(activity.viewBinding.message.text.isNullOrEmpty()).isTrue()
            assertThat(activity.viewBinding.topMessage.isVisible).isFalse()
            assertThat(activity.viewBinding.topMessage.text.isNullOrEmpty()).isTrue()
        }
    }

    @Test
    fun `when intent is in live mode show no indicator`() {
        val viewModel = createViewModel(paymentIntent = PAYMENT_INTENT.copy(isLiveMode = true))
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()
            assertThat(activity.viewBinding.testmode.isVisible).isFalse()
        }
    }

    @Test
    fun `when intent is not in live mode show indicator`() {
        val viewModel = createViewModel(paymentIntent = PAYMENT_INTENT.copy(isLiveMode = false))
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()
            assertThat(activity.viewBinding.testmode.isVisible).isTrue()
        }
    }

    @Test
    fun `Buy button should be enabled when primaryButtonEnabled is true`() {
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()

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
            // wait for bottom sheet to animate in
            idleLooper()

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
            // wait for bottom sheet to animate in
            idleLooper()

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
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()

            viewModel._viewState.value = PaymentSheetViewState.Reset(null)

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
            // wait for bottom sheet to animate in
            idleLooper()

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
            // wait for bottom sheet to animate in
            idleLooper()

            viewModel.updateBelowButtonText(null)
            assertThat(activity.viewBinding.notes.isVisible).isFalse()
        }
    }

    @Test
    fun `verify animation is enabled for layout transition changes`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()

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
    fun `Verify if customer has payment methods, display the saved payment methods screen`() {
        val viewModel = createViewModel(paymentMethods = PAYMENT_METHODS)

        val transitionTargets = mutableListOf<TransitionTarget>()
        viewModel.transition.observeEventsForever { transitionTargets.add(it) }

        activityScenario(viewModel).launch(intent).use {
            it.onActivity {
                idleLooper()
            }
        }

        assertThat(transitionTargets).containsExactly(TransitionTarget.SelectSavedPaymentMethods)
    }

    @Test
    fun `Verify if there are no payment methods, display the add payment method screen`() {
        val viewModel = createViewModel(paymentMethods = emptyList())

        val transitionTargets = mutableListOf<TransitionTarget>()
        viewModel.transition.observeEventsForever { transitionTargets.add(it) }

        activityScenario(viewModel).launch(intent).use {
            it.onActivity {
                idleLooper()
            }
        }

        assertThat(transitionTargets).containsExactly(TransitionTarget.AddFirstPaymentMethod)
    }

    @Test
    fun `Verify doesn't transition to first screen again on activity recreation`() {
        val viewModel = createViewModel(paymentMethods = emptyList())

        val transitionTargets = mutableListOf<TransitionTarget>()
        viewModel.transition.observeEventsForever { transitionTargets.add(it) }

        activityScenario(viewModel).launch(intent).use {
            it.onActivity {
                idleLooper()
            }

            it.recreate()

            it.onActivity {
                idleLooper()
            }
        }

        assertThat(transitionTargets).containsExactly(TransitionTarget.AddFirstPaymentMethod)
    }

    @Test
    fun `processing should enable after checkout`() {
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()

            runTest {
                viewModel.processing.test {
                    assertThat(awaitItem()).isFalse()
                    viewModel.checkout()
                    assertThat(awaitItem()).isTrue()
                }
            }
        }
    }

    private fun currentFragment(activity: PaymentSheetActivity) =
        activity.supportFragmentManager.findFragmentById(activity.viewBinding.fragmentContainer.id)

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
        paymentMethods: List<PaymentMethod> = PAYMENT_METHODS
    ): PaymentSheetViewModel = runBlocking {
        val lpmRepository = mock<LpmRepository>()
        whenever(lpmRepository.fromCode(any())).thenReturn(LpmRepository.HardcodedCard)
        whenever(lpmRepository.serverSpecLoadingState).thenReturn(LpmRepository.ServerSpecState.Uninitialized)

        val linkPaymentLauncher = mock<LinkPaymentLauncher>().stub {
            onBlocking { getAccountStatusFlow(any()) }.thenReturn(flowOf(AccountStatus.SignedOut))
        }

        registerFormViewModelInjector()

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
                linkState = LinkState(
                    configuration = LinkPaymentLauncher.Configuration(
                        stripeIntent = paymentIntent,
                        merchantName = "Merchant",
                        customerEmail = null,
                        customerName = null,
                        customerPhone = null,
                        customerBillingCountryCode = null,
                        shippingValues = null,
                    ),
                    loginState = LinkState.LoginState.LoggedOut,
                ).takeIf { paymentIntent.paymentMethodTypes.contains("link") },
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
            savedStateHandle = SavedStateHandle(),
            linkLauncher = linkPaymentLauncher
        ).also {
            it.injector = injector
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
            formFragmentArguments = FormFragmentArguments(
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
        whenever(mockFormBuilder.formFragmentArguments(any())).thenReturn(mockFormBuilder)
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
