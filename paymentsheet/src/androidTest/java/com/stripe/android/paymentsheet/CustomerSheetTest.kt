package com.stripe.android.paymentsheet

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.customersheet.CustomerSheetResult
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.RequestMatchers.query
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.IntegrationType
import com.stripe.android.paymentsheet.utils.IntegrationTypeProvider
import com.stripe.android.paymentsheet.utils.runCustomerSheetTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCustomerSheetApi::class)
@RunWith(TestParameterInjector::class)
internal class CustomerSheetTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val activityScenarioRule = composeTestRule.activityRule

    @get:Rule
    val networkRule = NetworkRule()

    private val page: CustomerSheetPage = CustomerSheetPage(composeTestRule)

    @TestParameter(valuesProvider = IntegrationTypeProvider::class)
    lateinit var integrationType: IntegrationType

    @Test
    fun testSuccessfulCardSave() = activityScenarioRule.runCustomerSheetTest(
        integrationType = integrationType,
        resultCallback = { result ->
            assertThat(result).isInstanceOf(CustomerSheetResult.Selected::class.java)
        }
    ) { context ->
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/payment_methods"),
            query("type", "card")
        ) { response ->
            response.testBodyFromFile("payment-methods-get-success-empty.json")
        }

        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/payment_methods"),
            query("type", "us_bank_account")
        ) { response ->
            response.testBodyFromFile("payment-methods-get-success-empty.json")
        }

        context.presentCustomerSheet()

        page.fillOutCardDetails()

        networkRule.enqueue(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_methods"),
            cardDetails(),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/setup_intents/seti_12345"),
            query("client_secret", "seti_12345_secret_12345"),
        ) { response ->
            response.testBodyFromFile("setup-intent-get.json")
        }

        networkRule.enqueue(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/setup_intents/seti_12345/confirm"),
            confirmParams()
        ) { response ->
            response.testBodyFromFile("setup-intent-confirm.json")
        }

        page.clickSaveButton()
        page.clickConfirmButton()
    }

    @Test
    fun testSuccessfulCardSaveWithCardBrandChoice() = activityScenarioRule.runCustomerSheetTest(
        integrationType = integrationType,
        resultCallback = { result ->
            assertThat(result).isInstanceOf(CustomerSheetResult.Selected::class.java)
        }
    ) { context ->
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method_with_cbc.json")
        }

        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/payment_methods"),
            query("type", "card")
        ) { response ->
            response.testBodyFromFile("payment-methods-get-success-empty.json")
        }

        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/payment_methods"),
            query("type", "us_bank_account")
        ) { response ->
            response.testBodyFromFile("payment-methods-get-success-empty.json")
        }

        context.presentCustomerSheet()

        page.fillOutCardDetails()
        page.changeCardBrandChoice()

        networkRule.enqueue(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_methods"),
            cardDetails(),
            cardBrandChoice(),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/setup_intents/seti_12345"),
            query("client_secret", "seti_12345_secret_12345"),
        ) { response ->
            response.testBodyFromFile("setup-intent-get.json")
        }

        networkRule.enqueue(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/setup_intents/seti_12345/confirm"),
            confirmParams(),
        ) { response ->
            response.testBodyFromFile("setup-intent-confirm.json")
        }

        page.clickSaveButton()
        page.clickConfirmButton()
    }

    @Test
    fun testCardNotSavedOnConfirmError() = activityScenarioRule.runCustomerSheetTest(
        integrationType = integrationType,
        resultCallback = {
            error("Shouldn't call CustomerSheetResultCallback")
        }
    ) { context ->
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/payment_methods"),
            query("type", "card")
        ) { response ->
            response.testBodyFromFile("payment-methods-get-success-empty.json")
        }

        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/payment_methods"),
            query("type", "us_bank_account")
        ) { response ->
            response.testBodyFromFile("payment-methods-get-success-empty.json")
        }

        context.presentCustomerSheet()

        page.fillOutCardDetails()

        networkRule.enqueue(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_methods"),
            cardDetails(),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/setup_intents/seti_12345"),
            query("client_secret", "seti_12345_secret_12345"),
        ) { response ->
            response.testBodyFromFile("setup-intent-get.json")
        }

        networkRule.enqueue(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/setup_intents/seti_12345/confirm"),
            confirmParams()
        ) { response ->
            response.setResponseCode(402)
            response.testBodyFromFile("setup-intent-confirm_incorrect_cvc.json")
        }

        page.clickSaveButton()

        page.waitForText("Your card's security code is incorrect.")

        context.markTestSucceeded()
    }

    private fun cardDetails(): RequestMatcher {
        return RequestMatchers.composite(
            bodyPart("type", "card"),
            bodyPart("card%5Bnumber%5D", CustomerSheetPage.CARD_NUMBER),
            bodyPart("card%5Bexp_month%5D", CustomerSheetPage.EXPIRY_MONTH),
            bodyPart("card%5Bexp_year%5D", CustomerSheetPage.EXPIRY_YEAR),
            bodyPart("card%5Bcvc%5D", CustomerSheetPage.CVC),
            bodyPart("billing_details%5Baddress%5D%5Bpostal_code%5D", CustomerSheetPage.ZIP_CODE),
            bodyPart("billing_details%5Baddress%5D%5Bcountry%5D", CustomerSheetPage.COUNTRY)
        )
    }

    private fun cardBrandChoice(): RequestMatcher {
        return RequestMatchers.composite(
            bodyPart(
                "card%5Bnetworks%5D%5Bpreferred%5D",
                "cartes_bancaires"
            )
        )
    }

    private fun confirmParams(): RequestMatcher {
        return RequestMatchers.composite(
            bodyPart("payment_method", "pm_12345"),
            bodyPart("client_secret", "seti_12345_secret_12345"),
            bodyPart("mandate_data%5Bcustomer_acceptance%5D%5Btype%5D", "online"),
            bodyPart("mandate_data%5Bcustomer_acceptance%5D%5Bonline%5D%5Binfer_from_client%5D", "true"),
        )
    }
}
