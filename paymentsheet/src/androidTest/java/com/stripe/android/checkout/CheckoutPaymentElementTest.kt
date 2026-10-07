package com.stripe.android.checkout

import androidx.test.espresso.Espresso
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutConfirm
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.checkouttesting.createPaymentMethod
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.elements.PaymentElement
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.link.ui.wallet.LinkWalletPage
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedContentPage
import com.stripe.android.paymentelement.EmbeddedFormPage
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.testing.waitUntilWithIdle
import com.stripe.paymentelementtestpages.VerticalModePage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.mockwebserver.MockResponse
import org.json.JSONArray
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutPaymentElementTest {
    private val networkRule = NetworkRule(validationTimeout = 5.seconds)

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule) {
        around(FeatureFlagTestRule(FeatureFlags.nativeLinkEnabled, isEnabled = true))
    }

    private val contentPage = EmbeddedContentPage(testRules.compose)
    private val formPage = EmbeddedFormPage(testRules.compose)
    private val linkWalletPage = LinkWalletPage(testRules.compose)
    private val verticalModePage = VerticalModePage(testRules.compose)

    @After
    fun teardown() {
        GooglePayRepository.resetFactory()
    }

    @Test
    fun testBackingOutOfFormPreservesPreviouslySelectedPaymentMethod() {
        runCheckoutPaymentElementScenario(
            networkRule = networkRule,
            setup = { controller ->
                controller.configure(DEFAULT_CLIENT_SECRET).getOrThrow()
            },
        ) {
            // Open the card form, then back out without entering any details.
            contentPage.clickOnLpm("card")
            formPage.waitUntilVisible()
            Espresso.pressBack()
            formPage.waitUntilMissing()

            // Select a payment method that does not require a form.
            contentPage.clickOnLpm("cashapp")
            contentPage.assertHasSelectedLpm("cashapp")

            // Re-open the card form and back out again.
            contentPage.clickOnLpm("card")
            formPage.waitUntilVisible()
            Espresso.pressBack()
            formPage.waitUntilMissing()

            // Backing out of the form must not clear the previously selected payment method.
            contentPage.assertHasSelectedLpm("cashapp")
            markTestSucceeded()
        }
    }

    @Test
    fun testSuccessfulCardPayment() {
        var checkoutResult: CheckoutController.Result? = null
        runCheckoutPaymentElementScenario(
            networkRule = networkRule,
            resultCallback = { result -> checkoutResult = result },
            setup = { controller ->
                controller.configure(DEFAULT_CLIENT_SECRET).getOrThrow()
            },
        ) {
            networkRule.createPaymentMethod()
            networkRule.checkoutConfirm { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }

            contentPage.clickOnLpm("card")
            formPage.fillOutCardDetails()
            formPage.clickPrimaryButton()
            confirm()
        }

        assertThat(checkoutResult).isInstanceOf(CheckoutController.Result.Completed::class.java)
    }

    @Test
    fun testPaymentMethodsAreDisabledWhileCheckoutUpdateIsInProgress() {
        runCheckoutPaymentElementScenario(
            networkRule = networkRule,
            setup = { controller ->
                controller.configure(DEFAULT_CLIENT_SECRET).getOrThrow()
            },
        ) {
            coroutineScope {
                contentPage.assertLpmIsEnabled("card", isEnabled = true)

                val holdResponse = CountDownLatch(1)
                networkRule.checkoutUpdate(
                    bodyPart("promotion_code", "10OFF"),
                ) { response ->
                    holdResponse.await(10, TimeUnit.SECONDS)
                    response.testBodyFromFile("checkout-session-init.json") { json ->
                        json.put("customer_email", "checkout@example.com")
                        json.getJSONObject("elements_session").remove("link_settings")
                    }
                }

                val update = async(Dispatchers.Main) {
                    controller.applyPromotionCode("10OFF")
                }
                try {
                    testRules.compose.waitUntilWithIdle {
                        controller.isUpdating.value
                    }
                    contentPage.assertLpmIsEnabled("card", isEnabled = false)
                } finally {
                    holdResponse.countDown()
                }

                assertThat(update.await().isSuccess).isTrue()
                testRules.compose.waitUntilWithIdle {
                    !controller.isUpdating.value
                }
                contentPage.assertLpmIsEnabled("card", isEnabled = true)
                markTestSucceeded()
            }
        }
    }

    @Test
    fun testPaymentOptionsBridgesLinkAccountStateThroughCheckout() {
        val checkoutInitResponse: (MockResponse) -> Unit = { response ->
            response.testBodyFromFile("checkout-session-init.json") { json ->
                json.put("customer_email", "test@stripe.com")
            }
        }
        val configuration = CheckoutController.Configuration().paymentElement(
            PaymentElement.Configuration()
                .paymentMethodLayout(PaymentElement.Configuration.PaymentMethodLayout.Vertical)
                .linkConfiguration(
                    PaymentElement.Configuration.LinkConfiguration().display(
                        PaymentElement.Configuration.LinkConfiguration.Display.WalletButtonHidden
                    )
                )
        )

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/lookup"),
        ) { response ->
            response.testBodyFromFile("consumer-accounts-signup-success.json") { json ->
                json.put("exists", true)
            }
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/payment_details/list"),
        ) { response ->
            response.testBodyFromFile("consumer-payment-details-success.json") { json ->
                val paymentDetails = json.getJSONObject("redacted_payment_details")
                json.put("redacted_payment_details", JSONArray().put(paymentDetails))
            }
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/log_out"),
        ) { response ->
            response.testBodyFromFile("consumer-session-logout-success.json")
        }

        runCheckoutPaymentElementScenario(
            networkRule = networkRule,
            checkoutInitResponse = checkoutInitResponse,
            setup = { controller ->
                controller.configure(DEFAULT_CLIENT_SECRET, configuration).getOrThrow()
            },
        ) {
            contentPage.clickOnLpm("link")
            contentPage.assertHasSelectedLpm("link")
            presentPaymentOptions()

            linkWalletPage.logOut()

            verticalModePage.waitUntilVisible()
            verticalModePage.assertLpmDoesNotExist("link")
            Espresso.pressBack()
            verticalModePage.waitUntilMissing()

            networkRule.checkoutInit(responseFactory = checkoutInitResponse)
            withContext(Dispatchers.Main) {
                controller.configure(DEFAULT_CLIENT_SECRET, configuration).getOrThrow()
            }
            markTestSucceeded()
        }
    }

    @Test
    fun testLinkAccountStatusIsLoaded_forLinkDisplayAutomatic() {
        runLinkLoadingTest(
            linkDisplay = PaymentElement.Configuration.LinkConfiguration.Display.Automatic,
            expectedToLoadLinkAccount = true,
        )
    }

    @Test
    fun testLinkAccountStatusIsLoaded_forLinkDisplayNever() {
        runLinkLoadingTest(
            linkDisplay = PaymentElement.Configuration.LinkConfiguration.Display.Never,
            expectedToLoadLinkAccount = false,
        )
    }

    private fun runLinkLoadingTest(
        linkDisplay: PaymentElement.Configuration.LinkConfiguration.Display,
        expectedToLoadLinkAccount: Boolean,
    ) {
        val checkoutInitResponse: (MockResponse) -> Unit = { response ->
            response.testBodyFromFile("checkout-session-init.json") { json ->
                json.put("customer_email", "test@stripe.com")
            }
        }
        val configuration = CheckoutController.Configuration().paymentElement(
            PaymentElement.Configuration()
                .paymentMethodLayout(PaymentElement.Configuration.PaymentMethodLayout.Vertical)
                .linkConfiguration(
                    PaymentElement.Configuration.LinkConfiguration().display(
                        linkDisplay
                    )
                )
        )

        if (expectedToLoadLinkAccount) {
            networkRule.enqueue(
                method("POST"),
                path("/v1/consumers/sessions/lookup"),
            ) { response ->
                response.testBodyFromFile("consumer-accounts-signup-success.json") { json ->
                    json.put("exists", true)
                }
            }
        }

        runCheckoutPaymentElementScenario(
            networkRule = networkRule,
            checkoutInitResponse = checkoutInitResponse,
            setup = { controller ->
                controller.configure(DEFAULT_CLIENT_SECRET, configuration).getOrThrow()
            },
        ) {
            // Just testing loading events, mark test succeeded once that has completed.
            markTestSucceeded()
        }
    }

    private companion object {
        const val DEFAULT_CLIENT_SECRET = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example"
    }
}
