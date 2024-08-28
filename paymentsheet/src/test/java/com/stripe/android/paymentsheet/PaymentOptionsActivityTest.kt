package com.stripe.android.paymentsheet

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
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
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentSheetFixtures.PAYMENT_OPTIONS_CONTRACT_ARGS
import com.stripe.android.paymentsheet.PaymentSheetFixtures.updateState
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.databinding.StripePrimaryButtonBinding
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_METHOD_CARD_TEST_TAG
import com.stripe.android.paymentsheet.ui.TEST_TAG_LIST
import com.stripe.android.paymentsheet.ui.getLabel
import com.stripe.android.uicore.elements.bottomsheet.BottomSheetContentTestTag
import com.stripe.android.utils.FakeCustomerRepository
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.InjectableActivityScenario
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import com.stripe.android.utils.TestUtils.idleLooper
import com.stripe.android.utils.TestUtils.viewModelFactoryFor
import com.stripe.android.utils.injectableActivityScenario
import com.stripe.android.view.ActivityStarter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
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

    private val PaymentOptionsActivity.continueButton: PrimaryButton
        get() = findViewById(R.id.primary_button)

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
    fun `dismissing bottom sheet before selection should return cancel result without selection`() {
        runActivityScenario {
            it.onActivity {
                pressBack()
            }

            composeTestRule.waitForIdle()
            idleLooper()

            assertThat(
                PaymentOptionResult.fromIntent(it.getResult().resultData)
            ).isEqualTo(
                PaymentOptionResult.Canceled(null, null, listOf())
            )
        }
    }

    @Test
    fun `dismissing bottom sheet should return cancel result even if there is a selection`() {
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
            it.onActivity {
                // We use US Bank Account because they don't dismiss PaymentSheet upon selection
                // due to their mandate requirement.
                val usBankAccountLabel = usBankAccount.getLabel()?.resolve(context)
                composeTestRule
                    .onNodeWithTag("${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_$usBankAccountLabel")
                    .performClick()

                pressBack()
            }

            composeTestRule.waitForIdle()

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
                assertThat(activity.continueButton.isVisible).isFalse()
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
                assertThat(activity.continueButton.isVisible).isTrue()
            }
        }
    }

    @Test
    fun `ContinueButton should be hidden when returning to payment options`() {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card"),
            )
        )

        val args = PAYMENT_OPTIONS_CONTRACT_ARGS.updateState(
            paymentMethods = PaymentMethodFixtures.createCards(5),
            stripeIntent = paymentMethodMetadata.stripeIntent,
        )

        runActivityScenario(args) {
            it.onActivity { activity ->
                assertThat(activity.continueButton.isVisible).isFalse()

                // Navigate to "Add Payment Method" fragment
                composeTestRule
                    .onNodeWithTag("${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_+ Add")
                    .performClick()

                assertThat(activity.continueButton.isVisible).isTrue()

                // Navigate back to payment options list
                pressBack()

                assertThat(activity.continueButton.isVisible).isFalse()
            }
        }
    }

    @Test
    fun `Verify Ready state updates the add button label`() {
        runActivityScenario {
            it.onActivity { activity ->
                val addBinding = StripePrimaryButtonBinding.bind(activity.continueButton)

                assertThat(addBinding.confirmedIcon.isVisible)
                    .isFalse()

                assertThat(activity.continueButton.externalLabel?.resolve(context))
                    .isEqualTo("Continue")

                activity.finish()
            }
        }
    }

    @Test
    fun `Verify bottom sheet expands on start`() {
        runActivityScenario {
            it.onActivity {
                composeTestRule
                    .onNodeWithTag(BottomSheetContentTestTag)
                    .assertIsDisplayed()
            }
        }
    }

    @Test
    fun `Verify selecting a payment method closes the sheet`() {
        val args = PAYMENT_OPTIONS_CONTRACT_ARGS.updateState(isGooglePayReady = true)

        runActivityScenario(args) { scenario ->
            scenario.onActivity {
                composeTestRule
                    .onNodeWithTag("${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_Google Pay")
                    .performClick()
            }

            composeTestRule.waitForIdle()

            val result = PaymentOptionResult.fromIntent(scenario.getResult().resultData)
            assertThat(result).isEqualTo(
                PaymentOptionResult.Succeeded(
                    paymentSelection = PaymentSelection.GooglePay,
                    paymentMethods = emptyList(),
                )
            )
        }
    }

    @Test
    fun `notes visibility is set correctly`() {
        val usBankAccount = PaymentMethodFixtures.US_BANK_ACCOUNT

        val label = usBankAccount.getLabel()?.resolve(context)
        val mandateText = "By continuing, you agree to authorize payments pursuant to these terms."

        val args = PAYMENT_OPTIONS_CONTRACT_ARGS.updateState(
            paymentMethods = listOf(usBankAccount),
            isGooglePayReady = true,
        )

        runActivityScenario(args) {
            it.onActivity {
                composeTestRule
                    .onNodeWithTag("${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_$label")
                    .performClick()

                composeTestRule
                    .onNodeWithText(mandateText)
                    .assertIsDisplayed()

                composeTestRule
                    .onNodeWithTag("${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_Google Pay")
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
                assertThat(activity.continueButton.isVisible).isTrue()
                assertThat(activity.continueButton.defaultTintList).isEqualTo(
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
    fun `Reports payment method selection in payment method form screen`() {
        val args = PAYMENT_OPTIONS_CONTRACT_ARGS.updateState(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp", "ideal"),
            ),
            paymentMethods = emptyList(),
        )

        runActivityScenario(args) {
            it.onActivity {
                composeTestRule
                    .onNodeWithTag(TEST_TAG_LIST + PaymentMethod.Type.CashAppPay.code)
                    .performClick()

                composeTestRule.waitForIdle()
            }
        }

        // We don't want the initial selection to be reported, as it's not a user selection
        verify(eventReporter, never()).onSelectPaymentMethod(
            code = eq(PaymentMethod.Type.Card.code),
        )

        verify(eventReporter).onSelectPaymentMethod(
            code = eq(PaymentMethod.Type.CashAppPay.code),
        )
    }

    @Test
    fun `mandate text is shown below primary button when showAbove is false`() {
        val args = PAYMENT_OPTIONS_CONTRACT_ARGS.updateState(
            paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )
        runActivityScenario(args) { scenario ->
            scenario.onActivity { activity ->
                val viewModel = activity.viewModel
                val text = "some text"
                val mandateNode = composeTestRule.onNode(hasText(text))
                val primaryButtonNode = composeTestRule
                    .onNodeWithTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG)

                viewModel.mandateHandler.updateMandateText(text.resolvableString, false)
                mandateNode.assertIsDisplayed()

                val mandatePosition = mandateNode.fetchSemanticsNode().positionInRoot.y
                val primaryButtonPosition = primaryButtonNode.fetchSemanticsNode().positionInRoot.y
                assertThat(mandatePosition).isGreaterThan(primaryButtonPosition)

                viewModel.mandateHandler.updateMandateText(null, false)
                mandateNode.assertDoesNotExist()
            }
        }
    }

    @Test
    fun `mandate text is shown above primary button when showAbove is true`() {
        val args = PAYMENT_OPTIONS_CONTRACT_ARGS.updateState(
            paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )
        runActivityScenario(args) { scenario ->
            scenario.onActivity { activity ->
                val viewModel = activity.viewModel
                val text = "some text"
                val mandateNode = composeTestRule.onNode(hasText(text))
                val primaryButtonNode = composeTestRule
                    .onNodeWithTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG)

                viewModel.mandateHandler.updateMandateText(text.resolvableString, true)
                mandateNode.assertIsDisplayed()

                val mandatePosition = mandateNode.fetchSemanticsNode().positionInRoot.y
                val primaryButtonPosition = primaryButtonNode.fetchSemanticsNode().positionInRoot.y
                assertThat(mandatePosition).isLessThan(primaryButtonPosition)

                viewModel.mandateHandler.updateMandateText(null, true)
                mandateNode.assertDoesNotExist()
            }
        }
    }

    @Test
    fun `mandate text is shown above primary button when in vertical mode`() {
        val args = PAYMENT_OPTIONS_CONTRACT_ARGS.updateState(
            paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            config = PAYMENT_OPTIONS_CONTRACT_ARGS.state.config.copy(
                paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
            )
        )
        runActivityScenario(args) { scenario ->
            scenario.onActivity { activity ->
                val viewModel = activity.viewModel
                val text = "some text"
                val mandateNode = composeTestRule.onNode(hasText(text))
                val primaryButtonNode = composeTestRule
                    .onNodeWithTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG)

                viewModel.mandateHandler.updateMandateText(text.resolvableString, false)
                mandateNode.assertIsDisplayed()

                val mandatePosition = mandateNode.fetchSemanticsNode().positionInRoot.y
                val primaryButtonPosition = primaryButtonNode.fetchSemanticsNode().positionInRoot.y
                assertThat(mandatePosition).isLessThan(primaryButtonPosition)

                viewModel.mandateHandler.updateMandateText(null, false)
                mandateNode.assertDoesNotExist()
            }
        }
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

        val viewModel = TestViewModelFactory.create(
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
        ) { linkHandler, savedStateHandle ->
            PaymentOptionsViewModel(
                args = args,
                eventReporter = eventReporter,
                customerRepository = FakeCustomerRepository(),
                workContext = testDispatcher,
                savedStateHandle = savedStateHandle,
                linkHandler = linkHandler,
                cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
                editInteractorFactory = mock()
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
