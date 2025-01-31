package com.stripe.android.paymentelement.confirmation.linkinline

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.CountryCode
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.analytics.FakeLinkAnalyticsHelper
import com.stripe.android.link.analytics.LinkAnalyticsHelper
import com.stripe.android.link.injection.LinkComponent
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardParams
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.CvcCheck
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.wallets.Wallet
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.FakeConfirmationOption
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.asLaunch
import com.stripe.android.paymentelement.confirmation.asNew
import com.stripe.android.paymentelement.confirmation.asNextStep
import com.stripe.android.paymentelement.confirmation.asSaved
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.utils.DummyActivityResultCaller
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.RecordingLinkStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock

internal class LinkInlineSignupConfirmationDefinitionTest {
    @Test
    fun `'key' should be 'LinkInlineSignup'`() {
        val definition = createLinkInlineSignupConfirmationDefinition()

        assertThat(definition.key).isEqualTo("LinkInlineSignup")
    }

    @Test
    fun `'option' return casted 'LinkInlineSignupConfirmationOption'`() {
        val definition = createLinkInlineSignupConfirmationDefinition()

        val option = createLinkInlineSignupConfirmationOption()

        assertThat(definition.option(option)).isEqualTo(option)
    }

    @Test
    fun `'option' return null for unknown option`() {
        val definition = createLinkInlineSignupConfirmationDefinition()

        assertThat(definition.option(FakeConfirmationOption())).isNull()
    }

    @Test
    fun `'createLauncher' should create launcher properly`() = test {
        val definition = createLinkInlineSignupConfirmationDefinition()

        val activityResultCaller = DummyActivityResultCaller.noOp()
        val onResult: (LinkInlineSignupConfirmationDefinition.Result) -> Unit = {}

        val launcher = definition.createLauncher(
            activityResultCaller = activityResultCaller,
            onResult = onResult,
        )

        assertThat(launcher.onResult).isEqualTo(onResult)
    }

    @Test
    fun `'action' should skip signup if signup failed on 'SignedOut' account status`() =
        testSkippedLinkSignupOnSignInError(
            accountStatus = AccountStatus.SignedOut,
        )

    @Test
    fun `'action' should skip signup if signup failed on 'Error' account status`() =
        testSkippedLinkSignupOnSignInError(
            accountStatus = AccountStatus.Error,
        )

    @Test
    fun `'action' should skip signup and return 'Launch' on 'VerificationStarted' account status`() =
        testSkippedLinkSignupOnAccountStatus(
            accountStatus = AccountStatus.VerificationStarted,
        )

    @Test
    fun `'action' should skip signup and return 'Launch' on 'NeedsVerification' account status`() =
        testSkippedLinkSignupOnAccountStatus(
            accountStatus = AccountStatus.NeedsVerification,
        )

    @Test
    fun `'action' should return 'Launch' with new option with null SFU if no reuse request`() =
        testSuccessfulSignupWithNewCard(
            saveOption = LinkInlineSignupConfirmationOption.PaymentMethodSaveOption.NoRequest,
            expectedSetupForFutureUsage = null,
            expectedShouldSave = false,
        )

    @Test
    fun `'action' should return 'Launch' with new option with 'Blank' SFU if requested no reuse`() =
        testSuccessfulSignupWithNewCard(
            saveOption = LinkInlineSignupConfirmationOption.PaymentMethodSaveOption.RequestedNoReuse,
            expectedSetupForFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.Blank,
            expectedShouldSave = false,
        )

    @Test
    fun `'action' should return 'Launch' with new option with 'OffSession' SFU if requested reuse`() =
        testSuccessfulSignupWithNewCard(
            saveOption = LinkInlineSignupConfirmationOption.PaymentMethodSaveOption.RequestedReuse,
            expectedSetupForFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession,
            expectedShouldSave = true,
        )

    @Test
    fun `'action' should return 'Launch' with saved option with 'Blank' SFU if no reuse request`() =
        testSuccessfulSignupWithSavedLinkCard(
            saveOption = LinkInlineSignupConfirmationOption.PaymentMethodSaveOption.NoRequest,
            expectedSetupForFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.Blank,
        )

    @Test
    fun `'action' should return 'Launch' with saved option with 'Blank' SFU if requested no reuse`() =
        testSuccessfulSignupWithSavedLinkCard(
            saveOption = LinkInlineSignupConfirmationOption.PaymentMethodSaveOption.RequestedNoReuse,
            expectedSetupForFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.Blank,
        )

    @Test
    fun `'action' should return 'Launch' with saved option with 'OffSession' SFU if requested reuse`() =
        testSuccessfulSignupWithSavedLinkCard(
            saveOption = LinkInlineSignupConfirmationOption.PaymentMethodSaveOption.RequestedReuse,
            expectedSetupForFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession,
        )

    @Test
    fun `'action' should skip & return 'Launch' if input is sign in`() = test(
        initialAccountStatus = AccountStatus.Verified,
    ) {
        val confirmationOption = createLinkInlineSignupConfirmationOption()

        val action = definition.action(
            confirmationOption = confirmationOption.copy(
                userInput = UserInput.SignIn(email = "email@email.com"),
            ),
            confirmationParameters = CONFIRMATION_PARAMETERS,
        )

        val getAccountStatusFlowCall = coordinatorScenario.getAccountStatusFlowCalls.awaitItem()

        assertThat(getAccountStatusFlowCall.configuration).isEqualTo(confirmationOption.linkConfiguration)

        assertThat(action).isInstanceOf<ConfirmationDefinition.Action.Launch<Unit>>()

        val launchAction = action.asLaunch()

        validateSkippedLaunchAction(confirmationOption, launchAction)

        assertThat(analyticsScenario.onLinkPopupSkippedCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `'action' should skip & return 'Launch' if failed to attach card`() = test(
        attachNewCardToAccountResult = Result.failure(IllegalStateException("Failed!")),
        initialAccountStatus = AccountStatus.Verified,
    ) {
        val confirmationOption = createLinkInlineSignupConfirmationOption()

        val action = definition.action(
            confirmationOption = confirmationOption,
            confirmationParameters = CONFIRMATION_PARAMETERS,
        )

        val getAccountStatusFlowCall = coordinatorScenario.getAccountStatusFlowCalls.awaitItem()

        assertThat(getAccountStatusFlowCall.configuration).isEqualTo(confirmationOption.linkConfiguration)

        val attachNewCardToAccountCall = coordinatorScenario.attachNewCardToAccountCalls.awaitItem()

        assertThat(attachNewCardToAccountCall.configuration).isEqualTo(confirmationOption.linkConfiguration)
        assertThat(attachNewCardToAccountCall.paymentMethodCreateParams)
            .isEqualTo(confirmationOption.createParams)

        assertThat(action).isInstanceOf<ConfirmationDefinition.Action.Launch<Unit>>()

        val launchAction = action.asLaunch()

        validateSkippedLaunchAction(confirmationOption, launchAction)
    }

    @Test
    fun `'action' should return 'Launch' after successful sign-in & attach`() = test(
        attachNewCardToAccountResult = Result.success(
            LinkPaymentDetails.Saved(
                paymentDetails = ConsumerPaymentDetails.Card(
                    id = "pm_1",
                    last4 = "4242",
                    isDefault = false,
                    expiryYear = 2030,
                    expiryMonth = 4,
                    brand = CardBrand.Visa,
                    cvcCheck = CvcCheck.Pass,
                    billingAddress = null,
                ),
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            )
        ),
        signInResult = Result.success(true),
        initialAccountStatus = AccountStatus.SignedOut,
        accountStatusOnSignIn = AccountStatus.Verified,
    ) {
        val confirmationOption = createLinkInlineSignupConfirmationOption()

        val action = definition.action(
            confirmationOption = confirmationOption,
            confirmationParameters = CONFIRMATION_PARAMETERS,
        )

        val firstGetAccountStatusFlowCall = coordinatorScenario.getAccountStatusFlowCalls.awaitItem()

        assertThat(firstGetAccountStatusFlowCall.configuration).isEqualTo(confirmationOption.linkConfiguration)

        val signInCall = coordinatorScenario.signInCalls.awaitItem()

        assertThat(signInCall.configuration).isEqualTo(confirmationOption.linkConfiguration)
        assertThat(signInCall.userInput).isEqualTo(confirmationOption.userInput)

        val secondGetAccountStatusFlowCall = coordinatorScenario.getAccountStatusFlowCalls.awaitItem()

        assertThat(secondGetAccountStatusFlowCall.configuration).isEqualTo(confirmationOption.linkConfiguration)

        val attachNewCardToAccountCall = coordinatorScenario.attachNewCardToAccountCalls.awaitItem()

        assertThat(attachNewCardToAccountCall.configuration).isEqualTo(confirmationOption.linkConfiguration)
        assertThat(attachNewCardToAccountCall.paymentMethodCreateParams)
            .isEqualTo(confirmationOption.createParams)

        assertThat(action).isInstanceOf<ConfirmationDefinition.Action.Launch<Unit>>()

        val launchAction = action.asLaunch()

        val nextConfirmationOption = launchAction.launcherArguments.nextConfirmationOption

        assertThat(nextConfirmationOption).isInstanceOf<PaymentMethodConfirmationOption.Saved>()

        val savedConfirmationOption = nextConfirmationOption.asSaved()

        assertThat(savedConfirmationOption.optionsParams).isEqualTo(
            PaymentMethodOptionsParams.Card(
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.Blank,
            )
        )

        val paymentMethod = savedConfirmationOption.paymentMethod

        assertThat(paymentMethod.id).isEqualTo("pm_1")
        assertThat(paymentMethod.type).isEqualTo(PaymentMethod.Type.Card)
        assertThat(paymentMethod.card?.last4).isEqualTo("4242")
        assertThat(paymentMethod.card?.wallet).isEqualTo(Wallet.LinkWallet(dynamicLast4 = "4242"))

        assertThat(launchAction.receivesResultInProcess).isTrue()
        assertThat(launchAction.deferredIntentConfirmationType).isNull()

        assertThat(storeScenario.markAsUsedCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `'launch' should immediately call 'onResult'`() = test {
        val definition = createLinkInlineSignupConfirmationDefinition()
        val launcher = LinkInlineSignupConfirmationDefinition.Launcher(onResultScenario.onResult)

        val nextOption = PaymentMethodConfirmationOption.Saved(
            paymentMethod = PaymentMethodFactory.card(random = true),
            optionsParams = PaymentMethodOptionsParams.Card(
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OnSession,
            ),
        )

        definition.launch(
            confirmationOption = createLinkInlineSignupConfirmationOption(),
            confirmationParameters = CONFIRMATION_PARAMETERS,
            launcher = launcher,
            arguments = LinkInlineSignupConfirmationDefinition.LauncherArguments(
                nextConfirmationOption = nextOption,
            ),
        )

        val onResultCall = onResultScenario.onResultCalls.awaitItem()

        assertThat(onResultCall.result.nextConfirmationOption).isEqualTo(nextOption)
    }

    @Test
    fun `'toResult' should be 'NextStep' on result`() = test {
        val definition = createLinkInlineSignupConfirmationDefinition(linkStore = storeScenario.linkStore)

        val nextOption = PaymentMethodConfirmationOption.Saved(
            paymentMethod = PaymentMethodFactory.card(random = true),
            optionsParams = PaymentMethodOptionsParams.Card(
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OnSession,
            ),
        )

        val result = definition.toResult(
            confirmationOption = createLinkInlineSignupConfirmationOption(),
            confirmationParameters = CONFIRMATION_PARAMETERS,
            result = LinkInlineSignupConfirmationDefinition.Result(
                nextConfirmationOption = nextOption,
            ),
            deferredIntentConfirmationType = null,
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.NextStep>()

        val nextStepResult = result.asNextStep()

        assertThat(nextStepResult.confirmationOption).isEqualTo(nextOption)
        assertThat(nextStepResult.parameters).isEqualTo(CONFIRMATION_PARAMETERS)
    }

    private fun testSkippedLinkSignupOnSignInError(
        accountStatus: AccountStatus
    ) = test {
        val userInput = UserInput.SignIn(email = "email@email.com")
        val confirmationOption = createLinkInlineSignupConfirmationOption(
            userInput = userInput,
        )

        actionTest(
            attachNewCardToAccountResult = Result.failure(IllegalStateException("Should not be used!")),
            accountStatus = accountStatus,
            signInResult = Result.failure(IllegalStateException("Something went wrong!")),
            confirmationOption = confirmationOption,
        ) { launchAction ->
            val signInCall = coordinatorScenario.signInCalls.awaitItem()

            assertThat(signInCall.configuration).isEqualTo(confirmationOption.linkConfiguration)
            assertThat(signInCall.userInput).isEqualTo(userInput)

            validateSkippedLaunchAction(confirmationOption, launchAction)
        }
    }

    private fun testSkippedLinkSignupOnAccountStatus(
        accountStatus: AccountStatus
    ) = test {
        val confirmationOption = createLinkInlineSignupConfirmationOption()

        actionTest(
            attachNewCardToAccountResult = Result.failure(IllegalStateException("Should not be used!")),
            accountStatus = accountStatus,
            signInResult = Result.success(true),
            confirmationOption = confirmationOption,
        ) { launchAction ->
            validateSkippedLaunchAction(confirmationOption, launchAction)

            assertThat(analyticsScenario.onLinkPopupSkippedCalls.awaitItem()).isNotNull()
        }
    }

    private fun testSuccessfulSignupWithNewCard(
        saveOption: LinkInlineSignupConfirmationOption.PaymentMethodSaveOption,
        expectedSetupForFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?,
        expectedShouldSave: Boolean,
    ) {
        val expectedCreateParams = PaymentMethodCreateParams.createCard(
            CardParams(
                number = "4242424242424242",
                expMonth = 7,
                expYear = 2025,
            )
        )

        val confirmationOption = createLinkInlineSignupConfirmationOption(
            saveOption = saveOption,
        )

        actionTest(
            attachNewCardToAccountResult = Result.success(
                LinkPaymentDetails.New(
                    paymentDetails = ConsumerPaymentDetails.Card(
                        id = "pm_1",
                        last4 = "4242",
                        isDefault = false,
                        expiryYear = 2030,
                        expiryMonth = 4,
                        brand = CardBrand.Visa,
                        cvcCheck = CvcCheck.Pass,
                        billingAddress = null,
                    ),
                    paymentMethodCreateParams = expectedCreateParams,
                    originalParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                )
            ),
            accountStatus = AccountStatus.Verified,
            signInResult = Result.success(true),
            confirmationOption = confirmationOption,
        ) { launchAction ->
            val attachNewCardToAccountCall = coordinatorScenario.attachNewCardToAccountCalls.awaitItem()

            assertThat(attachNewCardToAccountCall.configuration).isEqualTo(confirmationOption.linkConfiguration)
            assertThat(attachNewCardToAccountCall.paymentMethodCreateParams)
                .isEqualTo(confirmationOption.createParams)

            val nextConfirmationOption = launchAction.launcherArguments.nextConfirmationOption

            assertThat(nextConfirmationOption).isInstanceOf<PaymentMethodConfirmationOption.New>()

            val newConfirmationOption = nextConfirmationOption.asNew()

            assertThat(newConfirmationOption.createParams).isEqualTo(expectedCreateParams)
            assertThat(newConfirmationOption.optionsParams).isEqualTo(
                PaymentMethodOptionsParams.Card(
                    setupFutureUsage = expectedSetupForFutureUsage,
                )
            )
            assertThat(newConfirmationOption.shouldSave).isEqualTo(expectedShouldSave)

            assertThat(launchAction.receivesResultInProcess).isTrue()
            assertThat(launchAction.deferredIntentConfirmationType).isNull()

            assertThat(storeScenario.markAsUsedCalls.awaitItem()).isNotNull()
        }
    }

    private fun testSuccessfulSignupWithSavedLinkCard(
        saveOption: LinkInlineSignupConfirmationOption.PaymentMethodSaveOption,
        expectedSetupForFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage,
    ) {
        val confirmationOption = createLinkInlineSignupConfirmationOption(
            saveOption = saveOption,
        )

        actionTest(
            attachNewCardToAccountResult = Result.success(
                LinkPaymentDetails.Saved(
                    paymentDetails = ConsumerPaymentDetails.Card(
                        id = "pm_1",
                        last4 = "4242",
                        isDefault = false,
                        expiryYear = 2030,
                        expiryMonth = 4,
                        brand = CardBrand.Visa,
                        cvcCheck = CvcCheck.Pass,
                        billingAddress = null,
                    ),
                    paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                )
            ),
            signInResult = Result.success(true),
            accountStatus = AccountStatus.Verified,
            confirmationOption = confirmationOption,
        ) { launchAction ->
            val attachNewCardToAccountCall = coordinatorScenario.attachNewCardToAccountCalls.awaitItem()

            assertThat(attachNewCardToAccountCall.configuration).isEqualTo(confirmationOption.linkConfiguration)
            assertThat(attachNewCardToAccountCall.paymentMethodCreateParams)
                .isEqualTo(confirmationOption.createParams)

            val nextConfirmationOption = launchAction.launcherArguments.nextConfirmationOption

            assertThat(nextConfirmationOption).isInstanceOf<PaymentMethodConfirmationOption.Saved>()

            val savedConfirmationOption = nextConfirmationOption.asSaved()

            assertThat(savedConfirmationOption.optionsParams).isEqualTo(
                PaymentMethodOptionsParams.Card(
                    setupFutureUsage = expectedSetupForFutureUsage,
                )
            )

            val paymentMethod = savedConfirmationOption.paymentMethod

            assertThat(paymentMethod.id).isEqualTo("pm_1")
            assertThat(paymentMethod.type).isEqualTo(PaymentMethod.Type.Card)
            assertThat(paymentMethod.card?.last4).isEqualTo("4242")
            assertThat(paymentMethod.card?.wallet).isEqualTo(Wallet.LinkWallet(dynamicLast4 = "4242"))

            assertThat(launchAction.receivesResultInProcess).isTrue()
            assertThat(launchAction.deferredIntentConfirmationType).isNull()

            assertThat(storeScenario.markAsUsedCalls.awaitItem()).isNotNull()
        }
    }

    private fun actionTest(
        attachNewCardToAccountResult: Result<LinkPaymentDetails>,
        signInResult: Result<Boolean>,
        accountStatus: AccountStatus,
        confirmationOption: LinkInlineSignupConfirmationOption = createLinkInlineSignupConfirmationOption(),
        test: suspend Scenario.(
            action: ConfirmationDefinition.Action.Launch<LinkInlineSignupConfirmationDefinition.LauncherArguments>
        ) -> Unit
    ) = test(
        attachNewCardToAccountResult = attachNewCardToAccountResult,
        signInResult = signInResult,
        initialAccountStatus = accountStatus,
        accountStatusOnSignIn = AccountStatus.Verified,
    ) {
        val action = definition.action(
            confirmationOption = confirmationOption,
            confirmationParameters = CONFIRMATION_PARAMETERS,
        )

        val getAccountStatusFlowCall = coordinatorScenario.getAccountStatusFlowCalls.awaitItem()

        assertThat(getAccountStatusFlowCall.configuration).isEqualTo(confirmationOption.linkConfiguration)

        assertThat(action).isInstanceOf<ConfirmationDefinition.Action.Launch<Unit>>()

        test(action.asLaunch())
    }

    private fun validateSkippedLaunchAction(
        confirmationOption: LinkInlineSignupConfirmationOption,
        launchAction: ConfirmationDefinition.Action.Launch<LinkInlineSignupConfirmationDefinition.LauncherArguments>
    ) {
        val nextConfirmationOption = launchAction.launcherArguments.nextConfirmationOption

        assertThat(nextConfirmationOption).isInstanceOf<PaymentMethodConfirmationOption.New>()

        val nextNewConfirmationOption = nextConfirmationOption.asNew()

        assertThat(nextNewConfirmationOption.createParams).isEqualTo(confirmationOption.createParams)
        assertThat(nextNewConfirmationOption.optionsParams).isEqualTo(confirmationOption.optionsParams)

        assertThat(launchAction.receivesResultInProcess).isTrue()
        assertThat(launchAction.deferredIntentConfirmationType).isNull()
    }

    private fun test(
        attachNewCardToAccountResult: Result<LinkPaymentDetails> = Result.success(
            LinkPaymentDetails.New(
                paymentDetails = ConsumerPaymentDetails.Card(
                    id = "pm_123",
                    last4 = "4242",
                    expiryYear = 2024,
                    expiryMonth = 4,
                    brand = CardBrand.DinersClub,
                    cvcCheck = CvcCheck.Fail,
                    isDefault = false,
                    billingAddress = ConsumerPaymentDetails.BillingAddress(
                        countryCode = CountryCode.US,
                        postalCode = "42424"
                    )
                ),
                paymentMethodCreateParams = mock(),
                originalParams = mock(),
            )
        ),
        signInResult: Result<Boolean> = Result.success(true),
        initialAccountStatus: AccountStatus = AccountStatus.Verified,
        accountStatusOnSignIn: AccountStatus = AccountStatus.Verified,
        hasUsedLink: Boolean = false,
        test: suspend Scenario.() -> Unit
    ) = runTest {
        RecordingLinkConfigurationCoordinator.test(
            attachNewCardToAccountResult = attachNewCardToAccountResult,
            signInResult = signInResult,
            initialAccountStatus = initialAccountStatus,
            accountStatusOnSignIn = accountStatusOnSignIn,
        ) {
            val coordinatorScenario = this

            RecordingLinkAnalyticsHelper.test {
                val analyticsScenario = this

                RecordingOnLinkInlineResult.test {
                    val onResultScenario = this

                    RecordingLinkStore.test(hasUsedLink) {
                        val linkStoreScenario = this

                        test(
                            Scenario(
                                definition = createLinkInlineSignupConfirmationDefinition(
                                    linkConfigurationCoordinator = coordinatorScenario.coordinator,
                                    linkAnalyticsHelper = analyticsScenario.helper,
                                    linkStore = linkStoreScenario.linkStore,
                                ),
                                coordinatorScenario = coordinatorScenario,
                                storeScenario = linkStoreScenario,
                                analyticsScenario = analyticsScenario,
                                onResultScenario = onResultScenario,
                            )
                        )
                    }
                }
            }
        }
    }

    private fun createLinkInlineSignupConfirmationDefinition(
        linkConfigurationCoordinator: LinkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
        linkAnalyticsHelper: LinkAnalyticsHelper = FakeLinkAnalyticsHelper(),
        linkStore: LinkStore = RecordingLinkStore.noOp(),
    ): LinkInlineSignupConfirmationDefinition {
        return LinkInlineSignupConfirmationDefinition(
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            linkAnalyticsHelper = linkAnalyticsHelper,
            linkStore = linkStore,
        )
    }

    private fun createLinkInlineSignupConfirmationOption(
        createParams: PaymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
        saveOption: LinkInlineSignupConfirmationOption.PaymentMethodSaveOption =
            LinkInlineSignupConfirmationOption.PaymentMethodSaveOption.NoRequest,
        userInput: UserInput = UserInput.SignUp(
            email = "email@email.com",
            phone = "1234567890",
            country = "CA",
            name = "John Doe",
            consentAction = SignUpConsentAction.Checkbox,
        )
    ): LinkInlineSignupConfirmationOption {
        return LinkInlineSignupConfirmationOption(
            createParams = createParams,
            optionsParams = null,
            saveOption = saveOption,
            linkConfiguration = LinkConfiguration(
                stripeIntent = PaymentIntentFactory.create(),
                merchantName = "Merchant Inc.",
                merchantCountryCode = "CA",
                customerInfo = LinkConfiguration.CustomerInfo(
                    name = "Jphn Doe",
                    email = "johndoe@email.com",
                    phone = "+1123456789",
                    billingCountryCode = "CA"
                ),
                shippingDetails = null,
                passthroughModeEnabled = false,
                flags = mapOf(),
                cardBrandChoice = null,
                useAttestationEndpointsForLink = false,
                suppress2faModal = false,
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = "pi_123_secret_123",
                ),
                elementsSessionId = "session_1234"
            ),
            userInput = userInput,
        )
    }

    private class Scenario(
        val definition: LinkInlineSignupConfirmationDefinition,
        val coordinatorScenario: RecordingLinkConfigurationCoordinator.Scenario,
        val storeScenario: RecordingLinkStore.Scenario,
        val analyticsScenario: RecordingLinkAnalyticsHelper.Scenario,
        val onResultScenario: RecordingOnLinkInlineResult.Scenario,
    )

    private class RecordingOnLinkInlineResult private constructor() {
        data class OnResultCall(
            val result: LinkInlineSignupConfirmationDefinition.Result,
        )

        class Scenario(
            val onResult: (LinkInlineSignupConfirmationDefinition.Result) -> Unit,
            val onResultCalls: ReceiveTurbine<OnResultCall>,
        )

        companion object {
            suspend fun test(
                test: suspend Scenario.() -> Unit
            ) {
                val onResultCalls = Turbine<OnResultCall>()

                test(
                    Scenario(
                        onResult = {
                            onResultCalls.add(OnResultCall(it))
                        },
                        onResultCalls = onResultCalls,
                    )
                )

                onResultCalls.ensureAllEventsConsumed()
            }
        }
    }

    private class RecordingLinkAnalyticsHelper private constructor() : FakeLinkAnalyticsHelper() {
        private val onLinkPopupSkippedCalls = Turbine<Unit>()

        override fun onLinkPopupSkipped() {
            onLinkPopupSkippedCalls.add(Unit)
        }

        class Scenario(
            val helper: LinkAnalyticsHelper,
            val onLinkPopupSkippedCalls: ReceiveTurbine<Unit>,
        )

        companion object {
            suspend fun test(
                test: suspend Scenario.() -> Unit
            ) {
                val helper = RecordingLinkAnalyticsHelper()

                test(
                    Scenario(
                        helper = helper,
                        onLinkPopupSkippedCalls = helper.onLinkPopupSkippedCalls,
                    )
                )

                helper.onLinkPopupSkippedCalls.ensureAllEventsConsumed()
            }
        }
    }

    private class RecordingLinkConfigurationCoordinator private constructor(
        private val attachNewCardToAccountResult: Result<LinkPaymentDetails>,
        private val signInResult: Result<Boolean>,
        initialAccountStatus: AccountStatus,
        private val accountStatusOnSignIn: AccountStatus,
    ) : LinkConfigurationCoordinator {
        private val getAccountStatusFlowCalls = Turbine<GetAccountStatusFlowCall>()
        private val signInCalls = Turbine<SignInCall>()
        private val attachNewCardToAccountCalls = Turbine<AttachNewCardToAccountCall>()

        private val accountStatusFlow = MutableStateFlow(initialAccountStatus)

        override val emailFlow: StateFlow<String?>
            get() {
                throw NotImplementedError()
            }

        override fun getComponent(configuration: LinkConfiguration): LinkComponent {
            throw NotImplementedError()
        }

        override fun getAccountStatusFlow(configuration: LinkConfiguration): Flow<AccountStatus> {
            getAccountStatusFlowCalls.add(GetAccountStatusFlowCall(configuration))

            return accountStatusFlow
        }

        override suspend fun signInWithUserInput(
            configuration: LinkConfiguration,
            userInput: UserInput,
        ): Result<Boolean> {
            signInCalls.add(SignInCall(configuration, userInput))

            accountStatusFlow.value = accountStatusOnSignIn

            return signInResult
        }

        override suspend fun attachNewCardToAccount(
            configuration: LinkConfiguration,
            paymentMethodCreateParams: PaymentMethodCreateParams,
        ): Result<LinkPaymentDetails> {
            attachNewCardToAccountCalls.add(AttachNewCardToAccountCall(configuration, paymentMethodCreateParams))

            return attachNewCardToAccountResult
        }

        override suspend fun logOut(configuration: LinkConfiguration): Result<ConsumerSession> {
            throw NotImplementedError()
        }

        data class GetAccountStatusFlowCall(
            val configuration: LinkConfiguration
        )

        data class SignInCall(
            val configuration: LinkConfiguration,
            val userInput: UserInput,
        )

        data class AttachNewCardToAccountCall(
            val configuration: LinkConfiguration,
            val paymentMethodCreateParams: PaymentMethodCreateParams,
        )

        class Scenario(
            val coordinator: LinkConfigurationCoordinator,
            val getAccountStatusFlowCalls: ReceiveTurbine<GetAccountStatusFlowCall>,
            val signInCalls: ReceiveTurbine<SignInCall>,
            val attachNewCardToAccountCalls: ReceiveTurbine<AttachNewCardToAccountCall>,
        )

        companion object {
            suspend fun test(
                attachNewCardToAccountResult: Result<LinkPaymentDetails>,
                signInResult: Result<Boolean>,
                initialAccountStatus: AccountStatus,
                accountStatusOnSignIn: AccountStatus,
                test: suspend Scenario.() -> Unit,
            ) {
                val coordinator = RecordingLinkConfigurationCoordinator(
                    attachNewCardToAccountResult = attachNewCardToAccountResult,
                    signInResult = signInResult,
                    initialAccountStatus = initialAccountStatus,
                    accountStatusOnSignIn = accountStatusOnSignIn,
                )

                test(
                    Scenario(
                        coordinator = coordinator,
                        getAccountStatusFlowCalls = coordinator.getAccountStatusFlowCalls,
                        signInCalls = coordinator.signInCalls,
                        attachNewCardToAccountCalls = coordinator.attachNewCardToAccountCalls
                    )
                )

                coordinator.getAccountStatusFlowCalls.ensureAllEventsConsumed()
                coordinator.signInCalls.ensureAllEventsConsumed()
                coordinator.attachNewCardToAccountCalls.ensureAllEventsConsumed()
            }
        }
    }

    private companion object {
        private val PAYMENT_INTENT = PaymentIntentFactory.create()

        private val CONFIRMATION_PARAMETERS = ConfirmationDefinition.Parameters(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_123_secret_123",
            ),
            intent = PAYMENT_INTENT,
            appearance = PaymentSheet.Appearance(),
            shippingDetails = AddressDetails(),
        )
    }
}
