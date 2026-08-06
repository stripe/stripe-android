package com.stripe.android.paymentsheet

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithTag
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.ui.CUSTOMER_SHEET_SAVE_BUTTON_TEST_TAG
import com.stripe.android.networktesting.elementsSession
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.CustomerSheetTestType
import com.stripe.android.paymentsheet.utils.CustomerSheetUtils
import com.stripe.android.paymentsheet.utils.IntegrationType
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.runCustomerSheetTest
import org.junit.Rule
import org.junit.Test

@RequiresIme
internal class CustomerSheetKeyboardTest {
    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val testRules: TestRules = TestRules.create(composeTestRule = composeTestRule)

    private val networkRule = testRules.networkRule
    private val page = CustomerSheetPage(composeTestRule)

    @Test
    fun saveButtonIsVisibleAboveKeyboardWhenCardFormBecomesComplete() = runCustomerSheetTest(
        scenario = composeTestRule.activityRule.scenario,
        networkRule = networkRule,
        integrationType = IntegrationType.Activity,
        customerSheetTestType = CustomerSheetTestType.AttachToSetupIntent,
        configuration = CustomerSheet.Configuration.builder(merchantDisplayName = "Merchant Inc.")
            .defaultBillingDetails(defaultBillingDetailsWithoutPostalCode)
            .billingDetailsCollectionConfiguration(fullAddressCollection)
            .build(),
        resultCallback = { error("CustomerSheet should not return a result") },
    ) { context ->
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }
        CustomerSheetUtils.enqueueFetchRequests(networkRule = networkRule, withCards = false)

        context.presentCustomerSheet()

        page.fillOutCardDetails(fillOutZipCode = false)
        page.assertSaveButtonDisabled()
        page.focusZipCode()
        composeTestRule.waitForKeyboardToBeVisible()
        composeTestRule.onNodeWithTag(CUSTOMER_SHEET_SAVE_BUTTON_TEST_TAG)
            .assertIsNotDisplayed()

        page.enterZipCode()
        page.assertSaveButtonEnabled()
        composeTestRule.assertNodeWithTagVisibleAboveKeyboard(
            testTag = CUSTOMER_SHEET_SAVE_BUTTON_TEST_TAG,
        )

        context.markTestSucceeded()
    }

    private companion object {
        val defaultBillingDetailsWithoutPostalCode = PaymentSheet.BillingDetails(
            address = PaymentSheet.Address(
                line1 = "123 Main Street",
                city = "San Francisco",
                state = "CA",
                country = "US",
            ),
        )
        val fullAddressCollection = PaymentSheet.BillingDetailsCollectionConfiguration(
            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
        )
    }
}
