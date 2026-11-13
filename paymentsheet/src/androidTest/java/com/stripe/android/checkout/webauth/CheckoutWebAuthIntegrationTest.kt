package com.stripe.android.checkout.webauth

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CHECKOUT_PLAYGROUND_BACKEND_URL
import com.stripe.android.checkout.CheckoutElementsController
import com.stripe.android.checkout.CheckoutPlaygroundBackend
import com.stripe.android.checkout.CheckoutSessionParamsBuilder
import com.stripe.android.checkout.runCheckoutIntegrationTest
import com.stripe.android.elements.PaymentElement
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedContentPage
import com.stripe.android.paymentelement.EmbeddedFormPage
import com.stripe.android.testing.BrowserAuthorizationPage
import com.stripe.android.testing.CleanupChromeRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

// CI runs this package on a Google APIs emulator, apart from the aosp-atd suite whose WebView 101 leaks Activities.
@OptIn(CheckoutSessionPreview::class)
@RunWith(AndroidJUnit4::class)
internal class CheckoutWebAuthIntegrationTest {
    private val composeRule = createEmptyComposeRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(composeRule)
        .around(CleanupChromeRule)

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val contentPage = EmbeddedContentPage(composeRule)
    private val formPage = EmbeddedFormPage(composeRule)
    private val challengePage = ThreeDS2ChallengePage(device)
    private val browserPage = BrowserAuthorizationPage(device, composeRule)

    private val configuration = CheckoutElementsController.Configuration()
        .merchantDisplayName("Checkout E2E")
        .paymentElement(
            PaymentElement.Configuration()
                .googlePayConfiguration(
                    PaymentElement.Configuration.GooglePayConfiguration()
                        .display(PaymentElement.Configuration.GooglePayConfiguration.Display.Never)
                )
                .linkConfiguration(
                    PaymentElement.Configuration.LinkConfiguration()
                        .display(PaymentElement.Configuration.LinkConfiguration.Display.Never)
                )
        )

    @Test
    fun testCardWith3ds2ChallengeCompletes() = runBlocking {
        val result = runCheckoutIntegrationTest(
            backend = CheckoutPlaygroundBackend(
                baseUrl = CHECKOUT_PLAYGROUND_BACKEND_URL,
                merchant = "US",
            ),
            sessionParams = CheckoutSessionParamsBuilder(unitAmount = 1000L).build(),
            configuration = configuration,
            setup = {
                contentPage.clickOnLpm("card")
                formPage.fillOutCardDetails(newCardNumber = "4000000000003220")
                formPage.clickPrimaryButton()
            },
            handleNextAction = {
                challengePage.complete()
            },
        )
        assertThat(result).isInstanceOf(CheckoutElementsController.Result.Completed::class.java)
    }

    @Test
    fun testCashAppRedirectAuthorizedCompletes() = runBlocking {
        val result = runCheckoutIntegrationTest(
            backend = CheckoutPlaygroundBackend(
                baseUrl = CHECKOUT_PLAYGROUND_BACKEND_URL,
                merchant = "US",
            ),
            sessionParams = CheckoutSessionParamsBuilder(unitAmount = 1000L)
                .allowedPaymentMethodTypes(listOf("card", "cashapp"))
                .build(),
            configuration = configuration,
            setup = {
                contentPage.clickOnLpm("cashapp")
                contentPage.assertHasSelectedLpm("cashapp")
            },
            handleNextAction = {
                browserPage.awaitChromeLaunch()
                browserPage.dismissFirstRunIfShown()
                browserPage.awaitPageText(AUTHORIZE_TEST_PAYMENT, AUTHORIZATION_PAGE_LOAD_TIMEOUT_MS)
                checkNotNull(device.findObject(By.textContains(AUTHORIZE_TEST_PAYMENT))) {
                    "$AUTHORIZE_TEST_PAYMENT button not found"
                }.click()
            },
        )
        assertThat(result).isInstanceOf(CheckoutElementsController.Result.Completed::class.java)
    }

    private companion object {
        const val AUTHORIZE_TEST_PAYMENT = "AUTHORIZE TEST PAYMENT"
        const val AUTHORIZATION_PAGE_LOAD_TIMEOUT_MS = 60_000L
    }
}
