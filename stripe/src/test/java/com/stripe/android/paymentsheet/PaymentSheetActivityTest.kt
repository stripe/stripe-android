package com.stripe.android.paymentsheet

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentIntentResult
import com.stripe.android.R
import com.stripe.android.StripeIntentResult
import com.stripe.android.databinding.PrimaryButtonBinding
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.FakePaymentFlowResultProcessor
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.SessionId
import com.stripe.android.paymentsheet.model.FragmentConfigFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.ViewState
import com.stripe.android.paymentsheet.repositories.PaymentIntentRepository
import com.stripe.android.paymentsheet.repositories.PaymentMethodsRepository
import com.stripe.android.paymentsheet.ui.PrimaryButtonAnimator
import com.stripe.android.utils.InjectableActivityScenario
import com.stripe.android.utils.TestUtils.idleLooper
import com.stripe.android.utils.TestUtils.viewModelFactoryFor
import com.stripe.android.utils.injectableActivityScenario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
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

    private val googlePayRepository = FakeGooglePayRepository(true)
    private val eventReporter = mock<EventReporter>()

    private val viewModel = createViewModel()

    private val contract = PaymentSheetContract()

    private val intent = contract.createIntent(
        context,
        PaymentSheetContract.Args(
            "client_secret",
            sessionId = SessionId(),
            statusBarColor = PaymentSheetFixtures.STATUS_BAR_COLOR,
            PaymentSheetFixtures.CONFIG_CUSTOMER
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
    fun `handles clicks outside of bottom sheet`() {
        val scenario = activityScenario()
        scenario.launch(intent).use {
            it.onActivity { activity ->
                // wait for bottom sheet to animate in
                testDispatcher.advanceTimeBy(BottomSheetController.ANIMATE_IN_DELAY)
                idleLooper()
                assertThat(activity.bottomSheetBehavior.state)
                    .isEqualTo(BottomSheetBehavior.STATE_EXPANDED)

                activity.viewBinding.root.performClick()
                idleLooper()

                assertThat(activity.bottomSheetBehavior.state)
                    .isEqualTo(BottomSheetBehavior.STATE_HIDDEN)
            }
        }

        assertThat(
            contract.parseResult(
                scenario.getResult().resultCode,
                scenario.getResult().resultData
            )
        ).isEqualTo(
            PaymentResult.Canceled(
                null,
                PAYMENT_INTENT
            )
        )
    }

    @Test
    fun `updates buy button state`() {
        val scenario = activityScenario()
        scenario.launch(intent).use {
            it.onActivity { activity ->
                assertThat(activity.viewBinding.buyButton.isEnabled)
                    .isTrue()

                viewModel.updateSelection(PaymentSelection.GooglePay)
                assertThat(activity.viewBinding.buyButton.isEnabled).isTrue()

                viewModel.updateSelection(null)
                assertThat(activity.viewBinding.buyButton.isEnabled).isFalse()

                scenario.moveToState(Lifecycle.State.DESTROYED)
            }
        }
    }

    @Test
    fun `handles fragment transitions`() {
        val scenario = activityScenario()
        scenario.launch(intent).use {
            it.onActivity { activity ->
                // wait for bottom sheet to animate in
                testDispatcher.advanceTimeBy(BottomSheetController.ANIMATE_IN_DELAY)
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
                    .isInstanceOf(PaymentSheetAddCardFragment::class.java)
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
                PaymentResult.Canceled(
                    null,
                    PAYMENT_INTENT
                )
            )
        }
    }

    @Test
    fun `handles buy button clicks`() {
        val scenario = activityScenario()
        scenario.launch(intent).use {
            it.onActivity { activity ->
                // wait for bottom sheet to animate in
                testDispatcher.advanceTimeBy(BottomSheetController.ANIMATE_IN_DELAY)
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

                assertThat(viewModel.startConfirm.value)
                    .isEqualTo(
                        ConfirmPaymentIntentParams(
                            clientSecret = "client_secret",
                            paymentMethodId = "pm_123456789",
                            returnUrl = "stripe://return_url"
                        )
                    )
            }
        }
    }

    @Test
    fun `Verify Ready state updates the buy button label`() {
        val scenario = activityScenario()
        scenario.launch(intent).use {
            it.onActivity { activity ->
                // wait for bottom sheet to animate in
                testDispatcher.advanceTimeBy(BottomSheetController.ANIMATE_IN_DELAY)
                idleLooper()

                viewModel._viewState.value = ViewState.PaymentSheet.Ready(
                    amount = 1099,
                    currencyCode = "usd"
                )

                idleLooper()

                val buyBinding = PrimaryButtonBinding.bind(activity.viewBinding.buyButton)

                assertThat(buyBinding.confirmedIcon.isVisible)
                    .isFalse()
                assertThat(buyBinding.label.text)
                    .isEqualTo("Pay $10.99")

                idleLooper()

                activity.finish()
            }
        }
    }

    @Test
    fun `Verify StartProcessing state updates the buy button label`() {
        val scenario = activityScenario()
        scenario.launch(intent).use {
            it.onActivity { activity ->
                // wait for bottom sheet to animate in
                testDispatcher.advanceTimeBy(BottomSheetController.ANIMATE_IN_DELAY)
                idleLooper()

                viewModel._viewState.value = ViewState.PaymentSheet.StartProcessing

                idleLooper()

                val buyBinding = PrimaryButtonBinding.bind(activity.viewBinding.buyButton)
                assertThat(buyBinding.label.text)
                    .isEqualTo(activity.getString(R.string.stripe_paymentsheet_primary_button_processing))
            }
        }
    }

    @Test
    fun `Verify FinishProcessing state calls the callback`() {
        val scenario = activityScenario()
        scenario.launch(intent).use {
            it.onActivity { _ ->
                // wait for bottom sheet to animate in
                testDispatcher.advanceTimeBy(BottomSheetController.ANIMATE_IN_DELAY)
                idleLooper()

                var finishProcessingCalled = false
                viewModel._viewState.value = ViewState.PaymentSheet.FinishProcessing {
                    finishProcessingCalled = true
                }

                idleLooper()

                testDispatcher.advanceTimeBy(PrimaryButtonAnimator.HOLD_ANIMATION_ON_SLIDE_IN_COMPLETION)

                assertThat(finishProcessingCalled).isTrue()
            }
        }
    }

    @Test
    fun `Verify ProcessResult state closes the sheet`() {
        val scenario = activityScenario()
        scenario.launch(intent).use {
            it.onActivity { activity ->
                // wait for bottom sheet to animate in
                testDispatcher.advanceTimeBy(BottomSheetController.ANIMATE_IN_DELAY)
                idleLooper()

                viewModel._viewState.value = ViewState.PaymentSheet.ProcessResult(
                    PaymentIntentResult(
                        intent = PAYMENT_INTENT.copy(status = StripeIntent.Status.Succeeded),
                        outcomeFromFlow = StripeIntentResult.Outcome.SUCCEEDED
                    )
                )

                idleLooper()

                // wait animate time...
                testDispatcher.advanceTimeBy(
                    PrimaryButtonAnimator.HOLD_ANIMATION_ON_SLIDE_IN_COMPLETION
                )

                assertThat(activity.bottomSheetBehavior.state)
                    .isEqualTo(BottomSheetBehavior.STATE_HIDDEN)
            }
        }
    }

    @Test
    fun `successful payment should dismiss bottom sheet`() {
        val paymentIntentResult = PaymentIntentResult(PaymentIntentFixtures.PI_SUCCEEDED)
        val viewModel = createViewModel(
            paymentIntentResult = paymentIntentResult
        )

        val scenario = activityScenario(viewModel)
        scenario.launch(intent).use {
            it.onActivity { activity ->
                // wait for bottom sheet to animate in
                testDispatcher.advanceTimeBy(500)
                idleLooper()

                val viewStates = mutableListOf<ViewState?>()
                viewModel.viewState.observeForever { viewState ->
                    viewStates.add(viewState)
                }

                viewModel.onPaymentFlowResult(
                    PaymentFlowResult.Unvalidated(
                        "client_secret",
                        StripeIntentResult.Outcome.SUCCEEDED
                    )
                )
                idleLooper()

                assertThat(viewStates)
                    .isEqualTo(
                        listOf(
                            ViewState.PaymentSheet.Ready(amount = 1099, currencyCode = "usd"),
                            ViewState.PaymentSheet.FinishProcessing {},
                            ViewState.PaymentSheet.ProcessResult(result = paymentIntentResult)
                        )
                    )

                assertThat(activity.bottomSheetBehavior.state)
                    .isEqualTo(BottomSheetBehavior.STATE_HIDDEN)
            }
        }
    }

    @Test
    fun `shows add card fragment when no payment methods available`() {
        val scenario = activityScenario(
            createViewModel(
                paymentMethods = emptyList()
            )
        )
        scenario.launch(intent).use {
            it.onActivity { activity ->
                // wait for bottom sheet to animate in
                testDispatcher.advanceTimeBy(BottomSheetController.ANIMATE_IN_DELAY)
                idleLooper()

                assertThat(currentFragment(activity))
                    .isInstanceOf(PaymentSheetAddCardFragment::class.java)
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
                PaymentResult.Canceled(
                    null,
                    PAYMENT_INTENT
                )
            )
        }
    }

    @Test
    fun `buyButton is only enabled when not processing, transition target, and a selection has been made`() {
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).use {
            it.onActivity { activity ->
                assertThat(activity.viewBinding.buyButton.isEnabled)
                    .isTrue()
                // wait for bottom sheet to animate in
                testDispatcher.advanceTimeBy(BottomSheetController.ANIMATE_IN_DELAY)
                idleLooper()

                assertThat(activity.viewBinding.buyButton.isEnabled)
                    .isTrue()

                viewModel.updateSelection(PaymentSelection.GooglePay)
                idleLooper()

                assertThat(activity.viewBinding.buyButton.isEnabled)
                    .isTrue()
            }
        }
    }

    @Test
    fun `sets expected statusBarColor`() {
        val scenario = activityScenario()
        scenario.launch(intent).use {
            it.onActivity { activity ->
                assertThat(activity.window.statusBarColor)
                    .isEqualTo(PaymentSheetFixtures.STATUS_BAR_COLOR)
                scenario.moveToState(Lifecycle.State.DESTROYED)
            }
        }
    }

    @Test
    fun `Complete fragment transactions prior to setting the sheet mode and thus the back button`() {
        val scenario = activityScenario()
        scenario.launch(intent).use {
            it.onActivity { activity ->
                // wait for bottom sheet to animate in
                testDispatcher.advanceTimeBy(BottomSheetController.ANIMATE_IN_DELAY)
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

                assertThat(currentFragment(activity))
                    .isInstanceOf(PaymentSheetAddCardFragment::class.java)
                assertThat(activity.bottomSheetBehavior.state)
            }
        }
    }

    @Test
    fun `if fetched PaymentIntent is confirmed then should return Completed result`() {
        val scenario = activityScenario(
            createViewModel(
                paymentIntent = PaymentIntentFixtures.PI_SUCCEEDED
            )
        )
        scenario.launch(intent).use {
            it.onActivity { activity ->
                // wait for bottom sheet to animate in
                testDispatcher.advanceTimeBy(BottomSheetController.ANIMATE_IN_DELAY)
                activity.finish()
            }
        }

        assertThat(
            contract.parseResult(
                scenario.getResult().resultCode,
                scenario.getResult().resultData
            )
        ).isEqualTo(
            PaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED)
        )
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
        paymentMethods: List<PaymentMethod> = PAYMENT_METHODS,
        paymentIntentResult: PaymentIntentResult = PaymentIntentResult(paymentIntent)
    ): PaymentSheetViewModel {
        val paymentFlowResultProcessor = FakePaymentFlowResultProcessor().also {
            it.paymentIntentResult = paymentIntentResult
        }

        return PaymentSheetViewModel(
            publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            stripeAccountId = null,
            paymentIntentRepository = PaymentIntentRepository.Static(paymentIntent),
            paymentMethodsRepository = PaymentMethodsRepository.Static(paymentMethods),
            paymentFlowResultProcessor = paymentFlowResultProcessor,
            googlePayRepository = googlePayRepository,
            prefsRepository = FakePrefsRepository(),
            eventReporter = eventReporter,
            args = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY,
            workContext = testDispatcher
        )
    }

    private companion object {
        private val PAYMENT_INTENT = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        private val PAYMENT_METHODS = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
    }
}
