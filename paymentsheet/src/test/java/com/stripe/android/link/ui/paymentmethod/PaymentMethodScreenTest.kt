package com.stripe.android.link.ui.paymentmethod

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.printToString
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.confirmation.FakeLinkConfirmationHandler
import com.stripe.android.link.ui.PrimaryButtonTag
import com.stripe.android.link.ui.paymentmenthod.PAYMENT_METHOD_ERROR_TAG
import com.stripe.android.link.ui.paymentmenthod.PaymentMethodScreen
import com.stripe.android.link.ui.paymentmenthod.PaymentMethodViewModel
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeLogger
import com.stripe.android.ui.core.elements.events.LocalCardBrandDisallowedReporter
import com.stripe.android.ui.core.elements.events.LocalCardNumberCompletedEventReporter
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.stripe.android.link.confirmation.Result as LinkConfirmationResult

@RunWith(AndroidJUnit4::class)
internal class PaymentMethodScreenTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    @Test
    fun `form fields are displayed correctly`() = runScenario {
        onCardNumber().assertExists()
        onCvc().assertIsDisplayed()
        onExpiryDate().assertIsDisplayed()
        onZipCode().scrollToAndAssertDisplayed()
        onPayButton()
            .scrollToAndAssertDisplayed()
            .assertIsEnabled()
        onErrorText().assertIsNotDisplayed()
    }

    @Test
    fun `no error message on successful payment`() = runScenario {
        fillCardDetails()
        assertThat(formHelper.formFieldValuesChangedCall.awaitItem()).isNotNull()

        onPayButton()
            .scrollToAndAssertDisplayed()
            .assertIsEnabled()
            .performClick()

        onErrorText().assertDoesNotExist()
    }

    @Test
    fun `error message should be displayed on card creation failure`() = runScenario {
        fillCardDetails()

        linkAccountManager.createCardPaymentDetailsResult = Result.failure(Throwable("oops"))

        onPayButton()
            .scrollToAndAssertDisplayed()
            .assertIsEnabled()
            .performClick()

        composeTestRule.waitForIdle()

        onErrorText()
            .scrollToAndAssertDisplayed()
            .onChild()
            .assert(hasText("Something went wrong"))

        println(onErrorText().printToString())
    }

    @Test
    fun `error message should be displayed on payment confirmation failure`() = runScenario {
        fillCardDetails()

        linkConfirmationHandler.confirmWithLinkPaymentDetailsResult =
            LinkConfirmationResult.Failed("oops".resolvableString)

        onPayButton()
            .scrollToAndAssertDisplayed()
            .assertIsEnabled()
            .performClick()

        onErrorText()
            .scrollToAndAssertDisplayed()
            .onChild()
            .assert(hasText("oops"))
    }

    private fun fillCardDetails() {
        onCardNumber().performTextReplacement("4242424242424242")
        onCvc().performTextReplacement("123")
        onExpiryDate().performTextReplacement("12/34")
        onZipCode().performTextReplacement("12345")
    }

    private fun screen(viewModel: PaymentMethodViewModel) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalCardNumberCompletedEventReporter provides { },
                LocalCardBrandDisallowedReporter provides { }
            ) {
                PaymentMethodScreen(viewModel)
            }
        }
    }

    private fun runScenario(
        linkAccountManager: FakeLinkAccountManager = FakeLinkAccountManager(),
        linkConfirmationHandler: FakeLinkConfirmationHandler = FakeLinkConfirmationHandler(),
        formHelper: PaymentMethodFormHelper = PaymentMethodFormHelper(),
        block: suspend Scenario.() -> Unit
    ) {
        screen(
            viewModel = createViewModel(
                linkAccountManager = linkAccountManager,
                linkConfirmationHandler = linkConfirmationHandler,
                formHelper = formHelper,
            )
        )
        composeTestRule.waitForIdle()
        Scenario(
            linkAccountManager = linkAccountManager,
            linkConfirmationHandler = linkConfirmationHandler,
            formHelper = formHelper,
        ).apply {
            runTest {
                formHelper.formFieldValuesChangedCall.skipItems(1)
                block()
            }
        }
        formHelper.formFieldValuesChangedCall.ensureAllEventsConsumed()
    }

    private fun createViewModel(
        linkAccountManager: FakeLinkAccountManager,
        linkConfirmationHandler: FakeLinkConfirmationHandler,
        formHelper: FormHelper,
    ): PaymentMethodViewModel {
        return PaymentMethodViewModel(
            configuration = TestFactory.LINK_CONFIGURATION,
            linkAccount = TestFactory.LINK_ACCOUNT,
            linkAccountManager = linkAccountManager,
            linkConfirmationHandler = linkConfirmationHandler,
            logger = FakeLogger(),
            formHelper = formHelper,
            dismissWithResult = {}
        )
    }

    private fun onPayButton() =
        composeTestRule.onNodeWithTag(PrimaryButtonTag, useUnmergedTree = true)

    private fun onCardNumber() = composeTestRule.onNode(hasText("Card number"))

    private fun onCvc() = composeTestRule.onNode(hasText("CVC"))

    private fun onExpiryDate() = composeTestRule.onNode(
        hasContentDescription(value = "Expiration date", substring = true)
    )

    private fun onZipCode() = composeTestRule.onNode(hasText("ZIP Code"))

    private fun onErrorText() = composeTestRule.onNodeWithTag(PAYMENT_METHOD_ERROR_TAG, useUnmergedTree = false)

    private fun SemanticsNodeInteraction.scrollToAndAssertDisplayed(): SemanticsNodeInteraction {
        return assertExists()
            .performScrollTo()
            .assertIsDisplayed()
    }

    private data class Scenario(
        val linkAccountManager: FakeLinkAccountManager,
        val linkConfirmationHandler: FakeLinkConfirmationHandler,
        val formHelper: PaymentMethodFormHelper,
    )
}
