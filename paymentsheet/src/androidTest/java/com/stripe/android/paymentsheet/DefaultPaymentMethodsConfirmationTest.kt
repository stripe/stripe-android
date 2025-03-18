package com.stripe.android.paymentsheet

import androidx.compose.ui.test.hasTestTag
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_OPTION_TAB_LAYOUT_TEST_TAG
import com.stripe.android.paymentsheet.utils.ConfirmationType
import com.stripe.android.paymentsheet.utils.ConfirmationTypeProvider
import com.stripe.android.paymentsheet.utils.DefaultPaymentMethodsUtils
import com.stripe.android.paymentsheet.utils.PaymentSheetLayoutType
import com.stripe.android.paymentsheet.utils.PaymentSheetLayoutTypeProvider
import com.stripe.android.paymentsheet.utils.ProductIntegrationTestRunnerContext
import com.stripe.android.paymentsheet.utils.ProductIntegrationType
import com.stripe.android.paymentsheet.utils.ProductIntegrationTypeProvider
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.runProductIntegrationTest
import com.stripe.android.testing.PaymentMethodFactory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCustomerSessionApi::class)
@RunWith(TestParameterInjector::class)
internal class DefaultPaymentMethodsConfirmationTest {

    @get:Rule
    val testRules: TestRules = TestRules.create()

    private val composeTestRule = testRules.compose
    private val networkRule = testRules.networkRule

    @TestParameter(valuesProvider = ProductIntegrationTypeProvider::class)
    lateinit var integrationType: ProductIntegrationType

    @TestParameter(valuesProvider = PaymentSheetLayoutTypeProvider::class)
    lateinit var layoutType: PaymentSheetLayoutType

    @TestParameter(valuesProvider = ConfirmationTypeProvider::class)
    lateinit var confirmationType: ConfirmationType

    @Test
    fun setNewCardAsDefault_withSavedPaymentMethods_andSetAsDefault() = runProductIntegrationTest(
        networkRule = networkRule,
        createIntentCallback = confirmationType.createIntentCallback,
        integrationType = integrationType,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        val paymentSheetPage = PaymentSheetPage(composeTestRule)

        val cards = listOf(
            PaymentMethodFactory.card(last4 = "4242", id = "pm_1"),
            PaymentMethodFactory.card(last4 = "1001", id = "pm_2")
        )

        DefaultPaymentMethodsUtils.enqueueElementsSessionResponse(
            networkRule = networkRule,
            cards = cards
        )

        launch(
            testContext = testContext,
            paymentMethodLayout = layoutType.paymentMethodLayout,
            hasSavedPaymentMethods = true,
        )

        layoutType.payWithNewCardWithSavedPaymentMethods(
            composeTestRule = composeTestRule,
        )

        paymentSheetPage.fillOutCardDetails()
        paymentSheetPage.checkSaveForFuture()
        paymentSheetPage.checkSetAsDefaultCheckbox()

        paymentSheetPage.assertSetAsDefaultCheckboxChecked()

        enqueuePaymentIntentConfirmWithExpectedSetAsDefault(setAsDefaultValue = true)

        paymentSheetPage.clickPrimaryButton()
    }

    @Test
    fun setNewCardAsDefault_withSavedPaymentMethods_uncheckSetAsDefault_doesNotSendSetAsDefaultParamInConfirmCall() =
        runProductIntegrationTest(
            networkRule = networkRule,
            createIntentCallback = confirmationType.createIntentCallback,
            integrationType = integrationType,
            resultCallback = ::assertCompleted,
        ) { testContext ->
            val paymentSheetPage = PaymentSheetPage(composeTestRule)

            val cards = listOf(
                PaymentMethodFactory.card(last4 = "4242", id = "pm_1"),
                PaymentMethodFactory.card(last4 = "1001", id = "pm_2")
            )

            DefaultPaymentMethodsUtils.enqueueElementsSessionResponse(
                networkRule = networkRule,
                cards = cards
            )

            launch(
                testContext = testContext,
                paymentMethodLayout = layoutType.paymentMethodLayout,
                hasSavedPaymentMethods = true,
            )

            layoutType.payWithNewCardWithSavedPaymentMethods(
                composeTestRule = composeTestRule,
            )

            paymentSheetPage.fillOutCardDetails()
            paymentSheetPage.checkSaveForFuture()

            enqueuePaymentIntentConfirmWithExpectedSetAsDefault(setAsDefaultValue = false)

            paymentSheetPage.clickPrimaryButton()
        }

    @Test
    fun setNewCardAsDefault_withSavedPaymentMethods_uncheckSaveForFuture_doesNotSendSetAsDefaultParamInConfirmCall() =
        runProductIntegrationTest(
            networkRule = networkRule,
            createIntentCallback = confirmationType.createIntentCallback,
            integrationType = integrationType,
            resultCallback = ::assertCompleted,
        ) { testContext ->
            val paymentSheetPage = PaymentSheetPage(composeTestRule)

            val cards = listOf(
                PaymentMethodFactory.card(last4 = "4242", id = "pm_1"),
                PaymentMethodFactory.card(last4 = "1001", id = "pm_2")
            )

            DefaultPaymentMethodsUtils.enqueueElementsSessionResponse(
                networkRule = networkRule,
                cards = cards
            )

            launch(
                testContext = testContext,
                paymentMethodLayout = layoutType.paymentMethodLayout,
                hasSavedPaymentMethods = true,
            )

            layoutType.payWithNewCardWithSavedPaymentMethods(
                composeTestRule = composeTestRule,
            )

            paymentSheetPage.fillOutCardDetails()
            paymentSheetPage.checkSaveForFuture()
            paymentSheetPage.checkSetAsDefaultCheckbox()
            paymentSheetPage.checkSaveForFuture()

            enqueuePaymentIntentConfirmWithoutSetAsDefault()

            paymentSheetPage.clickPrimaryButton()
        }

    @Test
    fun payWithNewCard_checkSaveForFuture_sendsSetAsDefaultInConfirmCall() = runProductIntegrationTest(
        networkRule = networkRule,
        createIntentCallback = confirmationType.createIntentCallback,
        integrationType = integrationType,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        val paymentSheetPage = PaymentSheetPage(composeTestRule)

        DefaultPaymentMethodsUtils.enqueueElementsSessionResponse(networkRule = networkRule)

        launch(
            testContext = testContext,
            paymentMethodLayout = layoutType.paymentMethodLayout,
            hasSavedPaymentMethods = false,
        )

        paymentSheetPage.fillOutCardDetails()
        paymentSheetPage.checkSaveForFuture()
        paymentSheetPage.assertNoSetAsDefaultCheckbox()

        enqueuePaymentIntentConfirmWithExpectedSetAsDefault(true)

        paymentSheetPage.clickPrimaryButton()
    }

    @Test
    fun payWithNewCard_uncheckSaveForFuture_doesNotSendSetAsDefaultInConfirmCall() = runProductIntegrationTest(
        networkRule = networkRule,
        createIntentCallback = confirmationType.createIntentCallback,
        integrationType = integrationType,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        val paymentSheetPage = PaymentSheetPage(composeTestRule)

        DefaultPaymentMethodsUtils.enqueueElementsSessionResponse(networkRule = networkRule)

        launch(
            testContext = testContext,
            paymentMethodLayout = layoutType.paymentMethodLayout,
            hasSavedPaymentMethods = false,
        )

        paymentSheetPage.fillOutCardDetails()
        paymentSheetPage.checkSaveForFuture()
        paymentSheetPage.checkSaveForFuture()
        paymentSheetPage.assertNoSetAsDefaultCheckbox()

        enqueuePaymentIntentConfirmWithoutSetAsDefault()

        paymentSheetPage.clickPrimaryButton()
    }


    private fun enqueuePaymentIntentConfirmWithoutSetAsDefault() {
        return networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
            bodyPart(urlEncode("payment_method_data[allow_redisplay]"), "unspecified"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }
    }

    private fun enqueuePaymentIntentConfirmWithExpectedSetAsDefault(setAsDefaultValue: Boolean) {
        return networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
            bodyPart(urlEncode("payment_method_data[allow_redisplay]"), "always"),
            bodyPart(urlEncode("set_as_default_payment_method"), setAsDefaultValue.toString())
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }
    }

    private fun launch(
        testContext: ProductIntegrationTestRunnerContext,
        paymentMethodLayout: PaymentSheet.PaymentMethodLayout,
        hasSavedPaymentMethods: Boolean = true,
    ) {
        testContext.launch(
            configuration = PaymentSheet.Configuration(
                merchantDisplayName = "Example, Inc.",
                paymentMethodLayout = paymentMethodLayout,
                customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                    id = "cus_1",
                    clientSecret = "cuss_1",
                )
            ),
        )

        if (
            paymentMethodLayout == PaymentSheet.PaymentMethodLayout.Horizontal &&
            hasSavedPaymentMethods
        ) {
            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                composeTestRule
                    .onAllNodes(hasTestTag(SAVED_PAYMENT_OPTION_TAB_LAYOUT_TEST_TAG))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
        }
    }
}