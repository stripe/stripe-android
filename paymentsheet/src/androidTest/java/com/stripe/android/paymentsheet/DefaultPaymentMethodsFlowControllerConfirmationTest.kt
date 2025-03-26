package com.stripe.android.paymentsheet

import androidx.test.espresso.intent.rule.IntentsRule
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.paymentsheet.utils.ConfirmationType
import com.stripe.android.paymentsheet.utils.ConfirmationTypeProvider
import com.stripe.android.paymentsheet.utils.DefaultPaymentMethodsUtils
import com.stripe.android.paymentsheet.utils.IntegrationType
import com.stripe.android.paymentsheet.utils.PaymentMethodType
import com.stripe.android.paymentsheet.utils.PaymentMethodTypeProvider
import com.stripe.android.paymentsheet.utils.PaymentSheetLayoutType
import com.stripe.android.paymentsheet.utils.ProductIntegrationTestRunnerContext
import com.stripe.android.paymentsheet.utils.ProductIntegrationType
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.runFlowControllerTest
import com.stripe.android.testing.PaymentMethodFactory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class DefaultPaymentMethodsFlowControllerConfirmationTest {
    @get:Rule
    val testRules: TestRules = TestRules.create()

    private val composeTestRule = testRules.compose
    private val networkRule = testRules.networkRule

    @get:Rule
    val intentsRule = IntentsRule()

    @TestParameter(valuesProvider = ConfirmationTypeProvider::class)
    lateinit var confirmationType: ConfirmationType

    @TestParameter(valuesProvider = PaymentMethodTypeProvider::class)
    lateinit var paymentMethodType: PaymentMethodType

    // Confirmation behavior between horizontal and vertical doesn't differ, so we're testing with vertical mode only.
    private val layoutType: PaymentSheetLayoutType = PaymentSheetLayoutType.Vertical()

    @Test
    fun relaunchesIntoFormPageWithSaveForFutureUseChecked() = runFlowControllerTest(
        networkRule = networkRule,
        createIntentCallback = confirmationType.createIntentCallback,
        integrationType = IntegrationType.Compose,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        val page = PaymentSheetPage(composeTestRule)

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
            testContext = ProductIntegrationTestRunnerContext.WithFlowController(testContext),
            composeTestRule = composeTestRule,
            paymentMethodLayout = layoutType.paymentMethodLayout,
            hasSavedPaymentMethods = true,
            isDeferredIntent = confirmationType.isDeferredIntent,
            paymentMethodType = paymentMethodType,
        )

        page.clickOnLpm("card", forVerticalMode = true)

        page.assertSaveForFutureCheckboxNotChecked()
        page.assertNoSetAsDefaultCheckbox()

        page.fillOutCardDetails()
        page.checkSaveForFuture()

        page.assertSaveForFutureUseCheckboxChecked()

        confirmationType.enqueuePaymentIntentConfirmWithExpectedSetAsDefault(
            networkRule = networkRule,
            setAsDefault = false
        )
        page.clickPrimaryButton()

        assertThat(testContext.configureCallbackTurbine.awaitItem()?.label).endsWith("4242")

        composeTestRule.waitForIdle()

        testContext.flowController.presentPaymentOptions()

        page.assertSaveForFutureUseCheckboxChecked()
        page.assertSetAsDefaultCheckboxNotChecked()

        testContext.markTestSucceeded()
    }

    @Test
    fun relaunchesIntoFormPageWithBothChecked() = runFlowControllerTest(
        networkRule = networkRule,
        createIntentCallback = confirmationType.createIntentCallback,
        integrationType = IntegrationType.Compose,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        val page = PaymentSheetPage(composeTestRule)

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
            testContext = ProductIntegrationTestRunnerContext.WithFlowController(testContext),
            composeTestRule = composeTestRule,
            paymentMethodLayout = layoutType.paymentMethodLayout,
            hasSavedPaymentMethods = true,
            isDeferredIntent = confirmationType.isDeferredIntent,
            paymentMethodType = PaymentMethodType.Card,
        )

        page.clickOnLpm("card", forVerticalMode = true)

        page.assertSaveForFutureCheckboxNotChecked()
        page.assertNoSetAsDefaultCheckbox()

        page.fillOutCardDetails()
        page.checkSaveForFuture()
        page.checkSetAsDefaultCheckbox()

        page.assertSaveForFutureUseCheckboxChecked()
        page.assertSetAsDefaultCheckboxChecked()

        confirmationType.enqueuePaymentIntentConfirmWithExpectedSetAsDefault(
            networkRule = networkRule,
            setAsDefault = true
        )
        page.clickPrimaryButton()

        assertThat(testContext.configureCallbackTurbine.awaitItem()?.label).endsWith("4242")

        composeTestRule.waitForIdle()

        testContext.flowController.presentPaymentOptions()

        page.assertSaveForFutureUseCheckboxChecked()
        page.assertSetAsDefaultCheckboxChecked()

        testContext.markTestSucceeded()
    }
}
