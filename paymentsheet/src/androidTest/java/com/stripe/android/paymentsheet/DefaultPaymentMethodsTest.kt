package com.stripe.android.paymentsheet

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.networktesting.RequestMatchers
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.DefaultPaymentMethodsUtils
import com.stripe.android.paymentsheet.utils.PaymentSheetLayoutType
import com.stripe.android.paymentsheet.utils.PaymentSheetLayoutTypeProvider
import com.stripe.android.paymentsheet.utils.ProductIntegrationType
import com.stripe.android.paymentsheet.utils.ProductIntegrationTypeProvider
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.runProductIntegrationTest
import com.stripe.android.testing.PaymentMethodFactory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class DefaultPaymentMethodsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @get:Rule
    val testRules: TestRules = TestRules.create()

    private val composeTestRule = testRules.compose
    private val networkRule = testRules.networkRule

    @TestParameter(valuesProvider = ProductIntegrationTypeProvider::class)
    lateinit var integrationType: ProductIntegrationType

    @TestParameter(valuesProvider = PaymentSheetLayoutTypeProvider::class)
    lateinit var layoutType: PaymentSheetLayoutType

    @Test
    fun setDefaultCard_selectsCard() = runProductIntegrationTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = {},
    ) { testContext ->
        val cards = listOf(
            PaymentMethodFactory.card(last4 = "4242", id = "pm_1"),
            PaymentMethodFactory.card(last4 = "1001", id = "pm_2")
        )

        DefaultPaymentMethodsUtils.enqueueElementsSessionResponse(
            networkRule = networkRule,
            cards = cards,
        )

        DefaultPaymentMethodsUtils.launch(
            testContext = testContext,
            composeTestRule = composeTestRule,
            paymentMethodLayout = layoutType.paymentMethodLayout,
        )

        val originallySelectedPaymentMethod = cards[0]
        val newDefaultPaymentMethod = cards[1]

        layoutType.assertHasSelectedPaymentMethod(
            composeTestRule = composeTestRule,
            context = context,
            paymentMethod = originallySelectedPaymentMethod,
        )

        enqueueSetDefaultPaymentMethodRequest()

        layoutType.setDefaultPaymentMethod(
            composeTestRule = composeTestRule,
            newDefaultPaymentMethod = newDefaultPaymentMethod,
        )

        layoutType.assertHasSelectedPaymentMethod(
            composeTestRule = composeTestRule,
            context = context,
            paymentMethod = newDefaultPaymentMethod,
        )

        testContext.markTestSucceeded()
    }

    @Test
    fun updatePaymentMethodScreen_defaultPaymentMethod_setAsDefaultCheckboxDisplayedAndDisabled() =
        runProductIntegrationTest(
            networkRule = networkRule,
            integrationType = integrationType,
            resultCallback = {},
        ) { testContext ->
            val cards = listOf(
                PaymentMethodFactory.card(last4 = "4242", id = "pm_1"),
                PaymentMethodFactory.card(last4 = "1001", id = "pm_2")
            )

            val defaultPaymentMethod = cards[0]

            DefaultPaymentMethodsUtils.enqueueElementsSessionResponse(
                networkRule = networkRule,
                cards = cards,
                defaultPaymentMethod = defaultPaymentMethod.id,
            )

            DefaultPaymentMethodsUtils.launch(
                testContext = testContext,
                composeTestRule = composeTestRule,
                paymentMethodLayout = layoutType.paymentMethodLayout,
            )

            layoutType.assertSetDefaultPaymentMethodCheckboxDisplayedAndDisabled(
                composeTestRule = composeTestRule,
                paymentMethod = defaultPaymentMethod,
            )

            testContext.markTestSucceeded()
        }

    private fun enqueueSetDefaultPaymentMethodRequest() {
        networkRule.enqueue(
            RequestMatchers.host("api.stripe.com"),
            method("POST"),
            path("/v1/elements/customers/cus_1/set_default_payment_method"),
        ) { response ->
            response.setResponseCode(200)
            response.testBodyFromFile("set-default-payment-method-success.json")
        }
    }

    @Test
    fun defaultPaymentMethod_displayedWithDefaultBadge() = runProductIntegrationTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = {},
    ) { testContext ->
        val cards = listOf(
            PaymentMethodFactory.card(last4 = "4242", id = "pm_1"),
            PaymentMethodFactory.card(last4 = "1001", id = "pm_2")
        )

        DefaultPaymentMethodsUtils.enqueueElementsSessionResponse(
            networkRule = networkRule,
            cards = cards,
            defaultPaymentMethod = cards.first().id
        )

        DefaultPaymentMethodsUtils.launch(
            testContext = testContext,
            composeTestRule = composeTestRule,
            paymentMethodLayout = layoutType.paymentMethodLayout,
        )

        layoutType.assertDefaultPaymentMethodBadgeDisplayed(composeTestRule)

        testContext.markTestSucceeded()
    }

    @Test
    fun defaultPaymentMethod_isSelected() = runProductIntegrationTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = {},
    ) { testContext ->
        val cards = listOf(
            PaymentMethodFactory.card(last4 = "4242", id = "pm_1"),
            PaymentMethodFactory.card(last4 = "1001", id = "pm_2")
        )
        val defaultPaymentMethod = cards[1]

        DefaultPaymentMethodsUtils.enqueueElementsSessionResponse(
            networkRule = networkRule,
            cards = cards,
            defaultPaymentMethod = defaultPaymentMethod.id
        )

        DefaultPaymentMethodsUtils.launch(
            testContext = testContext,
            composeTestRule = composeTestRule,
            paymentMethodLayout = layoutType.paymentMethodLayout,
        )

        layoutType.assertHasSelectedPaymentMethod(
            composeTestRule = composeTestRule,
            context = context,
            paymentMethod = defaultPaymentMethod,
        )

        testContext.markTestSucceeded()
    }
}
