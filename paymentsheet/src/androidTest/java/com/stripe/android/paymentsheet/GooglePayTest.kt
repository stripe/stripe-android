package com.stripe.android.paymentsheet

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.googlepaylauncher.GooglePayAvailabilityClient
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.ResponseReplacement
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.ui.GOOGLE_PAY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.PAYMENT_SHEET_FORM_TEST_TAG
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_METHOD_CARD_TEST_TAG
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_OPTION_TAB_LAYOUT_TEST_TAG
import com.stripe.android.paymentsheet.utils.ProductIntegrationType
import com.stripe.android.paymentsheet.utils.ProductIntegrationTypeProvider
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.expectNoResult
import com.stripe.android.paymentsheet.utils.runProductIntegrationTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class GooglePayTest {
    @TestParameter(valuesProvider = ProductIntegrationTypeProvider::class)
    lateinit var integrationType: ProductIntegrationType

    @get:Rule
    val testRules: TestRules = TestRules.create()

    private val composeTestRule = testRules.compose

    @After
    fun teardown() {
        GooglePayRepository.resetFactory()
    }

    @Test
    fun googlePayIsShownWhenFullyEnabled() = runGooglePayTest(
        isGooglePayReady = true,
        isGooglePayEnabledInElementsSession = true,
        hasGooglePayConfig = true,
    ) {
        composeTestRule.onGooglePayOption().assertExists()
    }

    @Test
    fun googlePayIsNotShownWhenNotReady() = runGooglePayTest(
        isGooglePayReady = false,
        isGooglePayEnabledInElementsSession = true,
        hasGooglePayConfig = true,
    ) {
        composeTestRule.onGooglePayOption().assertDoesNotExist()
    }

    @Test
    fun googlePayIsNotShownWhenNotEnabledInElementsSession() = runGooglePayTest(
        isGooglePayReady = true,
        isGooglePayEnabledInElementsSession = false,
        hasGooglePayConfig = true,
    ) {
        composeTestRule.onGooglePayOption().assertDoesNotExist()
    }

    @Test
    fun googlePayIsNotShownWhenGooglePayConfigIsNotProvided() = runGooglePayTest(
        isGooglePayReady = true,
        isGooglePayEnabledInElementsSession = true,
        hasGooglePayConfig = false,
    ) {
        composeTestRule.onGooglePayOption().assertDoesNotExist()
    }

    private fun runGooglePayTest(
        isGooglePayReady: Boolean,
        isGooglePayEnabledInElementsSession: Boolean,
        hasGooglePayConfig: Boolean,
        test: () -> Unit,
    ) {
        GooglePayRepository.googlePayAvailabilityClientFactory =
            FakeGooglePayAvailabilityClient.Factory(isGooglePayReady)

        enqueueElementsSession(isGooglePayEnabledInElementsSession)

        runProductIntegrationTest(
            networkRule = testRules.networkRule,
            integrationType = integrationType,
            resultCallback = ::expectNoResult,
            paymentOptionCallback = {}
        ) { context ->
            val configBuilder = PaymentSheet.Configuration.Builder(merchantDisplayName = "Merchant, Inc.")

            if (hasGooglePayConfig) {
                configBuilder.googlePay(
                    PaymentSheet.GooglePayConfiguration(
                        environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                        countryCode = "USD",
                    )
                )
            }

            context.launch(configBuilder.build())

            waitUntilLoaded()

            test()

            context.markTestSucceeded()
        }
    }

    private fun enqueueElementsSession(isGooglePayEnabledInElementsSession: Boolean) {
        testRules.networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            val replacements = if (isGooglePayEnabledInElementsSession) {
                listOf()
            } else {
                listOf(
                    ResponseReplacement(
                        original = """
                                "google_pay_preference": "enabled"
                            """.trimIndent(),
                        new = """
                                "google_pay_preference": "disabled"
                            """.trimIndent(),
                    )
                )
            }

            response.testBodyFromFile("elements-sessions-requires_payment_method.json", replacements)
        }
    }

    private fun waitUntilLoaded() {
        composeTestRule.waitUntil(UI_TIMEOUT) {
            composeTestRule
                .onAllNodes(
                    hasTestTag(PAYMENT_SHEET_FORM_TEST_TAG)
                        .or(hasTestTag(SAVED_PAYMENT_OPTION_TAB_LAYOUT_TEST_TAG))
                )
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }

    private fun ComposeTestRule.onGooglePayOption(): SemanticsNodeInteraction {
        return when (integrationType) {
            ProductIntegrationType.PaymentSheet -> onNode(hasTestTag(GOOGLE_PAY_BUTTON_TEST_TAG))
            ProductIntegrationType.FlowController -> onNode(hasTestTag(GOOGLE_PAY_SAVED_OPTION_TEST_TAG))
        }
    }

    private class FakeGooglePayAvailabilityClient(
        private val isReady: Boolean,
    ) : GooglePayAvailabilityClient {
        override suspend fun isReady(request: IsReadyToPayRequest): Boolean {
            return isReady
        }

        class Factory(private val isReady: Boolean) : GooglePayAvailabilityClient.Factory {
            override fun create(paymentsClient: PaymentsClient): GooglePayAvailabilityClient {
                return FakeGooglePayAvailabilityClient(isReady)
            }
        }
    }

    private companion object {
        const val GOOGLE_PAY_SAVED_OPTION_TEST_TAG = "${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_Google Pay"
        const val UI_TIMEOUT = 5000L
    }
}
