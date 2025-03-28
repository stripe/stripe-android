package com.stripe.android.paymentsheet

import androidx.compose.ui.test.hasTestTag
import androidx.test.espresso.intent.rule.IntentsRule
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.paymentsheet.paymentdatacollection.ach.TEST_TAG_ACCOUNT_DETAILS
import com.stripe.android.paymentsheet.paymentdatacollection.ach.TEST_TAG_BILLING_DETAILS
import com.stripe.android.paymentsheet.utils.ConfirmationType
import com.stripe.android.paymentsheet.utils.ConfirmationTypeProvider
import com.stripe.android.paymentsheet.utils.DefaultPaymentMethodsUtils
import com.stripe.android.paymentsheet.utils.IntegrationType
import com.stripe.android.paymentsheet.utils.PaymentMethodType
import com.stripe.android.paymentsheet.utils.PaymentMethodTypeProvider
import com.stripe.android.paymentsheet.utils.PaymentSheetLayoutType
import com.stripe.android.paymentsheet.utils.ProductIntegrationTestRunnerContext
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
    fun relaunchesIntoFormPageWithSaveForFutureUseChecked() = relaunchIntoFormPage(
        expectedSetAsDefault = false,
        firstLaunchBlock = { page ->
            page.assertSaveForFutureCheckboxNotChecked()
            page.assertNoSetAsDefaultCheckbox()

            page.checkSaveForFuture()

            page.assertSaveForFutureUseCheckboxChecked()
        },
        secondLaunchBlock = { page ->
            page.waitUntilVisible()
            page.assertSaveForFutureUseCheckboxChecked()
            page.assertSetAsDefaultCheckboxNotChecked()
        }
    )

    @Test
    fun relaunchesIntoFormPageWithBothChecked() = relaunchIntoFormPage(
        expectedSetAsDefault = true,
        firstLaunchBlock = { page ->
            page.assertSaveForFutureCheckboxNotChecked()
            page.assertNoSetAsDefaultCheckbox()

            page.checkSaveForFuture()
            page.checkSetAsDefaultCheckbox()

            page.assertSaveForFutureUseCheckboxChecked()
            page.assertSetAsDefaultCheckboxChecked()
        },
        secondLaunchBlock = { page ->
            page.waitUntilVisible()
            page.assertSaveForFutureUseCheckboxChecked()
            page.assertSetAsDefaultCheckboxChecked()
        }
    )

    private fun relaunchIntoFormPage(
        expectedSetAsDefault: Boolean,
        firstLaunchBlock: (PaymentSheetPage) -> Unit,
        secondLaunchBlock: (PaymentSheetPage) -> Unit,
    ) {
        runFlowControllerTest(
            networkRule = networkRule,
            createIntentCallback = confirmationType.createIntentCallback,
            callConfirmOnPaymentOptionCallback = false,
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

            page.clickOnLpm(paymentMethodType.type.code, forVerticalMode = true)
            composeTestRule.waitForIdle()

            if (paymentMethodType is PaymentMethodType.UsBankAccount) {
                composeTestRule.waitUntil(
                    timeoutMillis = 5000L
                ) {
                    composeTestRule.onAllNodes(
                        hasTestTag(TEST_TAG_BILLING_DETAILS)
                    ).fetchSemanticsNodes().isNotEmpty()
                }
                paymentMethodType.fillOutFormDetails(composeTestRule = composeTestRule)

                composeTestRule.waitUntil(
                    timeoutMillis = 5000L
                ) {
                    composeTestRule.onAllNodes(
                        hasTestTag(TEST_TAG_ACCOUNT_DETAILS)
                    ).fetchSemanticsNodes().isNotEmpty()
                }
            } else {
                paymentMethodType.fillOutFormDetails(composeTestRule = composeTestRule)
            }

            composeTestRule.waitForIdle()

            firstLaunchBlock(page)

            page.clickPrimaryButton()
            composeTestRule.waitForIdle()

            assertThat(testContext.configureCallbackTurbine.awaitItem()?.label).endsWith(paymentMethodType.getLast4())

            composeTestRule.waitForIdle()

            testContext.flowController.presentPaymentOptions()

            composeTestRule.waitForIdle()
            secondLaunchBlock(page)
            composeTestRule.waitForIdle()
            page.clickPrimaryButton()

            confirmationType.enqueuePaymentIntentConfirmWithExpectedSetAsDefault(
                networkRule = networkRule,
                paymentMethodType = paymentMethodType,
                setAsDefault = expectedSetAsDefault
            )
            testContext.flowController.confirm()
        }
    }

    private fun PaymentMethodType.getLast4(): String {
        if (this is PaymentMethodType.Card) {
            return "4242"
        } else {
            return "6789"
        }
    }
}
