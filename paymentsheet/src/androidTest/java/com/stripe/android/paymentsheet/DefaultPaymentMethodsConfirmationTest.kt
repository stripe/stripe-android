package com.stripe.android.paymentsheet

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.paymentsheet.utils.ConfirmationType
import com.stripe.android.paymentsheet.utils.ConfirmationTypeProvider
import com.stripe.android.paymentsheet.utils.DefaultPaymentMethodsUtils
import com.stripe.android.paymentsheet.utils.PaymentSheetLayoutType
import com.stripe.android.paymentsheet.utils.ProductIntegrationType
import com.stripe.android.paymentsheet.utils.ProductIntegrationTypeProvider
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.runProductIntegrationTest
import com.stripe.android.testing.PaymentMethodFactory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class DefaultPaymentMethodsConfirmationTest {

    @get:Rule
    val testRules: TestRules = TestRules.create()

    private val composeTestRule = testRules.compose
    private val networkRule = testRules.networkRule

    @TestParameter(valuesProvider = ProductIntegrationTypeProvider::class)
    lateinit var integrationType: ProductIntegrationType

    @TestParameter(valuesProvider = ConfirmationTypeProvider::class)
    lateinit var confirmationType: ConfirmationType

    // Confirmation behavior between horizontal and vertical doesn't differ, so we're testing with vertical mode only.
    private val layoutType: PaymentSheetLayoutType = PaymentSheetLayoutType.Vertical()

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
            cards = cards,
            isDeferredIntent = confirmationType.isDeferredIntent,
        )

        DefaultPaymentMethodsUtils.launch(
            testContext = testContext,
            composeTestRule = composeTestRule,
            paymentMethodLayout = layoutType.paymentMethodLayout,
            hasSavedPaymentMethods = true,
            isDeferredIntent = confirmationType.isDeferredIntent,
        )

        layoutType.payWithNewCardWithSavedPaymentMethods(
            composeTestRule = composeTestRule,
        )

        paymentSheetPage.fillOutCardDetails()
        paymentSheetPage.checkSaveForFuture()
        paymentSheetPage.checkSetAsDefaultCheckbox()

        paymentSheetPage.assertSetAsDefaultCheckboxChecked()

        confirmationType.enqueuePaymentIntentConfirmWithExpectedSetAsDefault(
            networkRule = networkRule,
            setAsDefault = true,
        )

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
                cards = cards,
                isDeferredIntent = confirmationType.isDeferredIntent,
            )

            DefaultPaymentMethodsUtils.launch(
                testContext = testContext,
                composeTestRule = composeTestRule,
                paymentMethodLayout = layoutType.paymentMethodLayout,
                hasSavedPaymentMethods = true,
                isDeferredIntent = confirmationType.isDeferredIntent,
            )

            layoutType.payWithNewCardWithSavedPaymentMethods(
                composeTestRule = composeTestRule,
            )

            paymentSheetPage.fillOutCardDetails()
            paymentSheetPage.checkSaveForFuture()

            confirmationType.enqueuePaymentIntentConfirmWithExpectedSetAsDefault(
                networkRule = networkRule,
                setAsDefault = false
            )

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
                cards = cards,
                isDeferredIntent = confirmationType.isDeferredIntent,
            )

            DefaultPaymentMethodsUtils.launch(
                testContext = testContext,
                composeTestRule = composeTestRule,
                paymentMethodLayout = layoutType.paymentMethodLayout,
                hasSavedPaymentMethods = true,
                isDeferredIntent = confirmationType.isDeferredIntent,
            )

            layoutType.payWithNewCardWithSavedPaymentMethods(
                composeTestRule = composeTestRule,
            )

            paymentSheetPage.fillOutCardDetails()
            paymentSheetPage.checkSaveForFuture()
            paymentSheetPage.checkSetAsDefaultCheckbox()
            paymentSheetPage.checkSaveForFuture()

            confirmationType.enqueuePaymentIntentConfirmWithoutSetAsDefault(
                networkRule = networkRule,
            )

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

        DefaultPaymentMethodsUtils.enqueueElementsSessionResponse(
            networkRule = networkRule,
            isDeferredIntent = confirmationType.isDeferredIntent,
        )

        DefaultPaymentMethodsUtils.launch(
            testContext = testContext,
            composeTestRule = composeTestRule,
            paymentMethodLayout = layoutType.paymentMethodLayout,
            hasSavedPaymentMethods = false,
            isDeferredIntent = confirmationType.isDeferredIntent,
        )

        paymentSheetPage.fillOutCardDetails()
        paymentSheetPage.checkSaveForFuture()
        paymentSheetPage.assertNoSetAsDefaultCheckbox()

        confirmationType.enqueuePaymentIntentConfirmWithExpectedSetAsDefault(
            networkRule = networkRule,
            setAsDefault = true,
        )

        paymentSheetPage.clickPrimaryButton()
    }

    @Test
    fun payWithNewCard_doNotSaveCard_doesNotSendSetAsDefaultInConfirmCall() = runProductIntegrationTest(
        networkRule = networkRule,
        createIntentCallback = confirmationType.createIntentCallback,
        integrationType = integrationType,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        val paymentSheetPage = PaymentSheetPage(composeTestRule)

        DefaultPaymentMethodsUtils.enqueueElementsSessionResponse(
            networkRule = networkRule,
            isDeferredIntent = confirmationType.isDeferredIntent,
        )

        DefaultPaymentMethodsUtils.launch(
            testContext = testContext,
            composeTestRule = composeTestRule,
            paymentMethodLayout = layoutType.paymentMethodLayout,
            hasSavedPaymentMethods = false,
            isDeferredIntent = confirmationType.isDeferredIntent,
        )

        paymentSheetPage.fillOutCardDetails()
        paymentSheetPage.checkSaveForFuture()
        paymentSheetPage.checkSaveForFuture()
        paymentSheetPage.assertNoSetAsDefaultCheckbox()

        confirmationType.enqueuePaymentIntentConfirmWithoutSetAsDefault(
            networkRule = networkRule,
        )

        paymentSheetPage.clickPrimaryButton()
    }
}
