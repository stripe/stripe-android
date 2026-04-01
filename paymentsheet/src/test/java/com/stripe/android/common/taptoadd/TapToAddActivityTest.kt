package com.stripe.android.common.taptoadd

import android.app.Activity.RESULT_OK
import android.app.Application
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.rule.IntentsRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.TestFactory
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.RetryRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.utils.PaymentElementCallbackTestRule
import com.stripe.android.view.ActivityStarter
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

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

    private val linkHelper = TapToAddLinkHelper(composeTestRule, networkRule)
    private val confirmationHelper = TapToAddConfirmationTestHelper(composeTestRule)
    private val cardCollectionHelper =
        TapToAddCardCollectionTestHelper(networkRule, imageLoaderTestRule, terminalWrapperTestRule)
    private val cardAddedPage = TapToAddCardAddedPage(composeTestRule, linkHelper)
    private val confirmationPage = TapToAddConfirmationPage(composeTestRule)

    @Test
    fun successInContinueMode() = runScenario(
        mode = TapToAddMode.Continue,
    ) {
        val info = cardCollectionHelper.enqueueSuccessfulTapToCollectFlow()

        launch { activityScenario ->
            cardCollectionHelper.assertSuccessfulCardCollection(info)

            waitForIdle()

            cardAddedPage.assertShown(withLink = false)
            cardAddedPage.assertContinueButton(isEnabled = true)
            cardAddedPage.clickContinue()

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
                signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
            )
        )
    ) {
        val info = cardCollectionHelper.enqueueSuccessfulTapToCollectFlow()

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
                        linkInput = linkHelper.userInput(),
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
        val info = cardCollectionHelper.enqueueSuccessfulTapToCollectFlow()

        confirmationHelper.intendingPaymentConfirmationToBeLaunched(
            InternalPaymentResult.Completed(PaymentIntentFixtures.PI_SUCCEEDED)
        )

        launch { activityScenario ->
            cardCollectionHelper.assertSuccessfulCardCollection(info)

            waitForIdle()

            cardAddedPage.assertShown(withLink = false)
            cardAddedPage.assertContinueButton(isEnabled = true)
            cardAddedPage.clickContinue()

            waitForIdle()

            confirmationPage.assertPrimaryButton(label = "Pay $10.99", isEnabled = true)
            confirmationPage.clickPrimaryButton(label = "Pay $10.99")

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
                signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
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

            confirmationPage.assertPrimaryButton(label = "Pay $10.99", isEnabled = true)
            confirmationPage.clickPrimaryButton(label = "Pay $10.99")

            waitForIdle()

            confirmationHelper.intendedPaymentConfirmationToBeLaunched()

            waitForIdle()

            assertTapToAddResult(
                expectedResult = TapToAddResult.Complete,
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
        block: Scenario.() -> Unit,
    ) {
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
                                paymentElementCallbackIdentifier =
                                    cardCollectionHelper.paymentElementCallbackIdentifier,
                                productUsage = emptySet()
                            )
                        )
                    ).use { scenario ->
                        scenario.onActivity {
                            runTest {
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

    private class Scenario(
        val launch: (
            block: suspend (activityScenario: ActivityScenario<TapToAddActivity>) -> Unit
        ) -> Unit
    )

    private companion object {
        val PAYMENT_INTENT = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            amount = 1099
        )
    }
}
