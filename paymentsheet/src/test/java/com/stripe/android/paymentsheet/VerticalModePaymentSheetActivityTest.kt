package com.stripe.android.paymentsheet

import android.app.Application
import android.os.Build
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ExperimentalCardBrandFilteringApi
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.RequestMatchers.query
import com.stripe.android.networktesting.ResponseReplacement
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.PaymentMethodFactory.update
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(
    ExperimentalCardBrandFilteringApi::class,
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

    private val card1 = CardPaymentMethodDetails("pm_12345", "4242")
    private val card2 = CardPaymentMethodDetails("pm_67890", "5544")
    private val usBankAccount1 = UsBankPaymentMethodDetails("pm_54321")
    private val usBankAccount2 = UsBankPaymentMethodDetails("pm_09876")

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
            setupV1PaymentMethodsResponse(type = "card")
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
        managePage.clickEdit("pm_12345")
        editPage.clickRemove()
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
        managePage.clickEdit("pm_12345")
        editPage.clickRemove()

        managePage.waitUntilVisible()
        managePage.clickEdit("pm_67890")
        editPage.clickRemove()

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
        editPage.clickRemove()

        verticalModePage.waitUntilVisible()
        verticalModePage.assertDoesNotHaveSavedPaymentMethods()
        verticalModePage.assertPrimaryButton(isNotEnabled())
    }

//    @Test
//    fun `Updating a card brand updates the icon in the list`() = runTest(
//        customer = PaymentSheet.CustomerConfiguration(id = "cus_1", ephemeralKeySecret = "ek_test"),
//        networkSetup = {
//            setupElementsSessionsResponse(isCbcEligible = true)
//            setupV1PaymentMethodsResponse(card1.copy(addCbcNetworks = true), card2.copy(addCbcNetworks = true))
//            setupPaymentMethodUpdateResponse(paymentMethodDetails = card1, cardBrand = "visa")
//        },
//    ) {
//        verticalModePage.assertHasSavedPaymentMethods()
//        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_12345", cardBrand = "cartes_bancaries")
//        verticalModePage.assertPrimaryButton(isEnabled())
//
//        verticalModePage.clickViewMore()
//        managePage.waitUntilVisible()
//        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_12345", cardBrand = "cartes_bancaries")
//        managePage.clickEdit()
//        managePage.clickEdit("pm_12345")
//
//        editPage.assertIsVisible()
//        editPage.setCardBrand("Visa")
//        editPage.update()
//        managePage.waitUntilVisible()
//        managePage.clickDone()
//        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_12345", cardBrand = "visa")
//        Espresso.pressBack()
//
//        verticalModePage.waitUntilVisible()
//        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_12345", cardBrand = "visa")
//        verticalModePage.assertPrimaryButton(isEnabled())
//    }

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

    @Test
    fun `Selection is preserved after opening form`() = runTest(
        customer = PaymentSheet.CustomerConfiguration(id = "cus_1", ephemeralKeySecret = "ek_test"),
        networkSetup = {
            setupElementsSessionsResponse()
            setupV1PaymentMethodsResponse(card1, card2)
        },
    ) {
        verticalModePage.assertHasSavedPaymentMethods()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_12345")
        verticalModePage.assertPrimaryButton(isEnabled())

        verticalModePage.clickOnNewLpm("card")
        formPage.waitUntilVisible()
        Espresso.pressBack()

        verticalModePage.waitUntilVisible()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_12345")
        verticalModePage.assertPrimaryButton(isEnabled())

        verticalModePage.clickOnNewLpm("cashapp")
        verticalModePage.assertLpmIsSelected("cashapp")

        verticalModePage.clickOnNewLpm("card")
        formPage.waitUntilVisible()
        Espresso.pressBack()

        verticalModePage.waitUntilVisible()
        verticalModePage.assertLpmIsSelected("cashapp")
    }

    @Test
    fun `Primary button label is correctly applied`() = runTest(
        primaryButtonLabel = "Gimme money!",
        customer = PaymentSheet.CustomerConfiguration(id = "cus_1", ephemeralKeySecret = "ek_test"),
        networkSetup = {
            setupElementsSessionsResponse()
            setupV1PaymentMethodsResponse(card1, card2)
        },
    ) {
        verticalModePage.assertHasSavedPaymentMethods()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_12345")
        verticalModePage.assertPrimaryButton(isEnabled())
        composeTestRule.onNodeWithText("Gimme money!").assertExists()
    }

    @Test
    fun `Saved payment method mandates work correctly`() = runTest(
        customer = PaymentSheet.CustomerConfiguration(id = "cus_1", ephemeralKeySecret = "ek_test"),
        networkSetup = {
            setupElementsSessionsResponse(lpms = listOf("card", "us_bank_account"))
            setupV1PaymentMethodsResponse(usBankAccount1)
            setupV1PaymentMethodsResponse(card1, card2)
        },
    ) {
        verticalModePage.assertHasSavedPaymentMethods()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_12345")
        verticalModePage.assertPrimaryButton(isEnabled())
        verticalModePage.assertMandateDoesNotExists()

        verticalModePage.clickViewMore()
        managePage.waitUntilVisible()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_12345")
        managePage.selectPaymentMethod("pm_54321")

        verticalModePage.waitUntilVisible()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_54321")
        verticalModePage.assertPrimaryButton(isEnabled())
        verticalModePage.assertMandateExists()

        verticalModePage.clickViewMore()
        managePage.waitUntilVisible()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_54321")
        managePage.selectPaymentMethod("pm_12345")

        verticalModePage.waitUntilVisible()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_12345")
        verticalModePage.assertPrimaryButton(isEnabled())
        verticalModePage.assertMandateDoesNotExists()
    }

    @Test
    fun `Default saved payment method is loaded with mandate`() = runTest(
        customer = PaymentSheet.CustomerConfiguration(id = "cus_1", ephemeralKeySecret = "ek_test"),
        networkSetup = {
            setupElementsSessionsResponse(lpms = listOf("card", "us_bank_account"))
            setupV1PaymentMethodsResponse(usBankAccount1)
            setupV1PaymentMethodsResponse(type = "card")
        },
    ) {
        verticalModePage.assertHasSavedPaymentMethods()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_54321")
        verticalModePage.assertPrimaryButton(isEnabled())
        verticalModePage.assertMandateExists()
    }

    @Test
    fun `Manage screen should not display mandates`() = runTest(
        customer = PaymentSheet.CustomerConfiguration(id = "cus_1", ephemeralKeySecret = "ek_test"),
        networkSetup = {
            setupElementsSessionsResponse(lpms = listOf("card", "us_bank_account"))
            setupV1PaymentMethodsResponse(usBankAccount1, usBankAccount2)
            setupV1PaymentMethodsResponse(type = "card")
        },
    ) {
        verticalModePage.assertHasSavedPaymentMethods()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_54321")
        verticalModePage.assertPrimaryButton(isEnabled())
        verticalModePage.assertMandateExists()

        verticalModePage.clickViewMore()
        managePage.waitUntilVisible()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_54321")
        verticalModePage.assertMandateDoesNotExists()
        managePage.selectPaymentMethod("pm_09876")

        verticalModePage.waitUntilVisible()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_09876")
        verticalModePage.assertPrimaryButton(isEnabled())
        verticalModePage.assertMandateExists()
    }

    @OptIn(ExperimentalCardBrandFilteringApi::class)
    @Test
    fun `Entering Amex card shows disallowed error when disallowed`() = runTest(
        cardBrandAcceptance = PaymentSheet.CardBrandAcceptance.disallowed(
            listOf(

                PaymentSheet.CardBrandAcceptance.BrandCategory.Amex
            )
        ),
        networkSetup = {
            setupElementsSessionsResponse()
        },
    ) {
        verticalModePage.assertDoesNotHaveSavedPaymentMethods()
        verticalModePage.assertPrimaryButton(isNotEnabled())

        verticalModePage.clickOnNewLpm("card")
        formPage.waitUntilVisible()

        // Enter the start of an Amex card number
        formPage.fillCardNumber("3782")

        // Verify that the error message appears
        formPage.assertErrorExists("American Express is not accepted")
        verticalModePage.assertPrimaryButton(isNotEnabled())

        // Entering an accepted card brand (Visa) should be allowed
        formPage.fillOutCardDetails()
        verticalModePage.assertPrimaryButton(isEnabled())
    }

    @Test
    fun `Displayed saved payment method is correct when a card brand is disallowed`() = runTest(
        cardBrandAcceptance = PaymentSheet.CardBrandAcceptance.disallowed(
            listOf(

                PaymentSheet.CardBrandAcceptance.BrandCategory.Visa
            )
        ),
        customer = PaymentSheet.CustomerConfiguration(id = "cus_1", ephemeralKeySecret = "ek_test"),
        networkSetup = {
            setupElementsSessionsResponse()
            setupV1PaymentMethodsResponse(card1)
        },
    ) {
        // Saved Visa card should be filtered out
        verticalModePage.assertDoesNotHaveSavedPaymentMethods()
        verticalModePage.assertPrimaryButton(isNotEnabled())
    }

    @Test
    fun `Disallowed brands are hidden in the CBC dropdown`() = runTest(
        cardBrandAcceptance = PaymentSheet.CardBrandAcceptance.disallowed(
            listOf(

                PaymentSheet.CardBrandAcceptance.BrandCategory.Visa
            )
        ),
        customer = PaymentSheet.CustomerConfiguration(id = "cus_1", ephemeralKeySecret = "ek_test"),
        networkSetup = {
            setupElementsSessionsResponse(isCbcEligible = true)
            setupV1PaymentMethodsResponse(
                card1.copy(addCbcNetworks = true, brand = CardBrand.CartesBancaires),
                card2.copy(addCbcNetworks = true, brand = CardBrand.CartesBancaires)
            )
        },
    ) {
        verticalModePage.assertHasSavedPaymentMethods()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_12345", cardBrand = "cartes_bancaries")
        verticalModePage.assertPrimaryButton(isEnabled())
        verticalModePage.waitUntilVisible()

        verticalModePage.clickViewMore()
        managePage.waitUntilVisible()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_12345", cardBrand = "cartes_bancaries")
        managePage.clickEdit()
        managePage.clickEdit("pm_12345")

        editPage.assertIsVisible()

        // Even though our card is co-branded, Visa should not show up in the dropdown as it is disallowed
        editPage.assertNotInDropdown("Visa")
    }

    @OptIn(ExperimentalCardBrandFilteringApi::class)
    private fun runTest(
        primaryButtonLabel: String? = null,
        customer: PaymentSheet.CustomerConfiguration? = null,
        cardBrandAcceptance: PaymentSheet.CardBrandAcceptance = PaymentSheet.CardBrandAcceptance.all(),
        networkSetup: () -> Unit,
        initialLoadWaiter: () -> Unit = { verticalModePage.waitUntilVisible() },
        test: () -> Unit,
    ) {
        networkSetup()

        ActivityScenario.launch<PaymentSheetActivity>(
            PaymentSheetContractV2().createIntent(
                ApplicationProvider.getApplicationContext(),
                PaymentSheetContractV2.Args(
                    initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                        clientSecret = "pi_1234_secret_5678",
                    ),
                    config = PaymentSheet.Configuration.Builder(merchantDisplayName = "Merchant, Inc.")
                        .customer(customer)
                        .allowsDelayedPaymentMethods(true)
                        .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Vertical)
                        .cardBrandAcceptance(cardBrandAcceptance)
                        .apply {
                            if (primaryButtonLabel != null) {
                                primaryButtonLabel(primaryButtonLabel)
                            }
                        }
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
        type: String = paymentMethodDetails.first().type,
    ) {
        for (paymentMethodDetail in paymentMethodDetails) {
            // All types must match.
            assertThat(type).isEqualTo(paymentMethodDetail.type)
        }

        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/payment_methods"),
            query("type", type),
        ) { response ->
            val paymentMethodsArray = JSONArray()

            for (paymentMethodDetail in paymentMethodDetails) {
                paymentMethodsArray.put(paymentMethodDetail.createJson())
            }

            val paymentMethodsStringified = paymentMethodsArray.toString(2)

            val body = """
            {
              "object": "list",
              "data": $paymentMethodsStringified,
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
            response.setResponseCode(200)
        }
    }

    private fun setupPaymentMethodUpdateResponse(paymentMethodDetails: CardPaymentMethodDetails, cardBrand: String) {
        networkRule.enqueue(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_methods/${paymentMethodDetails.id}"),
            bodyPart(urlEncode("card[networks][preferred]"), cardBrand)
        ) { response ->
            response.setBody(
                paymentMethodDetails.createJson { originalCard ->
                    originalCard.copy(
                        card = originalCard.card!!.copy(
                            displayBrand = cardBrand
                        )
                    )
                }.toString(2)
            )
        }
    }

    sealed interface PaymentMethodDetails {
        val id: String
        val type: String

        fun createJson(transform: (PaymentMethod) -> PaymentMethod = { it }): JSONObject
    }

    private data class CardPaymentMethodDetails(
        override val id: String,
        val last4: String,
        val addCbcNetworks: Boolean = false,
        val brand: CardBrand = CardBrand.Visa
    ) : PaymentMethodDetails {
        override val type: String = "card"

        override fun createJson(transform: (PaymentMethod) -> PaymentMethod): JSONObject {
            val card = PaymentMethodFactory.card(
                id = id
            ).update(
                last4 = last4,
                addCbcNetworks = addCbcNetworks,
                brand = brand
            )
            return PaymentMethodFactory.convertCardToJson(transform(card))
        }
    }

    private data class UsBankPaymentMethodDetails(
        override val id: String,
    ) : PaymentMethodDetails {
        override val type: String = "us_bank_account"

        override fun createJson(transform: (PaymentMethod) -> PaymentMethod): JSONObject {
            return PaymentMethodFactory.convertUsBankAccountToJson(PaymentMethodFactory.usBankAccount().copy(id = id))
        }
    }
}
