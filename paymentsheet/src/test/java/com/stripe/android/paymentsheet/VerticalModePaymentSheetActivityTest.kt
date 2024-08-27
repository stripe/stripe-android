package com.stripe.android.paymentsheet

import android.app.Application
import android.os.Build
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.ResponseReplacement
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.PaymentMethodFactory.update
import org.json.JSONArray
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(
    ExperimentalPaymentMethodLayoutApi::class,
)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class VerticalModePaymentSheetActivityTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()

    private val composeTestRule = createAndroidComposeRule<PaymentSheetActivity>()
    private val networkRule = NetworkRule()

    private val verticalModePage = VerticalModePage(composeTestRule)
    private val formPage = FormPage(composeTestRule)
    private val managePage = ManagePage(composeTestRule)
    private val editPage = EditPage(composeTestRule)

    private val card1 = PaymentMethodDetails("pm_12345", "4242")
    private val card2 = PaymentMethodDetails("pm_67890", "5544")

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(composeTestRule)
        .around(networkRule)
        .around(PaymentConfigurationTestRule(applicationContext))
//        .around(ShampooRule(1000))

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
            setupV1PaymentMethodsResponse(card1, card2)
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
            setupV1PaymentMethodsResponse()
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

    @Test
    fun `Updates selected saved payment method`() = runTest(
        customer = PaymentSheet.CustomerConfiguration(id = "cus_1", ephemeralKeySecret = "ek_test"),
        networkSetup = {
            setupElementsSessionsResponse(lpms = listOf("card"))
            setupV1PaymentMethodsResponse(card1, card2)
        },
    ) {
        verticalModePage.assertHasSavedPaymentMethods()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_12345")
        verticalModePage.assertPrimaryButton(isEnabled())

        verticalModePage.clickViewMore()
        managePage.waitUntilVisible()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_12345")
        managePage.selectPaymentMethod("pm_67890")

        verticalModePage.waitUntilVisible()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_67890")
    }

    @Test
    fun `Removing card selects next available card`() = runTest(
        customer = PaymentSheet.CustomerConfiguration(id = "cus_1", ephemeralKeySecret = "ek_test"),
        networkSetup = {
            setupElementsSessionsResponse()
            setupV1PaymentMethodsResponse(card1, card2)
            setupPaymentMethodDetachResponse("pm_12345")
        },
    ) {
        verticalModePage.assertHasSavedPaymentMethods()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_12345")
        verticalModePage.assertPrimaryButton(isEnabled())

        verticalModePage.clickViewMore()
        managePage.waitUntilVisible()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_12345")
        managePage.clickEdit()
        managePage.clickRemove("pm_12345")
        managePage.clickDone()
        Espresso.pressBack()

        verticalModePage.waitUntilVisible()
        verticalModePage.clickSavedPaymentMethod("pm_67890")
        verticalModePage.assertPrimaryButton(isEnabled())
    }

    @Test
    fun `Removing last card navigates back`() = runTest(
        customer = PaymentSheet.CustomerConfiguration(id = "cus_1", ephemeralKeySecret = "ek_test"),
        networkSetup = {
            setupElementsSessionsResponse()
            setupV1PaymentMethodsResponse(card1, card2)
            setupPaymentMethodDetachResponse("pm_12345")
            setupPaymentMethodDetachResponse("pm_67890")
        },
    ) {
        verticalModePage.assertHasSavedPaymentMethods()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_12345")
        verticalModePage.assertPrimaryButton(isEnabled())

        verticalModePage.clickViewMore()
        managePage.waitUntilVisible()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_12345")
        managePage.clickEdit()
        managePage.clickRemove("pm_12345")
        managePage.clickRemove("pm_67890")

        verticalModePage.waitUntilVisible()
        verticalModePage.assertDoesNotHaveSavedPaymentMethods()
        verticalModePage.assertPrimaryButton(isNotEnabled())
    }

    @Test
    fun `Removing only card navigates back`() = runTest(
        customer = PaymentSheet.CustomerConfiguration(id = "cus_1", ephemeralKeySecret = "ek_test"),
        networkSetup = {
            setupElementsSessionsResponse()
            setupV1PaymentMethodsResponse(card1)
            setupPaymentMethodDetachResponse("pm_12345")
        },
    ) {
        verticalModePage.assertHasSavedPaymentMethods()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_12345")
        verticalModePage.assertPrimaryButton(isEnabled())

        verticalModePage.clickEdit()
        managePage.waitUntilRemoveVisible("pm_12345")
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_12345")
        managePage.clickRemove("pm_12345")

        verticalModePage.waitUntilVisible()
        verticalModePage.assertDoesNotHaveSavedPaymentMethods()
        verticalModePage.assertPrimaryButton(isNotEnabled())
    }

    @Test
    fun `Updating a card brand updates the icon in the list`() = runTest(
        customer = PaymentSheet.CustomerConfiguration(id = "cus_1", ephemeralKeySecret = "ek_test"),
        networkSetup = {
            setupElementsSessionsResponse(isCbcEligible = true)
            setupV1PaymentMethodsResponse(card1, card2, addCbcNetworks = true)
            setupPaymentMethodUpdateResponse(paymentMethodDetails = card1, cardBrand = "visa")
        },
    ) {
        verticalModePage.assertHasSavedPaymentMethods()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_12345", cardBrand = "cartes_bancaries")
        verticalModePage.assertPrimaryButton(isEnabled())

        verticalModePage.clickViewMore()
        managePage.waitUntilVisible()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_12345", cardBrand = "cartes_bancaries")
        managePage.clickEdit()
        managePage.clickEdit("pm_12345")

        editPage.assertIsVisible()
        editPage.setCardBrand("Visa")
        editPage.update()
        managePage.waitUntilVisible()
        managePage.clickDone()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_12345", cardBrand = "visa")
        Espresso.pressBack()

        verticalModePage.waitUntilVisible()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_12345", cardBrand = "visa")
        verticalModePage.assertPrimaryButton(isEnabled())
    }

    @Test
    fun `Displayed saved payment method is correct`() = runTest(
        customer = PaymentSheet.CustomerConfiguration(id = "cus_1", ephemeralKeySecret = "ek_test"),
        networkSetup = {
            setupElementsSessionsResponse()
            setupV1PaymentMethodsResponse(card1, card2)
        },
    ) {
        verticalModePage.assertHasSavedPaymentMethods()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_12345")
        verticalModePage.assertPrimaryButton(isEnabled())

        verticalModePage.clickViewMore()
        managePage.waitUntilVisible()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_12345")
        managePage.selectPaymentMethod("pm_67890")

        verticalModePage.waitUntilVisible()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_67890")
        verticalModePage.assertPrimaryButton(isEnabled())

        verticalModePage.clickOnNewLpm("cashapp")
        // We don't want it to switch back to the first saved payment method (pm_12345).
        verticalModePage.assertHasDisplayedSavedPaymentMethod("pm_67890")
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
        lpms: List<String> = listOf("card", "cashapp"),
        isCbcEligible: Boolean = false,
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
            replacements += ResponseReplacement(
                original = "CBC_ELIGIBLE_HERE",
                new = "$isCbcEligible",
            )
            response.testBodyFromFile("elements-sessions-requires_payment_method.json", replacements)
        }
    }

    private fun setupV1PaymentMethodsResponse(
        vararg paymentMethodDetails: PaymentMethodDetails,
        addCbcNetworks: Boolean = false,
    ) {
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/payment_methods"),
        ) { response ->
            val cardsArray = JSONArray()

            for (paymentMethodDetail in paymentMethodDetails) {
                val card = PaymentMethodFactory.card(
                    id = paymentMethodDetail.id
                ).update(
                    last4 = paymentMethodDetail.last4,
                    addCbcNetworks = addCbcNetworks,
                )
                cardsArray.put(PaymentMethodFactory.convertCardToJson(card))
            }

            val cardsStringified = cardsArray.toString(2)

            val body = """
            {
              "object": "list",
              "data": $cardsStringified,
              "has_more": false,
              "url": "/v1/payment_methods"
            }
            """.trimIndent()
            response.setBody(body)
        }
    }

    private fun setupPaymentMethodDetachResponse(paymentMethodId: String) {
        networkRule.enqueue(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_methods/$paymentMethodId/detach"),
        ) { response ->
            // We ignore the result.
            response.setResponseCode(500)
        }
    }

    private fun setupPaymentMethodUpdateResponse(paymentMethodDetails: PaymentMethodDetails, cardBrand: String) {
        networkRule.enqueue(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_methods/${paymentMethodDetails.id}"),
            bodyPart(urlEncode("card[networks][preferred]"), cardBrand)
        ) { response ->
            val originalCard = PaymentMethodFactory.card(
                id = paymentMethodDetails.id
            ).update(
                last4 = paymentMethodDetails.last4,
                addCbcNetworks = true,
            )

            val card = originalCard.copy(
                card = originalCard.card!!.copy(
                    displayBrand = cardBrand
                )
            )
            response.setBody(PaymentMethodFactory.convertCardToJson(card).toString(2))
        }
    }

    private data class PaymentMethodDetails(
        val id: String,
        val last4: String,
    )
}
