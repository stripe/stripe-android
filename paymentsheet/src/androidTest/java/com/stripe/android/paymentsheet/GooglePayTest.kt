package com.stripe.android.paymentsheet

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.core.os.bundleOf
import androidx.test.espresso.Espresso
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.googlepaylauncher.GooglePayAvailabilityClient
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.ResponseReplacement
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.paymentsheet.ui.GOOGLE_PAY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.PAYMENT_SHEET_FORM_TEST_TAG
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_METHOD_CARD_TEST_TAG
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_OPTION_TAB_LAYOUT_TEST_TAG
import com.stripe.android.paymentsheet.utils.ProductIntegrationTestRunnerContext
import com.stripe.android.paymentsheet.utils.ProductIntegrationType
import com.stripe.android.paymentsheet.utils.ProductIntegrationTypeProvider
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.expectNoResult
import com.stripe.android.paymentsheet.utils.runProductIntegrationTest
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.PaymentMethodFactory
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
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

    @Before
    fun setup() {
        Intents.init()
    }

    @After
    fun teardown() {
        GooglePayRepository.resetFactory()
        Intents.release()
    }

    @Test
    fun googlePayIsShownWhenFullyEnabled() = runGooglePayAvailabilityTest(
        isGooglePayReady = true,
        isGooglePayEnabledInElementsSession = true,
        hasGooglePayConfig = true,
        shouldHaveGooglePayOption = true,
    )

    @Test
    fun googlePayIsNotShownWhenNotReady() = runGooglePayAvailabilityTest(
        isGooglePayReady = false,
        isGooglePayEnabledInElementsSession = true,
        hasGooglePayConfig = true,
        shouldHaveGooglePayOption = false,
    )

    @Test
    fun googlePayIsNotShownWhenNotEnabledInElementsSession() = runGooglePayAvailabilityTest(
        isGooglePayReady = true,
        isGooglePayEnabledInElementsSession = false,
        hasGooglePayConfig = true,
        shouldHaveGooglePayOption = false,
    )

    @Test
    fun googlePayIsNotShownWhenGooglePayConfigIsNotProvided() = runGooglePayAvailabilityTest(
        isGooglePayReady = true,
        isGooglePayEnabledInElementsSession = true,
        hasGooglePayConfig = false,
        shouldHaveGooglePayOption = false,
    )

    @Test
    fun googlePaySucceeds() {
        var resultCallbackCalled = false

        runGooglePayFlowTest(
            paymentResultCallback = {
                resultCallbackCalled = true

                assertCompleted(it)
            }
        ) { scenario ->
            val paymentMethod = PaymentMethodFactory.card()

            intendingGooglePayToBeLaunched(GooglePayPaymentMethodLauncher.Result.Completed(paymentMethod))
            intendingPaymentConfirmationToBeLaunched(
                InternalPaymentResult.Completed(PaymentIntentFactory.create(paymentMethod))
            )

            scenario.confirm()

            intendedGooglePayToBeLaunched()
            intendedPaymentConfirmationToBeLaunched()

            composeTestRule.waitUntil(UI_TIMEOUT) {
                resultCallbackCalled
            }
        }
    }

    private fun runGooglePayFlowTest(
        paymentResultCallback: (PaymentSheetResult) -> Unit,
        test: (GooglePayFlowScenario) -> Unit
    ) {
        runGooglePayTest(
            isGooglePayReady = true,
            isGooglePayEnabledInElementsSession = true,
            hasGooglePayConfig = true,
            paymentResultCallback = paymentResultCallback,
        ) { context ->
            test(
                GooglePayFlowScenario {
                    Espresso.onIdle()
                    composeTestRule.waitForIdle()

                    composeTestRule.onGooglePayOption().performClick()

                    if (context is ProductIntegrationTestRunnerContext.WithFlowController) {
                        runTest {
                            val label = context.context.configureCallbackTurbine.awaitItem()?.label
                            assertThat(label).isEqualTo("Google Pay")
                        }

                        context.confirm()
                    }
                }
            )
        }
    }

    private fun runGooglePayAvailabilityTest(
        isGooglePayReady: Boolean,
        isGooglePayEnabledInElementsSession: Boolean,
        hasGooglePayConfig: Boolean,
        shouldHaveGooglePayOption: Boolean,
    ) = runGooglePayTest(
        isGooglePayReady = isGooglePayReady,
        isGooglePayEnabledInElementsSession = isGooglePayEnabledInElementsSession,
        hasGooglePayConfig = hasGooglePayConfig,
        paymentResultCallback = ::expectNoResult
    ) { context ->
        composeTestRule.onGooglePayOption().run {
            if (shouldHaveGooglePayOption) {
                assertExists()
            } else {
                assertDoesNotExist()
            }
        }

        context.markTestSucceeded()
    }

    private fun runGooglePayTest(
        isGooglePayReady: Boolean,
        isGooglePayEnabledInElementsSession: Boolean,
        hasGooglePayConfig: Boolean,
        paymentResultCallback: (PaymentSheetResult) -> Unit,
        test: (context: ProductIntegrationTestRunnerContext) -> Unit,
    ) {
        GooglePayRepository.googlePayAvailabilityClientFactory =
            FakeGooglePayAvailabilityClient.Factory(isGooglePayReady)

        enqueueElementsSession(isGooglePayEnabledInElementsSession)

        runProductIntegrationTest(
            networkRule = testRules.networkRule,
            integrationType = integrationType,
            resultCallback = paymentResultCallback,
        ) { context ->
            val configBuilder = PaymentSheet.Configuration.Builder(merchantDisplayName = "Merchant, Inc.")
                .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Horizontal)

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

            test(context)
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

    private fun intendingGooglePayToBeLaunched(result: GooglePayPaymentMethodLauncher.Result) {
        intending(hasComponent(GOOGLE_PAY_ACTIVITY_NAME)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                Intent().putExtra("extra_result", result)
            )
        )
    }

    private fun intendingPaymentConfirmationToBeLaunched(result: InternalPaymentResult) {
        intending(hasComponent(PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                Intent().putExtras(bundleOf("extra_args" to result))
            )
        )
    }

    private fun intendedGooglePayToBeLaunched() {
        intended(hasComponent(GOOGLE_PAY_ACTIVITY_NAME))
    }

    private fun intendedPaymentConfirmationToBeLaunched() {
        intended(hasComponent(PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME))
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

    private class GooglePayFlowScenario(
        val confirm: () -> Unit,
    )

    private companion object {
        const val GOOGLE_PAY_ACTIVITY_NAME =
            "com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherActivity"

        const val PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME =
            "com.stripe.android.payments.paymentlauncher.PaymentLauncherConfirmationActivity"

        const val GOOGLE_PAY_SAVED_OPTION_TEST_TAG = "${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_Google Pay"
        const val UI_TIMEOUT = 5000L
    }
}
