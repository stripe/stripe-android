package com.stripe.android.paymentsheet

import android.content.Context
import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentOptionsViewModel.TransitionTarget
import com.stripe.android.paymentsheet.PaymentSheetFixtures.PAYMENT_OPTIONS_CONTRACT_ARGS
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.databinding.PrimaryButtonBinding
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.address.AddressFieldElementRepository
import com.stripe.android.ui.core.elements.BankRepository
import com.stripe.android.ui.core.forms.resources.StaticResourceRepository
import com.stripe.android.utils.InjectableActivityScenario
import com.stripe.android.utils.TestUtils.idleLooper
import com.stripe.android.utils.TestUtils.viewModelFactoryFor
import com.stripe.android.utils.injectableActivityScenario
import com.stripe.android.view.ActivityStarter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class PaymentOptionsActivityTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private val eventReporter = mock<EventReporter>()
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
    fun `ContinueButton should be hidden when showing payment options`() {
        val scenario = activityScenario()
        scenario.launch(
            createIntent(
                PAYMENT_OPTIONS_CONTRACT_ARGS.copy(
                    paymentMethods = PaymentMethodFixtures.createCards(5)
                )
            )
        ).use {
            it.onActivity { activity ->
                assertThat(activity.viewBinding.continueButton.isVisible)
                    .isFalse()
            }
        }
    }

    @Test
    fun `ContinueButton should be visible when showing add payment method form`() {
        val scenario = activityScenario()
        scenario.launch(
            createIntent()
        ).use {
            it.onActivity { activity ->
                assertThat(activity.viewBinding.continueButton.isVisible)
                    .isTrue()
            }
        }
    }

    @Test
    fun `ContinueButton should be hidden when returning to payment options`() {
        val scenario = activityScenario()
        scenario.launch(
            createIntent(
                PAYMENT_OPTIONS_CONTRACT_ARGS.copy(
                    paymentMethods = PaymentMethodFixtures.createCards(5)
                )
            )
        ).use {
            it.onActivity { activity ->
                assertThat(activity.viewBinding.continueButton.isVisible)
                    .isFalse()

                // Navigate to "Add Payment Method" fragment
                with(
                    activity.supportFragmentManager.findFragmentById(R.id.fragment_container)
                        as PaymentOptionsListFragment
                ) {
                    transitionToAddPaymentMethod()
                }
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

                viewModel.updateSelection(mock())
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
                viewModel.updateNotes(
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
                viewModel.updateNotes(null)
                assertThat(activity.viewBinding.notes.isVisible).isFalse()
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
            prefsRepositoryFactory = { FakePrefsRepository() },
            eventReporter = eventReporter,
            customerRepository = FakeCustomerRepository(),
            workContext = testDispatcher,
            application = ApplicationProvider.getApplicationContext(),
            logger = Logger.noop(),
            injectorKey = DUMMY_INJECTOR_KEY,
            resourceRepository = StaticResourceRepository(
                BankRepository(
                    ApplicationProvider.getApplicationContext<Context>().resources
                ),
                AddressFieldElementRepository(
                    ApplicationProvider.getApplicationContext<Context>().resources
                )
            ),
            savedStateHandle = SavedStateHandle(),
            linkPaymentLauncherFactory = mock()
        )
    }
}
