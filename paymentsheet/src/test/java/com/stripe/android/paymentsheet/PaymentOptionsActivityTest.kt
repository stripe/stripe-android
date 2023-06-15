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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentSheetFixtures.PAYMENT_OPTIONS_CONTRACT_ARGS
import com.stripe.android.paymentsheet.PaymentSheetFixtures.updateState
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.databinding.StripeActivityPaymentOptionsBinding
import com.stripe.android.paymentsheet.databinding.StripePrimaryButtonBinding
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.getLabel
import com.stripe.android.ui.core.forms.resources.LpmRepository
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
import org.robolectric.annotation.Config
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

    private val StripeActivityPaymentOptionsBinding.continueButton: PrimaryButton
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
    fun `click outside of bottom sheet before selection should return cancel result without selection`() {
        runActivityScenario {
            it.onActivity { activity ->
                activity.viewBinding.root.performClick()
                activity.finish()
            }

            assertThat(
                PaymentOptionResult.fromIntent(it.getResult().resultData)
            ).isEqualTo(
                PaymentOptionResult.Canceled(null, null, listOf())
            )
        }
    }

    @Test
    fun `click outside of bottom sheet should return cancel result even if there is a selection`() {
        val initialSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.createCard(),
        )

        val usBankAccount = PaymentMethodFixtures.US_BANK_ACCOUNT
        val paymentMethods = listOf(initialSelection.paymentMethod, usBankAccount)

        val args = PAYMENT_OPTIONS_CONTRACT_ARGS.updateState(
            paymentSelection = initialSelection,
            paymentMethods = paymentMethods,
        )

        runActivityScenario(args) {
            it.onActivity { activity ->
                // We use US Bank Account because they don't dismiss PaymentSheet upon selection
                // due to their mandate requirement.
                val usBankAccountLabel = usBankAccount.getLabel(context.resources)
                composeTestRule
                    .onNodeWithTag("${PAYMENT_OPTION_CARD_TEST_TAG}_$usBankAccountLabel")
                    .performClick()

                activity.viewBinding.root.performClick()
                activity.finish()
            }

            val result = PaymentOptionResult.fromIntent(it.getResult().resultData)
            assertThat(result).isEqualTo(
                PaymentOptionResult.Canceled(null, initialSelection, paymentMethods)
            )
        }
    }

    @Test
    fun `ContinueButton should be hidden when showing payment options`() {
        val args = PAYMENT_OPTIONS_CONTRACT_ARGS.updateState(
            paymentMethods = PaymentMethodFixtures.createCards(5)
        )

        runActivityScenario(args) {
            it.onActivity { activity ->
                assertThat(activity.viewBinding.continueButton.isVisible).isFalse()
            }
        }
    }

    @Test
    fun `ContinueButton should be visible when showing add payment method form`() {
        val args = PAYMENT_OPTIONS_CONTRACT_ARGS.updateState(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK
        )

        runActivityScenario(args) {
            it.onActivity { activity ->
                assertThat(activity.viewBinding.continueButton.isVisible).isTrue()
            }
        }
    }

    @Test
    fun `ContinueButton should be hidden when returning to payment options`() {
        val args = PAYMENT_OPTIONS_CONTRACT_ARGS.updateState(
            paymentMethods = PaymentMethodFixtures.createCards(5)
        )

        runActivityScenario(args) {
            it.onActivity { activity ->
                assertThat(activity.viewBinding.continueButton.isVisible).isFalse()

                // Navigate to "Add Payment Method" fragment
                composeTestRule
                    .onNodeWithTag("${PAYMENT_OPTION_CARD_TEST_TAG}_+ Add")
                    .performClick()

                assertThat(activity.viewBinding.continueButton.isVisible).isTrue()

                // Navigate back to payment options list
                pressBack()

                assertThat(activity.viewBinding.continueButton.isVisible).isFalse()
            }
        }
    }

    @Test
    fun `Verify Ready state updates the add button label`() {
        runActivityScenario {
            it.onActivity { activity ->
                val addBinding = StripePrimaryButtonBinding.bind(activity.viewBinding.continueButton)

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
        runActivityScenario {
            it.onActivity { activity ->
                assertThat(activity.bottomSheetBehavior.state)
                    .isEqualTo(BottomSheetBehavior.STATE_EXPANDED)
                assertThat(activity.bottomSheetBehavior.isFitToContents)
                    .isFalse()
            }
        }
    }

    @Test
    fun `Verify selecting a payment method closes the sheet`() {
        val args = PAYMENT_OPTIONS_CONTRACT_ARGS.updateState(isGooglePayReady = true)

        runActivityScenario(args) {
            it.onActivity { activity ->
                composeTestRule
                    .onNodeWithTag("${PAYMENT_OPTION_CARD_TEST_TAG}_Google Pay")
                    .performClick()

                composeTestRule.waitForIdle()

                idleLooper()

                assertThat(activity.bottomSheetBehavior.state)
                    .isEqualTo(BottomSheetBehavior.STATE_HIDDEN)
            }
        }
    }

    @Test
    fun `notes visibility is set correctly`() {
        val usBankAccount = PaymentMethodFixtures.US_BANK_ACCOUNT

        val label = usBankAccount.getLabel(context.resources)
        val mandateText = "By continuing, you agree to authorize payments pursuant to these terms."

        val args = PAYMENT_OPTIONS_CONTRACT_ARGS.updateState(
            paymentMethods = listOf(usBankAccount),
            isGooglePayReady = true,
        )

        runActivityScenario(args) {
            it.onActivity {
                composeTestRule
                    .onNodeWithTag("${PAYMENT_OPTION_CARD_TEST_TAG}_$label")
                    .performClick()

                composeTestRule
                    .onNodeWithText(mandateText)
                    .assertIsDisplayed()

                composeTestRule
                    .onNodeWithTag("${PAYMENT_OPTION_CARD_TEST_TAG}_Google Pay")
                    .performClick()

                composeTestRule
                    .onNodeWithText(mandateText)
                    .assertDoesNotExist()
            }
        }
    }

    @Test
    fun `primary button appearance is set`() {
        val args = PAYMENT_OPTIONS_CONTRACT_ARGS.updateState(
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
            ),
        )

        runActivityScenario(args) {
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

    private fun runActivityScenario(
        args: PaymentOptionContract.Args = PAYMENT_OPTIONS_CONTRACT_ARGS,
        block: (InjectableActivityScenario<PaymentOptionsActivity>) -> Unit,
    ) {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            PaymentOptionsActivity::class.java
        ).putExtras(
            bundleOf(ActivityStarter.Args.EXTRA to args)
        )

        val lpmRepository = LpmRepository(
            arguments = LpmRepository.LpmRepositoryArguments(
                resources = ApplicationProvider.getApplicationContext<Context>().resources,
            ),
            lpmInitialFormData = LpmRepository.LpmInitialFormData(),
        ).apply {
            update(
                stripeIntent = args.state.stripeIntent,
                serverLpmSpecs = null,
            )
        }

        val viewModel = TestViewModelFactory.create(
            linkConfigurationCoordinator = mock<LinkConfigurationCoordinator>().stub {
                onBlocking { getAccountStatusFlow(any()) }.thenReturn(flowOf(AccountStatus.SignedOut))
            },
        ) { linkHandler, linkInteractor, savedStateHandle ->
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
                linkConfigurationCoordinator = linkInteractor,
                formViewModelSubComponentBuilderProvider = mock(),
            )
        }

        val scenario = injectableActivityScenario<PaymentOptionsActivity> {
            injectActivity {
                viewModelFactory = viewModelFactoryFor(viewModel)
            }
        }

        scenario.launchForResult(intent).use(block)
    }
}
