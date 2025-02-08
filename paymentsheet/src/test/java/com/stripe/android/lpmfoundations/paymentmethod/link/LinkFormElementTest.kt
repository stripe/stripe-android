package com.stripe.android.lpmfoundations.paymentmethod.link

import android.util.Log
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.core.Logger
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.analytics.FakeLinkEventsReporter
import com.stripe.android.link.attestation.FakeLinkAttestationCheck
import com.stripe.android.link.attestation.LinkAttestationCheck
import com.stripe.android.link.gate.FakeLinkGate
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.injection.LinkComponent
import com.stripe.android.link.injection.LinkInlineSignupAssistedViewModelFactory
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.InlineSignupViewModel
import com.stripe.android.link.ui.inline.LINK_INLINE_SIGNUP_REMAINING_FIELDS_TEST_TAG
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.PaymentIntentFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.runner.RunWith
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class LinkFormElementTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `If initial user input is provided, should be displayed to the user when alongside SFU`() {
        val element = createLinkFormElement(
            signupMode = LinkSignupMode.AlongsideSaveForFutureUse,
            initialLinkUserInput = UserInput.SignUp(
                name = "John Doe",
                email = "email@email.com",
                phone = "+11234567890",
                country = "CA",
                consentAction = SignUpConsentAction.Checkbox,
            ),
        )

        composeTestRule.setContent {
            element.ComposeUI(enabled = true)
        }

        composeTestRule.waitForRemainingLinkFields()

        composeTestRule.hasLinkFieldWith(text = "John Doe")
        composeTestRule.hasLinkFieldWith(text = "email@email.com")
        composeTestRule.hasLinkFieldWith(text = "(123) 456-7890")
    }

    @Test
    fun `If initial user input is provided, should be expanded and displayed to the user when instead of SFU`() {
        val element = createLinkFormElement(
            signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
            initialLinkUserInput = UserInput.SignUp(
                name = "John Doe",
                email = "email@email.com",
                phone = "+11234567890",
                country = "CA",
                consentAction = SignUpConsentAction.Checkbox,
            ),
        )

        composeTestRule.setContent {
            element.ComposeUI(enabled = true)
        }

        composeTestRule.waitForRemainingLinkFields()

        composeTestRule.hasLinkFieldWith(text = "John Doe")
        composeTestRule.hasLinkFieldWith(text = "email@email.com")
        composeTestRule.hasLinkFieldWith(text = "(123) 456-7890")
    }

    private fun ComposeTestRule.hasLinkFieldWith(text: String) {
        onNodeWithText(text).assertExists()
    }

    private fun ComposeTestRule.waitForRemainingLinkFields() {
        waitUntil(timeoutMillis = 5000L) {
            onAllNodesWithTag(testTag = LINK_INLINE_SIGNUP_REMAINING_FIELDS_TEST_TAG)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun createLinkFormElement(
        signupMode: LinkSignupMode,
        initialLinkUserInput: UserInput?,
    ): LinkFormElement {
        return LinkFormElement(
            configuration = createLinkInlineConfiguration(signupMode),
            initialLinkUserInput = initialLinkUserInput,
            linkConfigurationCoordinator = createLinkConfigurationCoordinator(),
            onLinkInlineSignupStateChanged = {},
        )
    }

    private fun createLinkInlineConfiguration(
        signupMode: LinkSignupMode,
    ): LinkInlineConfiguration {
        return LinkInlineConfiguration(
            signupMode = signupMode,
            linkConfiguration = LinkConfiguration(
                stripeIntent = PaymentIntentFactory.create(),
                merchantName = "Merchant, Inc.",
                merchantCountryCode = "CA",
                customerInfo = LinkConfiguration.CustomerInfo(
                    name = "John Doe",
                    email = null,
                    phone = null,
                    billingCountryCode = "CA",
                ),
                shippingDetails = null,
                passthroughModeEnabled = false,
                cardBrandChoice = null,
                flags = mapOf(),
                useAttestationEndpointsForLink = false,
                suppress2faModal = false,
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = "pi_123_secret_123",
                ),
                elementsSessionId = "session_1234",
            ),
        )
    }

    private fun createLinkConfigurationCoordinator(): LinkConfigurationCoordinator {
        return FakeLinkConfigurationCoordinator
    }

    private object FakeLinkConfigurationCoordinator : LinkConfigurationCoordinator {
        override val emailFlow: StateFlow<String?>
            get() {
                error("Not implemented!")
            }

        override fun getComponent(configuration: LinkConfiguration): LinkComponent {
            return FakeLinkComponent(configuration)
        }

        override fun getAccountStatusFlow(configuration: LinkConfiguration): Flow<AccountStatus> {
            error("Not implemented!")
        }

        override fun linkGate(configuration: LinkConfiguration): LinkGate {
            error("Not implemented!")
        }

        override fun linkAttestationCheck(configuration: LinkConfiguration): LinkAttestationCheck {
            error("Not implemented!")
        }

        override suspend fun signInWithUserInput(
            configuration: LinkConfiguration,
            userInput: UserInput
        ): Result<Boolean> {
            error("Not implemented!")
        }

        override suspend fun attachNewCardToAccount(
            configuration: LinkConfiguration,
            paymentMethodCreateParams: PaymentMethodCreateParams
        ): Result<LinkPaymentDetails> {
            error("Not implemented!")
        }

        override suspend fun logOut(configuration: LinkConfiguration): Result<ConsumerSession> {
            error("Not implemented!")
        }
    }

    private class FakeLinkComponent(
        override val configuration: LinkConfiguration,
    ) : LinkComponent() {
        override val linkAccountManager: LinkAccountManager = FakeLinkAccountManager()
        override val linkGate: LinkGate = FakeLinkGate()
        override val linkAttestationCheck = FakeLinkAttestationCheck()

        override val inlineSignupViewModelFactory: LinkInlineSignupAssistedViewModelFactory =
            FakeLinkInlineSignupAssistedViewModelFactory(linkAccountManager, configuration)
    }

    private class FakeLinkInlineSignupAssistedViewModelFactory(
        private val linkAccountManager: LinkAccountManager,
        private val configuration: LinkConfiguration,
    ) : LinkInlineSignupAssistedViewModelFactory {
        override fun create(signupMode: LinkSignupMode, initialUserInput: UserInput?): InlineSignupViewModel {
            return InlineSignupViewModel(
                signupMode = signupMode,
                config = configuration,
                initialUserInput = initialUserInput,
                linkAccountManager = linkAccountManager,
                linkEventsReporter = FakeLinkInlineSignupEventsReporter,
                logger = Logger.noop(),
                lookupDelay = 0L,
            )
        }
    }

    private object FakeLinkInlineSignupEventsReporter : FakeLinkEventsReporter() {
        override fun onSignupStarted(isInline: Boolean) {
            Log.d("LINK_FORM_ELEMENT_TEST", "onSignupStarted")
        }

        override fun onInlineSignupCheckboxChecked() {
            Log.d("LINK_FORM_ELEMENT_TEST", "onInlineSignupCheckboxChecked")
        }
    }
}
