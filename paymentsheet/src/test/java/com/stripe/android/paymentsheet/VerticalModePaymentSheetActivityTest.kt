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
import com.stripe.android.model.CardBrand
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.ResponseReplacement
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.RetryRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.paymentelementnetwork.CardPaymentMethodDetails
import com.stripe.paymentelementnetwork.UsBankPaymentMethodDetails
import com.stripe.paymentelementnetwork.setupPaymentMethodDetachResponse
import com.stripe.paymentelementnetwork.setupPaymentMethodUpdateResponse
import com.stripe.paymentelementnetwork.setupV1PaymentMethodsResponse
import com.stripe.paymentelementtestpages.EditPage
import com.stripe.paymentelementtestpages.FormPage
import com.stripe.paymentelementtestpages.ManagePage
import com.stripe.paymentelementtestpages.VerticalModePage
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class VerticalModePaymentSheetActivityTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()

    private val composeTestRule = createAndroidComposeRule<PaymentSheetActivity>()
    private val composeCleanupRule = createComposeCleanupRule()
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
        .outerRule(composeCleanupRule)
        .around(composeTestRule)
        .around(networkRule)
        .around(PaymentConfigurationTestRule(applicationContext))
        .around(RetryRule(3))

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
            networkRule.setupV1PaymentMethodsResponse(card1, card2)
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
            networkRule.setupV1PaymentMethodsResponse(type = "card")
        },
    ) {
        verticalModePage.assertIsNotVisible()
        verticalModePage.assertPrimaryButton(isNotEnabled())
        formPage.fillOutCardDetails()
        verticalModePage.assertPrimaryButton(isEnabled())
    }

    @Test
    fun `When the payment intent only has one LPM it launches directly into the form`() = runTest(
        billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
        ),
        initialLoadWaiter = { formPage.waitUntilVisible() },
        networkSetup = {
            setupElementsSessionsResponse(lpms = listOf("cashapp"))
        },
    ) {
        verticalModePage.assertIsNotVisible()
        formPage.fillOutEmail()
        verticalModePage.assertPrimaryButton(isEnabled())
    }

    @Test
    fun `Updates selected saved payment method`() = runTest(
        customer = PaymentSheet.CustomerConfiguration(id = "cus_1", ephemeralKeySecret = "ek_test"),
        networkSetup = {
            setupElementsSessionsResponse(lpms = listOf("card"))
            networkRule.setupV1PaymentMethodsResponse(card1, card2)
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
            networkRule.setupV1PaymentMethodsResponse(card1, card2)
            networkRule.setupPaymentMethodDetachResponse("pm_12345")
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
        editPage.waitUntilVisible()
        editPage.clickRemove()
        managePage.waitUntilVisible()
        managePage.waitUntilGone("pm_12345")
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
            networkRule.setupV1PaymentMethodsResponse(card1, card2)
            networkRule.setupPaymentMethodDetachResponse("pm_12345")
            networkRule.setupPaymentMethodDetachResponse("pm_67890")
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
        editPage.waitUntilMissing()

        verticalModePage.assertDoesNotHaveSavedPaymentMethods()
        verticalModePage.assertPrimaryButton(isNotEnabled())
    }

    @Test
    fun `Removing only card navigates back`() = runTest(
        customer = PaymentSheet.CustomerConfiguration(id = "cus_1", ephemeralKeySecret = "ek_test"),
        networkSetup = {
            setupElementsSessionsResponse()
            networkRule.setupV1PaymentMethodsResponse(card1)
            networkRule.setupPaymentMethodDetachResponse("pm_12345")
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

    @Test
    fun `Updating a card brand updates the icon in the list`() = runTest(
        customer = PaymentSheet.CustomerConfiguration(id = "cus_1", ephemeralKeySecret = "ek_test"),
        networkSetup = {
            setupElementsSessionsResponse(isCbcEligible = true)
            networkRule.setupV1PaymentMethodsResponse(
                card1.copy(addCbcNetworks = true),
                card2.copy(addCbcNetworks = true)
            )
            networkRule.setupPaymentMethodUpdateResponse(paymentMethodDetails = card1, cardBrand = "visa")
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
            networkRule.setupV1PaymentMethodsResponse(card1, card2)
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
            networkRule.setupV1PaymentMethodsResponse(card1, card2)
        },
    ) {
        verticalModePage.assertHasSavedPaymentMethods()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_12345")
        verticalModePage.assertPrimaryButton(isEnabled())

        verticalModePage.clickOnNewLpm("card")
        Espresso.onIdle()
        formPage.waitUntilVisible()
        Espresso.pressBack()

        verticalModePage.waitUntilVisible()
        verticalModePage.assertHasSelectedSavedPaymentMethod("pm_12345")
        verticalModePage.assertPrimaryButton(isEnabled())

        verticalModePage.clickOnNewLpm("cashapp")
        verticalModePage.assertLpmIsSelected("cashapp")

        verticalModePage.clickOnNewLpm("card")
        Espresso.onIdle()
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
            networkRule.setupV1PaymentMethodsResponse(card1, card2)
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
            networkRule.setupV1PaymentMethodsResponse(usBankAccount1)
            networkRule.setupV1PaymentMethodsResponse(card1, card2)
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
            networkRule.setupV1PaymentMethodsResponse(usBankAccount1)
            networkRule.setupV1PaymentMethodsResponse(type = "card")
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
            networkRule.setupV1PaymentMethodsResponse(usBankAccount1, usBankAccount2)
            networkRule.setupV1PaymentMethodsResponse(type = "card")
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
            networkRule.setupV1PaymentMethodsResponse(card1)
        },
    ) {
        // Saved Visa card should be filtered out
        verticalModePage.assertDoesNotHaveSavedPaymentMethods()
        verticalModePage.assertPrimaryButton(isNotEnabled())
    }

    @Test
    fun `Disallowed brands are disabled in the CBC dropdown`() = runTest(
        cardBrandAcceptance = PaymentSheet.CardBrandAcceptance.disallowed(
            listOf(

                PaymentSheet.CardBrandAcceptance.BrandCategory.Visa
            )
        ),
        customer = PaymentSheet.CustomerConfiguration(id = "cus_1", ephemeralKeySecret = "ek_test"),
        networkSetup = {
            setupElementsSessionsResponse(isCbcEligible = true)
            networkRule.setupV1PaymentMethodsResponse(
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
        editPage.assertInDropdownButDisabled("Visa (not accepted)")

        // Cartes Bancaires item should be in the drop down and selectable
        editPage.assertInDropdownAndEnabled("Cartes Bancaires")
    }

    private fun runTest(
        primaryButtonLabel: String? = null,
        customer: PaymentSheet.CustomerConfiguration? = null,
        billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration? = null,
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
                        .apply {
                            if (billingDetailsCollectionConfiguration != null) {
                                billingDetailsCollectionConfiguration(billingDetailsCollectionConfiguration)
                            }
                        }
                        .build(),
                    paymentElementCallbackIdentifier = PaymentSheetFixtures.PAYMENT_SHEET_CALLBACK_TEST_IDENTIFIER,
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
}
