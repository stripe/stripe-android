package com.stripe.android.paymentsheet

import android.content.Context
import android.os.Build
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.CardBrandFilter
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContractV2
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.LinkButtonTestTag
import com.stripe.android.model.CardBrand
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
import com.stripe.android.paymentsheet.cvcrecollection.FakeCvcRecollectionHandler
import com.stripe.android.paymentsheet.databinding.StripePrimaryButtonBinding
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddAnotherPaymentMethod
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.SelectSavedPaymentMethods
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.FakeBacsMandateConfirmationLauncher
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.Args
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionInteractor
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.state.WalletsProcessingState
import com.stripe.android.paymentsheet.ui.GOOGLE_PAY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.PAYMENT_SHEET_EDIT_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.SHEET_NAVIGATION_BUTTON_TAG
import com.stripe.android.paymentsheet.ui.TEST_TAG_LIST
import com.stripe.android.paymentsheet.ui.TEST_TAG_MODIFY_BADGE
import com.stripe.android.paymentsheet.ui.TEST_TAG_REMOVE_BADGE
import com.stripe.android.paymentsheet.utils.FakeUserFacingLogger
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.TEST_TAG_DIALOG_CONFIRM_BUTTON
import com.stripe.android.uicore.elements.bottomsheet.BottomSheetContentTestTag
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.FakeCustomerRepository
import com.stripe.android.utils.FakeIntentConfirmationInterceptor
import com.stripe.android.utils.FakePaymentElementLoader
import com.stripe.android.utils.InjectableActivityScenario
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import com.stripe.android.utils.TestUtils.viewModelFactoryFor
import com.stripe.android.utils.injectableActivityScenario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
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

    private val cvcRecollectionHandler = FakeCvcRecollectionHandler()

    private val contract = PaymentSheetContractV2()

    private val intent = contract.createIntent(
        context,
        PaymentSheetContractV2.Args(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
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

            startEditing()
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

            startEditing()

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

            viewModel.onError(error.resolvableString)

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

            viewModel.onError(error.resolvableString)

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

            viewModel.onError(error.resolvableString)

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
        val googlePayListener = viewModel.captureGooglePayListener()

        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity {
            val error = "some error"

            composeTestRule
                .onNodeWithText(error)
                .assertDoesNotExist()

            viewModel.onError(error.resolvableString)

            composeTestRule
                .onNodeWithText(error)
                .assertExists()

            val newSelection = PaymentSelection.Saved(paymentMethod = paymentMethods.last())
            viewModel.updateSelection(newSelection)

            composeTestRule
                .onNodeWithTag(GOOGLE_PAY_BUTTON_TEST_TAG)
                .performClick()

            googlePayListener.onActivityResult(GooglePayPaymentMethodLauncher.Result.Canceled)

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

            viewModel.onError(error.resolvableString)

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

            viewModel.onError(error.resolvableString)

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
    fun `selected saved PM does not change after selecting a new non-saved PM`() {
        val paymentMethods = PAYMENT_METHODS.take(1)
        val viewModel = createViewModel(paymentMethods = paymentMethods)
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity {
            composeTestRule.onNodeWithTag(
                "SAVED_PAYMENT_METHOD_CARD_TEST_TAG_···· 4242",
                useUnmergedTree = true,
            ).assertIsSelected()

            composeTestRule.onNodeWithTag(
                PaymentOptionsItem.AddCard.viewType.name
            ).performClick()

            composeTestRule.onNodeWithTag(
                SHEET_NAVIGATION_BUTTON_TAG
            ).performClick()

            composeTestRule.onNodeWithTag(
                "SAVED_PAYMENT_METHOD_CARD_TEST_TAG_···· 4242",
                useUnmergedTree = true,
            ).assertIsSelected()
        }
    }

    @Test
    fun `removing last selected saved PM clears out saved payment selection`() {
        val paymentMethods = PAYMENT_METHODS.take(1)
        val viewModel = createViewModel(paymentMethods = paymentMethods)
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity { activity ->
            startEditing()

            composeTestRule.onNodeWithTag(
                TEST_TAG_REMOVE_BADGE,
                useUnmergedTree = true,
            ).performClick()

            composeTestRule.onNodeWithTag(TEST_TAG_DIALOG_CONFIRM_BUTTON).performClick()

            composeTestRule.waitForIdle()

            assertThat(viewModel.navigationHandler.currentScreen.value)
                .isInstanceOf(PaymentSheetScreen.AddFirstPaymentMethod::class.java)

            composeTestRule.onNodeWithTag(
                TEST_TAG_LIST + "card",
            ).onChildren().assertAny(isSelected())
            assertThat(activity.buyButton.isEnabled).isFalse()
        }
    }

    @Test
    fun `updates buy button state on add payment`() {
        Dispatchers.setMain(testDispatcher)

        val viewModel = createViewModel(paymentMethods = emptyList())
        val googlePayListener = viewModel.captureGooglePayListener()
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

            googlePayListener.onActivityResult(GooglePayPaymentMethodLauncher.Result.Canceled)
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
        val viewModel = createViewModel(
            paymentMethods = paymentMethods,
            cbcEligibility = CardBrandChoiceEligibility.Eligible(
                preferredNetworks = listOf(CardBrand.Visa, CardBrand.CartesBancaires)
            ),
        )
        val scenario = activityScenario(viewModel)

        viewModel.navigationHandler.currentScreen.test {
            scenario.launch(intent)
            assertThat(awaitItem()).isInstanceOf<SelectSavedPaymentMethods>()

            viewModel.transitionToAddPaymentScreen()
            assertThat(awaitItem()).isInstanceOf<AddAnotherPaymentMethod>()

            pressBack()
            assertThat(awaitItem()).isInstanceOf<SelectSavedPaymentMethods>()

            startEditing()
            composeTestRule.onNodeWithTag(TEST_TAG_MODIFY_BADGE).performClick()
            assertThat(awaitItem()).isInstanceOf<PaymentSheetScreen.EditPaymentMethod>()

            pressBack()
            assertThat(awaitItem()).isInstanceOf<SelectSavedPaymentMethods>()

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

            composeTestRule.waitForIdle()
            assertThat(activity.buyButton.externalLabel?.resolve(context))
                .isEqualTo(activity.getString(R.string.stripe_paymentsheet_primary_button_processing))
        }
    }

    @Test
    fun `Verify FinishProcessing state calls the callback`() {
        Dispatchers.setMain(testDispatcher)
        val viewModel = createViewModel()
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity {
            val countDownLatch = CountDownLatch(1)

            viewModel.checkoutIdentifier = CheckoutIdentifier.SheetBottomBuy

            viewModel.viewState.value = PaymentSheetViewState.FinishProcessing {
                countDownLatch.countDown()
            }

            countDownLatch.await(5, TimeUnit.SECONDS)
        }
    }

    @Test
    fun `Verify FinishProcessing state calls the callback on google pay view state observer`() {
        Dispatchers.setMain(testDispatcher)

        val viewModel = createViewModel(isGooglePayAvailable = true)
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity {
            val countDownLatch = CountDownLatch(1)

            viewModel.checkoutIdentifier = CheckoutIdentifier.SheetTopWallet

            viewModel.viewState.value = PaymentSheetViewState.FinishProcessing {
                countDownLatch.countDown()
            }

            countDownLatch.await(5, TimeUnit.SECONDS)
        }
    }

    @Test
    fun `google pay flow updates the scroll view before and after`() {
        val viewModel = createViewModel(isGooglePayAvailable = true)
        val googlePayListener = viewModel.captureGooglePayListener()
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity {
            viewModel.checkoutIdentifier = CheckoutIdentifier.SheetTopWallet

            composeTestRule
                .onNodeWithTag(GOOGLE_PAY_BUTTON_TEST_TAG)
                .performClick()

            composeTestRule.waitForIdle()

            assertThat(viewModel.walletsProcessingState.value).isEqualTo(WalletsProcessingState.Processing)
            assertThat(viewModel.contentVisible.value).isEqualTo(false)

            googlePayListener.onActivityResult(
                GooglePayPaymentMethodLauncher.Result.Completed(PAYMENT_METHODS.first())
            )

            assertThat(viewModel.walletsProcessingState.value).isEqualTo(WalletsProcessingState.Processing)
            assertThat(viewModel.contentVisible.value).isEqualTo(true)

            fakeIntentConfirmationInterceptor.enqueueCompleteStep()

            assertThat(viewModel.walletsProcessingState.value).isInstanceOf<WalletsProcessingState.Completed>()
            assertThat(viewModel.contentVisible.value).isEqualTo(true)
        }
    }

    @Test
    fun `link flow updates the payment sheet before and after`() {
        val viewModel = createViewModel(isLinkAvailable = true)
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity {
            viewModel.checkoutIdentifier = CheckoutIdentifier.SheetTopWallet

            composeTestRule
                .onNodeWithTag(LinkButtonTestTag)
                .performClick()

            composeTestRule.waitForIdle()

            assertThat(viewModel.walletsProcessingState.value).isEqualTo(WalletsProcessingState.Processing)

            viewModel.linkHandler.onLinkActivityResult(LinkActivityResult.Completed(PAYMENT_METHODS.first()))

            assertThat(viewModel.walletsProcessingState.value).isEqualTo(WalletsProcessingState.Processing)

            fakeIntentConfirmationInterceptor.enqueueCompleteStep()

            assertThat(viewModel.walletsProcessingState.value).isInstanceOf<WalletsProcessingState.Completed>()
        }
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
        ).isInstanceOf<PaymentSheetResult.Failed>()
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

            viewModel.checkoutIdentifier = CheckoutIdentifier.SheetTopWallet
            viewModel.viewState.value =
                PaymentSheetViewState.Reset(PaymentSheetViewState.UserErrorMessage(errorMessage.resolvableString))

            composeTestRule
                .onNodeWithText(errorMessage)
                .assertExists()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `CVC recollection adds CVC to PaymentMethodOptionsParams`() {
        cvcRecollectionHandler.cvcRecollectionEnabled = true
        cvcRecollectionHandler.requiresCVCRecollection = true
        val viewModel = createViewModel(
            paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION,
            paymentMethods = PAYMENT_METHODS.take(1)
        )
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity {
            composeTestRule.onNodeWithTag(
                "SAVED_PAYMENT_METHOD_CARD_TEST_TAG_···· 4242",
                useUnmergedTree = true,
            ).assertIsSelected()

            composeTestRule.waitUntilAtLeastOneExists(
                hasText("Confirm your CVC")
            )

            composeTestRule.onNodeWithText("CVC").performTextInput("123")

            viewModel.checkout()

            (viewModel.selection.value as PaymentSelection.Saved).let {
                (it.paymentMethodOptionsParams as PaymentMethodOptionsParams.Card).let { card ->
                    assertThat(card.cvc).isEqualTo("123")
                }
            }
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
                PaymentSheetViewState.Reset(PaymentSheetViewState.UserErrorMessage(errorMessage.resolvableString))

            composeTestRule
                .onNodeWithText(errorMessage)
                .assertExists()

            viewModel.checkout()

            composeTestRule
                .onNodeWithText(errorMessage)
                .assertDoesNotExist()

            fakeIntentConfirmationInterceptor.enqueueFailureStep(
                IllegalStateException(errorMessage),
                errorMessage
            )

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
            val text = StripeUiCoreR.string.stripe_paymentsheet_payment_method_us_bank_account.resolvableString
            viewModel.mandateHandler.updateMandateText(text, false)

            composeTestRule
                .onNodeWithText(text.resolve(context))
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

            viewModel.mandateHandler.updateMandateText(text.resolvableString, false)
            mandateNode.assertIsDisplayed()

            val mandatePosition = mandateNode.fetchSemanticsNode().positionInRoot.y
            val primaryButtonPosition = primaryButtonNode.fetchSemanticsNode().positionInRoot.y
            assertThat(mandatePosition).isGreaterThan(primaryButtonPosition)

            viewModel.mandateHandler.updateMandateText(null, false)
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

            viewModel.mandateHandler.updateMandateText(text.resolvableString, true)
            mandateNode.assertIsDisplayed()

            val mandatePosition = mandateNode.fetchSemanticsNode().positionInRoot.y
            val primaryButtonPosition = primaryButtonNode.fetchSemanticsNode().positionInRoot.y
            assertThat(mandatePosition).isLessThan(primaryButtonPosition)

            viewModel.mandateHandler.updateMandateText(null, true)
            mandateNode.assertDoesNotExist()
        }
    }

    @Test
    fun `mandate text is shown above primary button when in vertical mode`() {
        val args = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
            config = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY.config.copy(
                paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
            )
        )
        val viewModel = createViewModel(args = args)
        val scenario = activityScenario(viewModel)

        scenario.launch(intent).onActivity {
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

    @Test
    fun `Handles missing arguments correctly`() {
        val scenario = ActivityScenario.launchActivityForResult(PaymentSheetActivity::class.java)

        val result = contract.parseResult(
            scenario.result.resultCode,
            scenario.result.resultData,
        )

        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        assertThat(result).isInstanceOf<PaymentSheetResult.Failed>()
    }

    @Test
    fun `Handles invalid arguments correctly`() {
        val invalidCustomerConfig = PaymentSheet.CustomerConfiguration(
            id = "",
            ephemeralKeySecret = "",
        )

        val args = PaymentSheetContractV2.Args(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
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
        assertThat(result).isInstanceOf<PaymentSheetResult.Failed>()
    }

    @Test
    fun `Handles invalid client secret correctly`() {
        val args = PaymentSheetContractV2.Args(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(clientSecret = ""),
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
        assertThat(result).isInstanceOf<PaymentSheetResult.Failed>()
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
            assertThat(activity.buyButton.externalLabel?.resolve(context)).isEqualTo("Pay CA\$99.99")
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
            assertThat(activity.buyButton.externalLabel?.resolve(context)).isEqualTo("Pay")
            testDispatcher.scheduler.advanceTimeBy(250)
            assertThat(activity.buyButton.externalLabel?.resolve(context)).isEqualTo("Pay CA\$99.99")
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

        verify(eventReporter).onPressConfirmButton(any())
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
        initialPaymentSelection: PaymentSelection? = paymentMethods.firstOrNull()?.let { PaymentSelection.Saved(it) },
        args: PaymentSheetContractV2.Args = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY,
        cbcEligibility: CardBrandChoiceEligibility = CardBrandChoiceEligibility.Ineligible,
    ): PaymentSheetViewModel = runBlocking {
        TestViewModelFactory.create(
            linkConfigurationCoordinator = mock<LinkConfigurationCoordinator>().stub {
                onBlocking { getAccountStatusFlow(any()) }.thenReturn(flowOf(AccountStatus.SignedOut))
                on { emailFlow } doReturn stateFlowOf("email@email.com")
            },
        ) { linkHandler, savedStateHandle ->
            PaymentSheetViewModel(
                args = args,
                eventReporter = eventReporter,
                paymentElementLoader = FakePaymentElementLoader(
                    stripeIntent = paymentIntent,
                    customer = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(paymentMethods = paymentMethods),
                    isGooglePayAvailable = isGooglePayAvailable,
                    linkState = LinkState(
                        configuration = mock(),
                        loginState = LinkState.LoginState.LoggedOut,
                        signupMode = null,
                    ).takeIf { isLinkAvailable },
                    delay = loadDelay,
                    paymentSelection = initialPaymentSelection,
                    cbcEligibility = cbcEligibility,
                ),
                customerRepository = FakeCustomerRepository(paymentMethods),
                prefsRepository = FakePrefsRepository(),
                logger = Logger.noop(),
                workContext = testDispatcher,
                savedStateHandle = savedStateHandle,
                linkHandler = linkHandler,
                defaultConfirmationHandlerFactory = DefaultConfirmationHandler.Factory(
                    intentConfirmationInterceptor = fakeIntentConfirmationInterceptor,
                    savedStateHandle = savedStateHandle,
                    stripePaymentLauncherAssistedFactory = stripePaymentLauncherAssistedFactory,
                    bacsMandateConfirmationLauncherFactory = { FakeBacsMandateConfirmationLauncher() },
                    googlePayPaymentMethodLauncherFactory = googlePayPaymentMethodLauncherFactory,
                    paymentConfigurationProvider = { PaymentConfiguration(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY) },
                    statusBarColor = { args.statusBarColor },
                    errorReporter = FakeErrorReporter(),
                    logger = FakeUserFacingLogger(),
                ),
                cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
                editInteractorFactory = FakeEditPaymentMethodInteractor.Factory(),
                errorReporter = FakeErrorReporter(),
                cvcRecollectionHandler = cvcRecollectionHandler,
                cvcRecollectionInteractorFactory = object : CvcRecollectionInteractor.Factory {
                    override fun create(
                        args: Args,
                        processing: StateFlow<Boolean>,
                        coroutineScope: CoroutineScope,
                    ): CvcRecollectionInteractor {
                        return FakeCvcRecollectionInteractor()
                    }
                }
            )
        }
    }

    private fun PaymentSheetViewModel.captureGooglePayListener():
        ActivityResultCallback<GooglePayPaymentMethodLauncher.Result> {
        val mockActivityResultCaller = mock<ActivityResultCaller> {
            on {
                registerForActivityResult<
                    GooglePayPaymentMethodLauncherContractV2.Args,
                    GooglePayPaymentMethodLauncher.Result
                    >(any(), any())
            } doReturn mock()
        }

        registerFromActivity(mockActivityResultCaller, TestLifecycleOwner())

        val googlePayListenerCaptor =
            argumentCaptor<ActivityResultCallback<GooglePayPaymentMethodLauncher.Result>>()

        verify(mockActivityResultCaller).registerForActivityResult(
            any<GooglePayPaymentMethodLauncherContractV2>(),
            googlePayListenerCaptor.capture(),
        )

        return googlePayListenerCaptor.firstValue
    }

    private fun createGooglePayPaymentMethodLauncherFactory() =
        object : GooglePayPaymentMethodLauncherFactory {
            override fun create(
                lifecycleScope: CoroutineScope,
                config: GooglePayPaymentMethodLauncher.Config,
                readyCallback: GooglePayPaymentMethodLauncher.ReadyCallback,
                activityResultLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>,
                skipReadyCheck: Boolean,
                cardBrandFilter: CardBrandFilter
            ): GooglePayPaymentMethodLauncher {
                val googlePayPaymentMethodLauncher = mock<GooglePayPaymentMethodLauncher>()
                readyCallback.onReady(true)
                return googlePayPaymentMethodLauncher
            }
        }

    private fun startEditing() {
        composeTestRule.waitUntil {
            composeTestRule.onAllNodesWithTag(PAYMENT_SHEET_EDIT_BUTTON_TEST_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(PAYMENT_SHEET_EDIT_BUTTON_TEST_TAG).performClick()
    }

    private companion object {
        private val PAYMENT_INTENT = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        private val PAYMENT_METHODS = listOf(PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD)
    }
}
