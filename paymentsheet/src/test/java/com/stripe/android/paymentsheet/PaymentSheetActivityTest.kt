package com.stripe.android.paymentsheet

import android.content.Context
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.common.ui.BottomSheetContentTestTag
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
import com.stripe.android.paymentsheet.databinding.StripePrimaryButtonBinding
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddAnotherPaymentMethod
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.SelectSavedPaymentMethods
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.ui.GOOGLE_PAY_BUTTON_TEST_TAG
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
        hostActivityLauncher = mock(),
        statusBarColor = null,
    )

    private val paymentLauncher: StripePaymentLauncher by lazy {
        paymentLauncherFactory.create(
            publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
        ) as StripePaymentLauncher
    }

    private val stripePaymentLauncherAssistedFactory = mock<StripePaymentLauncherAssistedFactory> {
        on { create(any(), any(), any(), any(), any()) } doReturn paymentLauncher
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
    fun `bottom sheet expands on start`() {
        val scenario = activityScenario()
        scenario.launchForResult(intent).onActivity {
            composeTestRule
                .onNodeWithTag(BottomSheetContentTestTag)
                .assertIsDisplayed()
        }
    }

    private val PaymentSheetActivity.buyButton: PrimaryButton
        get() = findViewById(R.id.primary_button)

    @Test
    fun `disables primary button when editing`() {
        val viewModel = createViewModel(
            initialPaymentSelection = PaymentSelection.Saved(PAYMENT_METHODS.last()),
        )

        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity { activity ->
            assertThat(activity.buyButton.isEnabled).isTrue()

            viewModel.toggleEditing()
            assertThat(activity.buyButton.isEnabled).isFalse()
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

            activity.buyButton.callOnClick()

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

            activity.buyButton.callOnClick()

            composeTestRule
                .onNodeWithText(error)
                .assertDoesNotExist()
        }
    }

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
                .onNodeWithTag(GOOGLE_PAY_BUTTON_TEST_TAG)
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
                .onNodeWithTag(GOOGLE_PAY_BUTTON_TEST_TAG)
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
            assertThat(activity.buyButton.isVisible).isTrue()
            assertThat(activity.buyButton.isEnabled).isFalse()

            // Update to Google Pay
            viewModel.checkoutWithGooglePay()
            assertThat(activity.buyButton.isVisible).isTrue()
            assertThat(activity.buyButton.isEnabled).isFalse()
            assertThat(viewModel.contentVisible.value).isFalse()

            viewModel.onGooglePayResult(GooglePayPaymentMethodLauncher.Result.Canceled)
            assertThat(viewModel.contentVisible.value).isTrue()

            // Update to saved card
            viewModel.updateSelection(
                PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            )
            assertThat(activity.buyButton.isVisible).isTrue()
            assertThat(activity.buyButton.isEnabled).isTrue()

            // Back to empty/invalid card
            viewModel.updateSelection(null)
            assertThat(activity.buyButton.isVisible).isTrue()
            assertThat(activity.buyButton.isEnabled).isFalse()

            // New valid card
            viewModel.updateSelection(
                PaymentSelection.New.Card(
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    CardBrand.Visa,
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse
                )
            )
            assertThat(activity.buyButton.isVisible).isTrue()
            assertThat(activity.buyButton.isEnabled).isTrue()
        }
    }

    @Test
    fun `Reports canceled result when exiting sheet via back press`() = runTest(testDispatcher) {
        val scenario = activityScenario()

        scenario.launchForResult(intent).onActivity {
            pressBack()
        }

        composeTestRule.waitForIdle()

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
        val card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
            card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.card?.copy(
                networks = PaymentMethod.Card.Networks(
                    available = setOf("visa", "cartes_bancaires"),
                )
            )
        )

        val paymentMethods = listOf(card)
        val viewModel = createViewModel(paymentMethods = paymentMethods)
        val scenario = activityScenario(viewModel)

        viewModel.currentScreen.test {
            scenario.launch(intent)
            assertThat(awaitItem()).isEqualTo(SelectSavedPaymentMethods)

            viewModel.transitionToAddPaymentScreen()
            assertThat(awaitItem()).isEqualTo(AddAnotherPaymentMethod)

            pressBack()
            assertThat(awaitItem()).isEqualTo(SelectSavedPaymentMethods)

            viewModel.modifyPaymentMethod(card)
            assertThat(awaitItem()).isInstanceOf(PaymentSheetScreen.EditPaymentMethod::class.java)

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
            assertThat(activity.buyButton.isEnabled)
                .isTrue()

            activity.buyButton.performClick()

            assertThat(activity.buyButton.isEnabled)
                .isFalse()
        }
    }

    @Test
    fun `Verify Ready state updates the buy button label`() {
        val viewModel = createViewModel()
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity { activity ->
            viewModel.viewState.value = PaymentSheetViewState.Reset(null)

            val buyBinding = StripePrimaryButtonBinding.bind(activity.buyButton)

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

            assertThat(activity.buyButton.externalLabel)
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

            assertThat(finishProcessingCalled).isTrue()
        }
    }

    @Test
    fun `google pay flow updates the scroll view before and after`() {
        val viewModel = createViewModel(isGooglePayAvailable = true)
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity {
            viewModel.checkoutIdentifier = CheckoutIdentifier.SheetTopGooglePay

            composeTestRule
                .onNodeWithTag(GOOGLE_PAY_BUTTON_TEST_TAG)
                .performClick()

            composeTestRule.waitForIdle()

            assertThat(viewModel.contentVisible.value).isEqualTo(false)

            viewModel.onGooglePayResult(GooglePayPaymentMethodLauncher.Result.Canceled)
            assertThat(viewModel.contentVisible.value).isEqualTo(true)
        }
    }

    @Test
    fun `Verify ProcessResult state closes the sheet`() {
        val viewModel = createViewModel()
        val scenario = activityScenario(viewModel)

        scenario.launchForResult(intent).onActivity {
            viewModel.onFinish()
        }

        composeTestRule.waitForIdle()

        val result = contract.parseResult(
            scenario.getResult().resultCode,
            scenario.getResult().resultData
        )

        assertThat(result).isEqualTo(PaymentSheetResult.Completed)
    }

    @Test
    fun `successful payment should dismiss bottom sheet`() {
        val viewModel = createViewModel()
        val scenario = activityScenario(viewModel)

        scenario.launchForResult(intent).onActivity {
            viewModel.checkoutIdentifier = CheckoutIdentifier.SheetBottomBuy
            viewModel.onPaymentResult(PaymentResult.Completed)
        }

        composeTestRule.waitForIdle()

        val result = contract.parseResult(
            scenario.getResult().resultCode,
            scenario.getResult().resultData
        )

        assertThat(result).isEqualTo(PaymentSheetResult.Completed)
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
            viewModel.updateMandateText(text, false)

            composeTestRule
                .onNodeWithText(text)
                .assertIsDisplayed()
        }
    }

    @Test
    fun `mandate text is shown below primary button when showAbove is false`() {
        val viewModel = createViewModel()
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity {
            val text = "some text"
            val mandateNode = composeTestRule.onNode(hasText(text))
            val primaryButtonNode = composeTestRule
                .onNodeWithTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG)

            viewModel.updateMandateText(text, false)
            mandateNode.assertIsDisplayed()

            val mandatePosition = mandateNode.fetchSemanticsNode().positionInRoot.y
            val primaryButtonPosition = primaryButtonNode.fetchSemanticsNode().positionInRoot.y
            assertThat(mandatePosition).isGreaterThan(primaryButtonPosition)

            viewModel.updateMandateText(null, false)
            mandateNode.assertDoesNotExist()
        }
    }

    @Test
    fun `mandate text is shown above primary button when showAbove is true`() {
        val viewModel = createViewModel()
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity {
            val text = "some text"
            val mandateNode = composeTestRule.onNode(hasText(text))
            val primaryButtonNode = composeTestRule
                .onNodeWithTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG)

            viewModel.updateMandateText(text, true)
            mandateNode.assertIsDisplayed()

            val mandatePosition = mandateNode.fetchSemanticsNode().positionInRoot.y
            val primaryButtonPosition = primaryButtonNode.fetchSemanticsNode().positionInRoot.y
            assertThat(mandatePosition).isLessThan(primaryButtonPosition)

            viewModel.updateMandateText(null, true)
            mandateNode.assertDoesNotExist()
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
            config = PaymentSheet.Configuration(
                merchantDisplayName = "Some name",
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
            assertThat(activity.buyButton.externalLabel).isEqualTo("Pay CA\$99.99")
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
            assertThat(activity.buyButton.externalLabel).isEqualTo("Pay")
            testDispatcher.scheduler.advanceTimeBy(250)
            assertThat(activity.buyButton.externalLabel).isEqualTo("Pay CA\$99.99")
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

        verify(eventReporter).onPressConfirmButton()
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
                mock(),
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
                editInteractorFactory = FakeEditPaymentMethodInteractor.Factory
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
        private val PAYMENT_METHODS = listOf(PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD)
    }
}
