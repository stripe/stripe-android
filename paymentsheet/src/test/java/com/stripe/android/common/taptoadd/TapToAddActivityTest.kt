package com.stripe.android.common.taptoadd

import android.app.Activity.RESULT_OK
import android.app.Application
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.rule.IntentsRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.TestFactory
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.LinkSignupModeResult
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.RetryRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.tta.testing.TapToAddCardAddedPage
import com.stripe.android.tta.testing.TapToAddCardCollectionTestHelper
import com.stripe.android.tta.testing.TapToAddConfirmationPage
import com.stripe.android.tta.testing.TapToAddConfirmationTestHelper
import com.stripe.android.tta.testing.TapToAddDelayPage
import com.stripe.android.tta.testing.TapToAddErrorPage
import com.stripe.android.tta.testing.TapToAddLinkTestHelper
import com.stripe.android.tta.testing.TerminalTestDelegate
import com.stripe.android.utils.PaymentElementCallbackTestRule
import com.stripe.android.utils.PaymentLauncherContractArgsCvcMatcher
import com.stripe.android.view.ActivityStarter
import com.stripe.stripeterminal.external.models.TerminalErrorCode
import com.stripe.stripeterminal.external.models.TerminalException
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import com.stripe.android.core.R as StripeCoreR
import com.stripe.android.paymentsheet.R as PaymentSheetR

@OptIn(TapToAddPreview::class)
@RunWith(RobolectricTestRunner::class)
class TapToAddActivityTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val composeTestRule = createEmptyComposeRule()
    private val featureFlagTestRule = FeatureFlagTestRule(FeatureFlags.enableTapToAdd, true)
    private val composeCleanupRule = createComposeCleanupRule()
    private val terminalWrapperTestRule = TerminalWrapperTestRule()
    private val imageLoaderTestRule = TapToAddStripeImageLoaderTestRule()
    private val intentsRule = IntentsRule()
    private val paymentElementCallbackTestRule = PaymentElementCallbackTestRule()
    private val networkRule = NetworkRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(composeCleanupRule)
        .around(composeTestRule)
        .around(networkRule)
        .around(terminalWrapperTestRule)
        .around(featureFlagTestRule)
        .around(paymentElementCallbackTestRule)
        .around(imageLoaderTestRule)
        .around(PaymentConfigurationTestRule(applicationContext))
        .around(RetryRule(3))
        .around(intentsRule)

    private val linkHelper = TapToAddLinkTestHelper(composeTestRule, networkRule)
    private val confirmationHelper = TapToAddConfirmationTestHelper(composeTestRule)
    private val cardCollectionHelper = TapToAddCardCollectionTestHelper(networkRule) {
        terminalWrapperTestRule.delegate
    }
    private val cardArtTestHelper = TapToAddCardArtTestHelper(imageLoaderTestRule)
    private val cardAddedPage = TapToAddCardAddedPage(composeTestRule, linkHelper)
    private val confirmationPage = TapToAddConfirmationPage(composeTestRule)
    private val delayPage = TapToAddDelayPage(composeTestRule)
    private val errorPage = TapToAddErrorPage(composeTestRule)

    @Test
    fun successInContinueMode() = runScenario(
        mode = TapToAddMode.Continue,
    ) {
        val info = cardCollectionHelper.enqueueSuccessfulTapToCollectFlow(shouldValidate = true)

        enqueueCallbacks(CreateIntentResult.Success(info.setupIntentClientSecret))

        launch { activityScenario ->
            cardCollectionHelper.assertSuccessfulCardCollection(info)

            waitForIdle()

            cardAddedPage.assertShown()
            cardAddedPage.advancePastScreen()

            waitForIdle()

            assertTapToAddResult(
                expectedResult = TapToAddResult.Continue(PaymentSelection.Saved(info.cardPaymentMethod)),
                activityScenario = activityScenario,
            )
        }
    }

    @Test
    fun successWithLinkInlineSignupInContinueMode() = runScenario(
        mode = TapToAddMode.Continue,
        metadata = PaymentMethodMetadataFactory.create(
            isTapToAddSupported = true,
            hasCustomerConfiguration = true,
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION.copy(
                    customerInfo = LinkConfiguration.CustomerInfo(
                        name = null,
                        email = null,
                        phone = null,
                        billingCountryCode = null
                    )
                ),
                loginState = LinkState.LoginState.LoggedOut,
                signupModeResult = LinkSignupModeResult.Enabled(
                    mode = LinkSignupMode.InsteadOfSaveForFutureUse,
                    availableForSavedPaymentMethods = true,
                ),
            )
        )
    ) {
        val info = cardCollectionHelper.enqueueSuccessfulTapToCollectFlow(shouldValidate = true)

        enqueueCallbacks(CreateIntentResult.Success(info.setupIntentClientSecret))
        linkHelper.enqueueLookup()

        launch { activityScenario ->
            cardCollectionHelper.assertSuccessfulCardCollection(info)

            waitForIdle()

            cardAddedPage.assertShown(withLink = true)
            cardAddedPage.assertContinueButton(isEnabled = true)
            cardAddedPage.clickCheckboxToSaveWithLink()
            cardAddedPage.assertContinueButton(isEnabled = false)
            cardAddedPage.fillLinkInput()
            cardAddedPage.assertContinueButton(isEnabled = true)
            cardAddedPage.clickContinue()

            waitForIdle()

            assertTapToAddResult(
                expectedResult = TapToAddResult.Continue(
                    paymentSelection = PaymentSelection.Saved(
                        paymentMethod = info.cardPaymentMethod,
                        linkInput = linkHelper.input().toUserInput(),
                    ),
                ),
                activityScenario = activityScenario,
            )
        }
    }

    @Test
    fun successInCompleteMode() = runScenario(
        mode = TapToAddMode.Complete,
        metadata = PaymentMethodMetadataFactory.create(
            isTapToAddSupported = true,
            hasCustomerConfiguration = true,
            stripeIntent = PAYMENT_INTENT,
        ),
    ) {
        val info = cardCollectionHelper.enqueueSuccessfulTapToCollectFlow(shouldValidate = true)

        enqueueCallbacks(CreateIntentResult.Success(info.setupIntentClientSecret))

        confirmationHelper.intendingPaymentConfirmationToBeLaunched(
            InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED)
        )

        launch { activityScenario ->
            cardCollectionHelper.assertSuccessfulCardCollection(info)

            waitForIdle()

            confirmationPage.assertPrimaryButton(withLabel = "Pay $10.99", isEnabled = true)
            confirmationPage.clickPrimaryButton()

            waitForIdle()

            confirmationHelper.intendedPaymentConfirmationToBeLaunched()

            waitForIdle()

            assertTapToAddResult(
                expectedResult = TapToAddResult.Complete,
                activityScenario = activityScenario,
            )
        }
    }

    @Test
    fun failedPaymentInCompleteMode() = runScenario(
        mode = TapToAddMode.Complete,
        metadata = PaymentMethodMetadataFactory.create(
            isTapToAddSupported = true,
            hasCustomerConfiguration = true,
            stripeIntent = PAYMENT_INTENT,
        ),
    ) {
        val errorMessage = "Transaction failed due to some error!"

        cardCollectionHelper.enqueueSuccessfulTapToCollectFlow()

        confirmationHelper.intendingPaymentConfirmationToBeLaunched(
            result = InternalPaymentResult.Failed(
                throwable = LocalStripeException(
                    displayMessage = errorMessage,
                    analyticsValue = null,
                )
            )
        )

        launch {
            waitForIdle()

            confirmationPage.assertPrimaryButton(withLabel = "Pay $10.99", isEnabled = true)
            confirmationPage.clickPrimaryButton()

            waitForIdle()

            confirmationHelper.intendedPaymentConfirmationToBeLaunched()

            waitForIdle()

            confirmationPage.assertPrimaryButton(isEnabled = true)
            confirmationPage.assertErrorMessageShown(errorMessage)
        }
    }

    @Test
    fun successWithLinkInlineSignupInCompleteMode() = runScenario(
        mode = TapToAddMode.Complete,
        metadata = PaymentMethodMetadataFactory.create(
            isTapToAddSupported = true,
            hasCustomerConfiguration = true,
            stripeIntent = PAYMENT_INTENT,
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION.copy(
                    customerInfo = LinkConfiguration.CustomerInfo(
                        name = null,
                        email = null,
                        phone = null,
                        billingCountryCode = null
                    )
                ),
                loginState = LinkState.LoginState.LoggedOut,
                signupModeResult = LinkSignupModeResult.Enabled(
                    mode = LinkSignupMode.InsteadOfSaveForFutureUse,
                    availableForSavedPaymentMethods = true,
                ),
            )
        )
    ) {
        val info = cardCollectionHelper.enqueueSuccessfulTapToCollectFlow()

        linkHelper.enqueueLookup()
        linkHelper.enqueueSignup()
        linkHelper.enqueueCreatePaymentDetailsFromPaymentMethod(info.cardPaymentMethod.id)

        confirmationHelper.intendingPaymentConfirmationToBeLaunched(
            InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED)
        )

        launch { activityScenario ->
            waitForIdle()

            cardAddedPage.assertShown(withLink = true)
            cardAddedPage.assertContinueButton(isEnabled = true)
            cardAddedPage.clickCheckboxToSaveWithLink()
            cardAddedPage.assertContinueButton(isEnabled = false)
            cardAddedPage.fillLinkInput()
            cardAddedPage.assertContinueButton(isEnabled = true)
            cardAddedPage.clickContinue()

            waitForIdle()

            delayPage.advancePastScreen()

            waitForIdle()

            confirmationPage.assertPrimaryButton(isEnabled = true)
            confirmationPage.clickPrimaryButton()

            waitForIdle()

            confirmationHelper.intendedPaymentConfirmationToBeLaunched()

            waitForIdle()

            assertTapToAddResult(
                expectedResult = TapToAddResult.Complete,
                activityScenario = activityScenario,
            )
        }
    }

    @Test
    fun successWithCvcRecollection() = runScenario(
        mode = TapToAddMode.Complete,
        metadata = PaymentMethodMetadataFactory.create(
            isTapToAddSupported = true,
            hasCustomerConfiguration = true,
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION,
        )
    ) {
        val info = cardCollectionHelper.enqueueSuccessfulTapToCollectFlow()

        enqueueCallbacks(CreateIntentResult.Success(info.setupIntentClientSecret))

        confirmationHelper.intendingPaymentConfirmationToBeLaunched(
            InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED)
        )

        launch { activityScenario ->
            cardCollectionHelper.assertSuccessfulCardCollection(info)

            waitForIdle()

            confirmationPage.assertPrimaryButton(withLabel = "Pay $10.99", isEnabled = false)
            confirmationPage.assertCvcRecollectionFieldShown()
            confirmationPage.fillCvc("123")
            confirmationPage.assertPrimaryButton(isEnabled = true)
            confirmationPage.clickPrimaryButton()

            waitForIdle()

            confirmationHelper.intendedPaymentConfirmationToBeLaunched(
                hasExtra("extra_args", PaymentLauncherContractArgsCvcMatcher("123"))
            )

            waitForIdle()

            assertTapToAddResult(
                expectedResult = TapToAddResult.Complete,
                activityScenario = activityScenario,
            )
        }
    }

    @Test
    fun canceledDuringTerminalTapFlow() = runScenario(
        mode = TapToAddMode.Complete,
    ) {
        terminalWrapperTestRule.delegate.setScenario(
            TerminalTestDelegate.Scenario(
                collectSetupIntentPaymentMethodResult = TerminalTestDelegate.SetupIntentResult.Failure(
                    exception = TerminalException(
                        errorCode = TerminalErrorCode.CANCELED,
                        errorMessage = "Canceled!",
                    )
                )
            )
        )

        launch { activityScenario ->
            waitForIdle()

            assertTapToAddResult(
                expectedResult = TapToAddResult.Canceled(paymentSelection = null),
                activityScenario = activityScenario,
            )
        }
    }

    @Test
    fun canceledFromCardAddedScreen() = runScenario(
        mode = TapToAddMode.Continue,
        metadata = PaymentMethodMetadataFactory.create(
            isTapToAddSupported = true,
            hasCustomerConfiguration = true,
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION.copy(
                    customerInfo = LinkConfiguration.CustomerInfo(
                        name = null,
                        email = null,
                        phone = null,
                        billingCountryCode = null
                    )
                ),
                loginState = LinkState.LoginState.LoggedOut,
                signupModeResult = LinkSignupModeResult.Enabled(
                    mode = LinkSignupMode.InsteadOfSaveForFutureUse,
                    availableForSavedPaymentMethods = true,
                ),
            )
        )
    ) {
        val info = cardCollectionHelper.enqueueSuccessfulTapToCollectFlow()

        launch { activityScenario ->
            waitForIdle()

            cardAddedPage.assertShown(withLink = true)
            cardAddedPage.clickCloseButton()

            waitForIdle()

            assertTapToAddResult(
                expectedResult = TapToAddResult.Canceled(
                    paymentSelection = PaymentSelection.Saved(paymentMethod = info.cardPaymentMethod),
                ),
                activityScenario = activityScenario,
            )
        }
    }

    @Test
    fun canceledFromConfirmationScreen() = runScenario(
        mode = TapToAddMode.Complete,
        metadata = PaymentMethodMetadataFactory.create(
            isTapToAddSupported = true,
            hasCustomerConfiguration = true,
            stripeIntent = PAYMENT_INTENT,
        ),
    ) {
        val info = cardCollectionHelper.enqueueSuccessfulTapToCollectFlow()

        launch { activityScenario ->
            waitForIdle()

            confirmationPage.assertPrimaryButton(isEnabled = true)
            confirmationPage.clickCloseButton()

            waitForIdle()

            assertTapToAddResult(
                expectedResult = TapToAddResult.Canceled(
                    paymentSelection = PaymentSelection.Saved(paymentMethod = info.cardPaymentMethod),
                ),
                activityScenario = activityScenario,
            )
        }
    }

    @Test
    fun userShownErrorFromCardCollection() = errorTest(
        errorCode = TerminalErrorCode.CARD_READ_TIMED_OUT,
        expectedTitle = applicationContext.getString(
            StripeCoreR.string.stripe_timed_out
        ),
        expectedAction = applicationContext.getString(
            PaymentSheetR.string.stripe_tap_to_add_timed_out_error_action
        ),
        expectedResult = TapToAddResult.Canceled(paymentSelection = null)
    )

    @Test
    fun userShownUnsupportedErrorFromCardCollection() = errorTest(
        errorCode = TerminalErrorCode.TAP_TO_PAY_UNSUPPORTED_DEVICE,
        expectedTitle = applicationContext.getString(
            PaymentSheetR.string.stripe_tap_to_add_unsupported_device_error_title
        ),
        expectedAction = applicationContext.getString(
            PaymentSheetR.string.stripe_tap_to_add_unsupported_device_error_action
        ),
        expectedResult = TapToAddResult.UnsupportedDevice
    )

    private fun errorTest(
        errorCode: TerminalErrorCode,
        expectedTitle: String,
        expectedAction: String,
        expectedResult: TapToAddResult,
    ) = runScenario(
        mode = TapToAddMode.Complete
    ) {
        terminalWrapperTestRule.delegate.setScenario(
            TerminalTestDelegate.Scenario(
                collectSetupIntentPaymentMethodResult = TerminalTestDelegate.SetupIntentResult.Failure(
                    exception = TerminalException(
                        errorCode = errorCode,
                        errorMessage = "Error!",
                    )
                )
            )
        )

        enqueueCallbacks(CreateIntentResult.Success("seti_123_secret_123"))

        launch { activityScenario ->
            assertThat(terminalWrapperTestRule.delegate.awaitIsInitializedCall()).isNotNull()
            assertThat(terminalWrapperTestRule.delegate.awaitSetTapToPayUxConfigurationCall()).isNotNull()
            assertThat(terminalWrapperTestRule.delegate.awaitConnectedReaderCall()).isNotNull()
            assertThat(terminalWrapperTestRule.delegate.awaitRetrieveSetupIntentCall()).isNotNull()
            assertThat(terminalWrapperTestRule.delegate.awaitCollectSetupIntentPaymentMethodCall()).isNotNull()
            assertThat(terminalWrapperTestRule.delegate.awaitSupportsReadersOfTypeCall()).isNotNull()

            waitForIdle()

            errorPage.assertShown(
                expectedTitle = expectedTitle,
                expectedAction = expectedAction,
            )
            errorPage.clickCloseButton()

            waitForIdle()

            assertTapToAddResult(
                expectedResult = expectedResult,
                activityScenario = activityScenario,
            )
        }
    }

    private fun runScenario(
        mode: TapToAddMode,
        metadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            isTapToAddSupported = true,
            hasCustomerConfiguration = true,
        ),
        createIntentResult: CreateIntentResult = CreateIntentResult.Success(clientSecret = "seti_123_secret_123"),
        block: Scenario.() -> Unit,
    ) {
        enqueueCallbacks(createIntentResult)

        block(
            Scenario(
                launch = { block ->
                    ActivityScenario.launchActivityForResult<TapToAddActivity>(
                        TapToAddContract.createIntent(
                            context = applicationContext,
                            input = TapToAddContract.Args(
                                mode = mode,
                                eventMode = EventReporter.Mode.Custom,
                                paymentMethodMetadata = metadata,
                                paymentElementCallbackIdentifier = PAYMENT_ELEMENT_CALLBACK_IDENTIFIER,
                                productUsage = emptySet()
                            )
                        )
                    ).use { scenario ->
                        scenario.onActivity {
                            runTest {
                                cardArtTestHelper.assertCardArtAssetPreloads()
                                block(scenario)
                            }
                        }
                    }
                }
            )
        )
    }

    private fun assertTapToAddResult(
        expectedResult: TapToAddResult,
        activityScenario: ActivityScenario<TapToAddActivity>
    ) {
        assertThat(activityScenario.result.resultCode).isEqualTo(RESULT_OK)

        @Suppress("DEPRECATION")
        val actualResult = activityScenario.result.resultData.extras
            ?.getParcelable<TapToAddResult>(ActivityStarter.Result.EXTRA)

        assertThat(actualResult).isEqualTo(expectedResult)
    }

    private fun waitForIdle() {
        composeTestRule.waitForIdle()
    }

    private fun enqueueCallbacks(
        createCardPresentSetupIntentResult: CreateIntentResult
    ) {
        PaymentElementCallbackReferences[PAYMENT_ELEMENT_CALLBACK_IDENTIFIER] =
            PaymentElementCallbacks.Builder()
                .createCardPresentSetupIntentCallback {
                    createCardPresentSetupIntentResult
                }
                .build()
    }

    private fun TapToAddLinkTestHelper.Input.toUserInput(): UserInput {
        return UserInput.SignUp(
            email = email,
            phone = phone,
            name = name,
            country = "US",
            consentAction = SignUpConsentAction.Checkbox,
        )
    }

    private class Scenario(
        val launch: (
            block: suspend (activityScenario: ActivityScenario<TapToAddActivity>) -> Unit
        ) -> Unit
    )

    private companion object {
        const val PAYMENT_ELEMENT_CALLBACK_IDENTIFIER = "mpe1"
        val PAYMENT_INTENT = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            amount = 1099
        )
    }
}
