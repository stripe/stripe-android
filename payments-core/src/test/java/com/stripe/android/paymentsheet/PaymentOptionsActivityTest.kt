package com.stripe.android.paymentsheet

import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.databinding.PrimaryButtonBinding
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentOptionsViewModel.TransitionTarget
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.utils.InjectableActivityScenario
import com.stripe.android.utils.TestUtils.idleLooper
import com.stripe.android.utils.TestUtils.viewModelFactoryFor
import com.stripe.android.utils.injectableActivityScenario
import com.stripe.android.view.ActivityStarter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class PaymentOptionsActivityTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()

    private val eventReporter = mock<EventReporter>()
    private val viewModel = createViewModel()

    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `click outside of bottom sheet should return cancel result`() {
        val scenario = activityScenario()
        scenario.launch(
            createIntent()
        ).use {
            it.onActivity { activity ->
                activity.viewBinding.root.performClick()
                activity.finish()
            }

            assertThat(
                PaymentOptionResult.fromIntent(scenario.getResult().resultData)
            ).isEqualTo(
                PaymentOptionResult.Canceled(null)
            )
        }
    }

    @Test
    fun `click outside of bottom sheet should return cancel result even if there is a selection`() {
        val scenario = activityScenario()
        scenario.launch(
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
                PaymentOptionResult.Canceled(null)
            )
        }
    }

    @Test
    fun `AddButton should be hidden when showing payment options`() {
        val scenario = activityScenario()
        scenario.launch(
            createIntent(
                PAYMENT_OPTIONS_CONTRACT_ARGS.copy(
                    paymentMethods = PaymentMethodFixtures.createCards(5)
                )
            )
        ).use {
            it.onActivity { activity ->
                assertThat(activity.viewBinding.addButton.isVisible)
                    .isFalse()
            }
        }
    }

    @Test
    fun `AddButton should be visible when showing add payment method form`() {
        val scenario = activityScenario()
        scenario.launch(
            createIntent()
        ).use {
            it.onActivity { activity ->
                assertThat(activity.viewBinding.addButton.isVisible)
                    .isTrue()
            }
        }
    }

    @Test
    fun `AddButton should be hidden when returning to payment options`() {
        val scenario = activityScenario()
        scenario.launch(
            createIntent(
                PAYMENT_OPTIONS_CONTRACT_ARGS.copy(
                    paymentMethods = PaymentMethodFixtures.createCards(5)
                )
            )
        ).use {
            it.onActivity { activity ->
                assertThat(activity.viewBinding.addButton.isVisible)
                    .isFalse()

                // Navigate to "Add Payment Method" fragment
                with(
                    activity.supportFragmentManager.findFragmentById(R.id.fragment_container)
                        as PaymentOptionsListFragment
                ) {
                    transitionToAddPaymentMethod()
                }
                idleLooper()

                assertThat(activity.viewBinding.addButton.isVisible)
                    .isTrue()

                // Navigate back to payment options list
                activity.onBackPressed()
                idleLooper()

                assertThat(activity.viewBinding.addButton.isVisible)
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

                val addBinding = PrimaryButtonBinding.bind(activity.viewBinding.addButton)

                assertThat(addBinding.confirmedIcon.isVisible)
                    .isFalse()

                assertThat(addBinding.label.text)
                    .isEqualTo("Add")

                activity.finish()
            }
        }
    }

    @Test
    fun `Verify if google pay is ready, stay on the select saved payment method`() {
        val viewModel = createViewModel(
            PAYMENT_OPTIONS_CONTRACT_ARGS.copy(isGooglePayReady = true)
        )
        val transitionTarget = mutableListOf<BaseSheetViewModel.Event<TransitionTarget?>>()
        viewModel.transition.observeForever {
            transitionTarget.add(it)
        }
        val scenario = activityScenario(viewModel)
        scenario.launch(
            createIntent()
        ).use {
            idleLooper()
            assertThat(transitionTarget[1].peekContent())
                .isInstanceOf(TransitionTarget.SelectSavedPaymentMethod::class.java)
        }
    }

    @Test
    fun `Verify if payment methods is not empty select, saved payment method`() {
        val args = PAYMENT_OPTIONS_CONTRACT_ARGS.copy(
            isGooglePayReady = false,
            paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )

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
        return PaymentOptionsViewModel(
            args = args,
            prefsRepository = FakePrefsRepository(),
            eventReporter = eventReporter,
            customerRepository = FakeCustomerRepository(),
            workContext = testDispatcher,
            application = ApplicationProvider.getApplicationContext()
        )
    }

    private companion object {
        private val PAYMENT_OPTIONS_CONTRACT_ARGS = PaymentOptionContract.Args(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentMethods = emptyList(),
            config = PaymentSheetFixtures.CONFIG_GOOGLEPAY,
            isGooglePayReady = false,
            newCard = null,
            statusBarColor = PaymentSheetFixtures.STATUS_BAR_COLOR,
            injectorKey = 0
        )
    }
}
