package com.stripe.android.paymentsheet

import android.app.Application
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.ResponseReplacement
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.testing.PaymentConfigurationTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(
    ExperimentalPaymentMethodLayoutApi::class,
)
@RunWith(RobolectricTestRunner::class)
internal class VerticalModePaymentSheetActivityTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()

    private val composeTestRule = createAndroidComposeRule<PaymentSheetActivity>()
    private val networkRule = NetworkRule()

    private val verticalModePage = VerticalModePage(composeTestRule)
    private val formPage = FormPage(composeTestRule)

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(composeTestRule)
        .around(networkRule)
        .around(PaymentConfigurationTestRule(applicationContext))

    @Test
    fun `Allows paying with card`() = runTest(
        networkSetup = {
            setupElementsSessionsResponse()
        },
    ) {
        verticalModePage.assertDoesNotHaveSavedPaymentMethods()
        verticalModePage.assertPrimaryButton(isNotEnabled())
        verticalModePage.clickOnNewLpm("card")
        formPage.waitUntilVisible()
        verticalModePage.assertPrimaryButton(isNotEnabled())
        formPage.fillOutCardDetails()
        verticalModePage.assertPrimaryButton(isEnabled())
    }

    @Test
    fun `Allows paying with cashapp`() = runTest(
        networkSetup = {
            setupElementsSessionsResponse()
        },
    ) {
        verticalModePage.assertDoesNotHaveSavedPaymentMethods()
        verticalModePage.assertPrimaryButton(isNotEnabled())
        verticalModePage.clickOnNewLpm("cashapp")
        verticalModePage.assertPrimaryButton(isEnabled())
        formPage.assertIsNotDisplayed()
    }

    @Test
    fun `Displays saved payment methods`() = runTest(
        customer = PaymentSheet.CustomerConfiguration(id = "cus_1", ephemeralKeySecret = "ek_test"),
        networkSetup = {
            setupElementsSessionsResponse(lpms = listOf("card"))
            setupV1PaymentMethodsResponse(isEmpty = false)
        },
    ) {
        verticalModePage.assertHasSavedPaymentMethods()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_12345")
        verticalModePage.assertPrimaryButton(isEnabled())
    }

    @Test
    fun `When the payment intent only has card it launches directly into the form`() = runTest(
        initialLoadWaiter = { formPage.waitUntilVisible() },
        networkSetup = {
            setupElementsSessionsResponse(lpms = listOf("card"))
        },
    ) {
        verticalModePage.assertIsNotVisible()
        verticalModePage.assertPrimaryButton(isNotEnabled())
        formPage.fillOutCardDetails()
        verticalModePage.assertPrimaryButton(isEnabled())
    }

    @Test
    fun `When the payment intent only has card it launches directly into the form with customer`() = runTest(
        customer = PaymentSheet.CustomerConfiguration(id = "cus_1", ephemeralKeySecret = "ek_test"),
        initialLoadWaiter = { formPage.waitUntilVisible() },
        networkSetup = {
            setupElementsSessionsResponse(lpms = listOf("card"))
            setupV1PaymentMethodsResponse(isEmpty = true)
        },
    ) {
        verticalModePage.assertIsNotVisible()
        verticalModePage.assertPrimaryButton(isNotEnabled())
        formPage.fillOutCardDetails()
        verticalModePage.assertPrimaryButton(isEnabled())
    }

    @Test
    fun `When the payment intent only has one LPM it launches directly into the form`() = runTest(
        initialLoadWaiter = { formPage.waitUntilVisible() },
        networkSetup = {
            setupElementsSessionsResponse(lpms = listOf("cashapp"))
        },
    ) {
        verticalModePage.assertIsNotVisible()
        verticalModePage.assertPrimaryButton(isEnabled())
    }

    private fun runTest(
        customer: PaymentSheet.CustomerConfiguration? = null,
        networkSetup: () -> Unit,
        initialLoadWaiter: () -> Unit = { verticalModePage.waitUntilVisible() },
        test: () -> Unit,
    ) {
        networkSetup()

        ActivityScenario.launch<PaymentSheetActivity>(
            PaymentSheetContractV2().createIntent(
                ApplicationProvider.getApplicationContext(),
                PaymentSheetContractV2.Args(
                    initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                        clientSecret = "pi_1234_secret_5678",
                    ),
                    config = PaymentSheet.Configuration.Builder(merchantDisplayName = "Merchant, Inc.")
                        .customer(customer)
                        .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Vertical)
                        .build(),
                    statusBarColor = PaymentSheetFixtures.STATUS_BAR_COLOR,
                )
            )
        ).use { scenario ->
            scenario.onActivity {
                initialLoadWaiter()

                test()
            }
        }
    }

    private fun setupElementsSessionsResponse(
        lpms: List<String> = listOf("card", "cashapp")
    ) {
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            val replacements = mutableListOf<ResponseReplacement>()
            replacements += ResponseReplacement(
                original = "PAYMENT_METHOD_TYPES_GO_HERE",
                // Turn the list into JSON like syntax, surrounding each LPM with quotes, and comma separating.
                new = lpms.joinToString(
                    separator = "\",\"",
                    prefix = "\"",
                    postfix = "\"",
                )
            )
            response.testBodyFromFile("elements-sessions-requires_payment_method.json", replacements)
        }
    }

    private fun setupV1PaymentMethodsResponse(isEmpty: Boolean) {
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/payment_methods"),
        ) { response ->
            if (isEmpty) {
                response.testBodyFromFile("payment-methods-get-success-empty.json")
            } else {
                response.testBodyFromFile("payment-methods-get-success.json")
            }
        }
    }
}
