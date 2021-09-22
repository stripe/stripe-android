package com.stripe.android.paymentsheet

import android.animation.LayoutTransition
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.Logger
import com.stripe.android.PaymentConfiguration
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContract
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherFactory
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.PaymentSheetViewModel.CheckoutIdentifier
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.databinding.PrimaryButtonBinding
import com.stripe.android.paymentsheet.databinding.StripeGooglePayButtonBinding
import com.stripe.android.paymentsheet.model.FragmentConfigFixtures
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.model.StripeIntentValidator
import com.stripe.android.paymentsheet.repositories.StripeIntentRepository
import com.stripe.android.paymentsheet.ui.PrimaryButtonAnimator
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.utils.InjectableActivityScenario
import com.stripe.android.utils.TestUtils.idleLooper
import com.stripe.android.utils.TestUtils.viewModelFactoryFor
import com.stripe.android.utils.injectableActivityScenario
import com.stripe.android.view.ActivityScenarioFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class PaymentSheetActivityTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val testDispatcher = TestCoroutineDispatcher()

    private val eventReporter = mock<EventReporter>()
    private val googlePayPaymentMethodLauncherFactory =
        createGooglePayPaymentMethodLauncherFactory()
    private val stripePaymentLauncherAssistedFactory =
        mock<StripePaymentLauncherAssistedFactory>()

    private val viewModel = createViewModel()

    private val contract = PaymentSheetContract()

    private val intent = contract.createIntent(
        context,
        PaymentSheetContract.Args(
            PaymentIntentClientSecret("client_secret"),
            PaymentSheetFixtures.CONFIG_CUSTOMER,
            statusBarColor = PaymentSheetFixtures.STATUS_BAR_COLOR,
        )
    )

    @BeforeTest
    fun before() {
        Dispatchers.setMain(testDispatcher)

        PaymentConfiguration.init(
            context,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `bottom sheet expands on start and handles click outside`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
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
    fun `updates buy button state on payment methods list`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            // Google Pay initially selected as there's no saved selection
            assertThat(activity.viewBinding.buyButton.isVisible).isFalse()
            assertThat(activity.viewBinding.googlePayButton.isVisible).isTrue()
            assertThat(activity.viewBinding.googlePayButton.isEnabled).isTrue()

            viewModel.updateSelection(
                PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            )
            assertThat(activity.viewBinding.buyButton.isVisible).isTrue()
            assertThat(activity.viewBinding.buyButton.isEnabled).isTrue()
            assertThat(activity.viewBinding.googlePayButton.isVisible).isFalse()

            viewModel.updateSelection(null)
            assertThat(activity.viewBinding.buyButton.isVisible).isTrue()
            assertThat(activity.viewBinding.buyButton.isEnabled).isFalse()
            assertThat(activity.viewBinding.googlePayButton.isVisible).isFalse()
        }
    }

    @Test
    fun `disables primary button when editing`() {
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()

            assertThat(activity.viewBinding.buyButton.isEnabled)
                .isTrue()
            assertThat(activity.viewBinding.googlePayButton.isEnabled)
                .isTrue()

            viewModel.setEditing(true)

            assertThat(activity.viewBinding.buyButton.isEnabled)
                .isFalse()
            assertThat(activity.viewBinding.googlePayButton.isEnabled)
                .isFalse()
        }
    }

    @Test
    fun `updates buy button state on add payment`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            viewModel.transitionTo(
                PaymentSheetViewModel.TransitionTarget.AddPaymentMethodFull(
                    FragmentConfigFixtures.DEFAULT
                )
            )
            idleLooper()

            // Initially empty card
            assertThat(activity.viewBinding.buyButton.isVisible).isTrue()
            assertThat(activity.viewBinding.buyButton.isEnabled).isFalse()
            assertThat(activity.viewBinding.googlePayButton.isVisible).isFalse()

            // Update to Google Pay
            viewModel.updateSelection(PaymentSelection.GooglePay)
            assertThat(activity.viewBinding.buyButton.isVisible).isTrue()
            assertThat(activity.viewBinding.buyButton.isEnabled).isFalse()
            assertThat(activity.viewBinding.googlePayButton.isVisible).isFalse()
            viewModel.onGooglePayResult(GooglePayPaymentMethodLauncher.Result.Canceled)

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
                    shouldSavePaymentMethod = false
                )
            )
            assertThat(activity.viewBinding.buyButton.isVisible).isTrue()
            assertThat(activity.viewBinding.buyButton.isEnabled).isTrue()
        }
    }

    @Test
    fun `handles fragment transitions`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()

            assertThat(currentFragment(activity))
                .isInstanceOf(PaymentSheetListFragment::class.java)
            assertThat(activity.bottomSheetBehavior.state)
                .isEqualTo(BottomSheetBehavior.STATE_EXPANDED)

            viewModel.transitionTo(
                PaymentSheetViewModel.TransitionTarget.AddPaymentMethodFull(
                    FragmentConfigFixtures.DEFAULT
                )
            )
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

            viewModel.transitionTo(
                PaymentSheetViewModel.TransitionTarget.AddPaymentMethodFull(
                    FragmentConfigFixtures.DEFAULT
                )
            )
            idleLooper()

            assertThat(activity.toolbar.navigationContentDescription)
                .isEqualTo(context.getString(R.string.stripe_paymentsheet_back))

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
                        clientSecret = "client_secret"
                    )
                )
        }
    }

    @Test
    fun `Verify animation is enabled for layout transition changes`() {
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
    fun `Verify processing state disables toolbar`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()

            viewModel._processing.value = true

            idleLooper()

            assertThat(activity.toolbar.isEnabled).isFalse()
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

            val buyBinding = PrimaryButtonBinding.bind(activity.viewBinding.buyButton)
            assertThat(buyBinding.label.text)
                .isEqualTo(activity.getString(R.string.stripe_paymentsheet_primary_button_processing))
        }
    }

    @Test
    fun `Verify FinishProcessing state calls the callback`() {
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

            testDispatcher.advanceTimeBy(PrimaryButtonAnimator.HOLD_ANIMATION_ON_SLIDE_IN_COMPLETION)

            assertThat(finishProcessingCalled).isTrue()
        }
    }

    @Test
    fun `Verify StartProcessing state updates the google button label`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->

            // wait for bottom sheet to animate in
            idleLooper()

            viewModel.checkoutIdentifier = CheckoutIdentifier.SheetBottomGooglePay
            viewModel._viewState.value = PaymentSheetViewState.StartProcessing

            idleLooper()

            val googlePayButton =
                StripeGooglePayButtonBinding.bind(activity.viewBinding.googlePayButton)
            assertThat(googlePayButton.primaryButton.viewBinding.label.text)
                .isEqualTo(activity.getString(R.string.stripe_paymentsheet_primary_button_processing))
        }
    }

    @Test
    fun `Verify FinishProcessing state calls the callback on google pay view state observer`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity {
            viewModel.checkoutIdentifier = CheckoutIdentifier.SheetBottomGooglePay

            // wait for bottom sheet to animate in
            idleLooper()

            var finishProcessingCalled = false
            viewModel._viewState.value = PaymentSheetViewState.FinishProcessing {
                finishProcessingCalled = true
            }

            idleLooper()

            testDispatcher.advanceTimeBy(PrimaryButtonAnimator.HOLD_ANIMATION_ON_SLIDE_IN_COMPLETION)

            assertThat(finishProcessingCalled).isTrue()
        }
    }

    @Test
    fun `Verify ProcessResult state closes the sheet`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()

            viewModel._paymentSheetResult.value = PaymentSheetResult.Completed

            idleLooper()

            // wait animate time...
            testDispatcher.advanceTimeBy(
                PrimaryButtonAnimator.HOLD_ANIMATION_ON_SLIDE_IN_COMPLETION
            )

            assertThat(activity.bottomSheetBehavior.state)
                .isEqualTo(BottomSheetBehavior.STATE_HIDDEN)
        }
    }

    @Test
    fun `successful payment should dismiss bottom sheet`() {
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            testDispatcher.advanceTimeBy(500)
            idleLooper()

            viewModel.checkoutIdentifier = CheckoutIdentifier.SheetBottomBuy
            viewModel.onPaymentResult(PaymentResult.Completed)

            idleLooper()

            assertThat(activity.bottomSheetBehavior.state)
                .isEqualTo(BottomSheetBehavior.STATE_HIDDEN)
        }
    }

    @Test
    fun `shows add card fragment when no payment methods available`() {
        val scenario = activityScenario(
            createViewModel(
                paymentMethods = emptyList()
            )
        )
        scenario.launch(intent).onActivity { activity ->
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
    fun `buyButton is only enabled when not processing, transition target, and a selection has been made`() {
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity { activity ->
            assertThat(activity.viewBinding.buyButton.isEnabled)
                .isTrue()
            // wait for bottom sheet to animate in
            idleLooper()

            assertThat(activity.viewBinding.buyButton.isEnabled)
                .isTrue()

            viewModel.updateSelection(PaymentSelection.GooglePay)
            idleLooper()

            assertThat(activity.viewBinding.buyButton.isEnabled)
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

            viewModel.transitionTo(
                PaymentSheetViewModel.TransitionTarget.SelectSavedPaymentMethod(
                    FragmentConfigFixtures.DEFAULT
                )
            )
            viewModel.transitionTo(
                PaymentSheetViewModel.TransitionTarget.AddPaymentMethodFull(
                    FragmentConfigFixtures.DEFAULT
                )
            )

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
        scenario.launch(intent).onActivity { activity ->
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
    fun `when new payment method is selected then error message is cleared`() {
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()

            assertThat(activity.viewBinding.message.isVisible).isFalse()
            assertThat(activity.viewBinding.message.text.isNullOrEmpty()).isTrue()

            val errorMessage = "Error message"
            viewModel._viewState.value =
                PaymentSheetViewState.Reset(BaseSheetViewModel.UserErrorMessage(errorMessage))

            assertThat(activity.viewBinding.message.isVisible).isTrue()
            assertThat(activity.viewBinding.message.text).isEqualTo(errorMessage)

            viewModel.updateSelection(PaymentSelection.GooglePay)

            assertThat(activity.viewBinding.message.isVisible).isFalse()
            assertThat(activity.viewBinding.message.text.isNullOrEmpty()).isTrue()
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

            val errorMessage = "Error message"
            viewModel._viewState.value =
                PaymentSheetViewState.Reset(BaseSheetViewModel.UserErrorMessage(errorMessage))

            assertThat(activity.viewBinding.message.isVisible).isTrue()
            assertThat(activity.viewBinding.message.text).isEqualTo(errorMessage)

            viewModel.checkout(CheckoutIdentifier.SheetBottomBuy)

            assertThat(activity.viewBinding.message.isVisible).isFalse()
            assertThat(activity.viewBinding.message.text.isNullOrEmpty()).isTrue()
        }
    }

    @Test
    fun `when intent is in live mode show no indicator`() {
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()

            viewModel._liveMode.value = true
            assertThat(activity.viewBinding.livemode.isVisible).isFalse()
        }
    }

    @Test
    fun `when intent is not in live mode show indicator`() {
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            idleLooper()

            viewModel._liveMode.value = false
            assertThat(activity.viewBinding.livemode.isVisible).isTrue()
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
        PaymentSheetViewModel(
            ApplicationProvider.getApplicationContext(),
            PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY,
            eventReporter,
            { PaymentConfiguration(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY) },
            StripeIntentRepository.Static(paymentIntent),
            StripeIntentValidator(),
            FakeCustomerRepository(paymentMethods),
            FakePrefsRepository(),
            stripePaymentLauncherAssistedFactory,
            googlePayPaymentMethodLauncherFactory,
            Logger.noop(),
            testDispatcher
        )
    }

    private fun createGooglePayPaymentMethodLauncherFactory() =
        object : GooglePayPaymentMethodLauncherFactory {
            override fun create(
                lifecycleScope: CoroutineScope,
                config: GooglePayPaymentMethodLauncher.Config,
                readyCallback: GooglePayPaymentMethodLauncher.ReadyCallback,
                activityResultLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContract.Args>
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
