package com.stripe.android.paymentsheet

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CustomPaymentMethodResult
import com.stripe.android.paymentelement.CustomPaymentMethodResultHandler
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.paymentsheet.utils.ProductIntegrationType
import com.stripe.android.paymentsheet.utils.ProductIntegrationTypeProvider
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.runProductIntegrationTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCustomPaymentMethodsApi::class)
@RunWith(TestParameterInjector::class)
internal class CustomPaymentMethodsTest {
    @get:Rule
    val testRules: TestRules = TestRules.create()

    @TestParameter(valuesProvider = ProductIntegrationTypeProvider::class)
    lateinit var integrationType: ProductIntegrationType

    private val applicationContext = ApplicationProvider.getApplicationContext<Context>()
    private val composeTestRule = testRules.compose
    private val networkRule = testRules.networkRule

    private val page = PaymentSheetPage(composeTestRule)

    @Test
    fun testSuccessful() {
        val customPaymentMethod = PaymentSheet.CustomPaymentMethod(
            id = "cpmt_123",
            subtitle = "Pay now",
            disableBillingDetailCollection = true,
        )

        var calledConfirmCallback = false
        var confirmedCustomPaymentMethod: PaymentSheet.CustomPaymentMethod? = null
        var confirmedBillingDetails: PaymentMethod.BillingDetails? = null

        runProductIntegrationTest(
            networkRule = networkRule,
            integrationType = integrationType,
            builder = {
                confirmCustomPaymentMethodCallback { customPaymentMethod, billingDetails ->
                    calledConfirmCallback = true
                    confirmedCustomPaymentMethod = customPaymentMethod
                    confirmedBillingDetails = billingDetails

                    CustomPaymentMethodResultHandler.handleCustomPaymentMethodResult(
                        context = applicationContext,
                        customPaymentMethodResult = CustomPaymentMethodResult.completed(),
                    )
                }
            },
            resultCallback = {
                assertThat(calledConfirmCallback).isTrue()
                assertThat(confirmedCustomPaymentMethod).isEqualTo(customPaymentMethod)
                assertThat(confirmedBillingDetails).isEqualTo(PaymentMethod.BillingDetails())

                assertCompleted(it)
            },
        ) { context ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("GET"),
                path("/v1/elements/sessions"),
            ) { response ->
                response.testBodyFromFile("elements-sessions-cpms.json")
            }

            context.launch(
                configuration = PaymentSheet.Configuration.Builder(merchantDisplayName = "Merchant, Inc.")
                    .customPaymentMethods(listOf(customPaymentMethod))
                    .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Horizontal)
                    .paymentMethodOrder(listOf("cpmt_123", "card"))
                    .build()
            )

            page.clickPrimaryButton()
        }
    }

    @Test
    fun testSuccessfulWithBillingDetailsCollection() {
        val customPaymentMethod = PaymentSheet.CustomPaymentMethod(
            id = "cpmt_123",
            subtitle = "Pay now",
            disableBillingDetailCollection = false,
        )

        var calledConfirmCallback = false
        var confirmedCustomPaymentMethod: PaymentSheet.CustomPaymentMethod? = null
        var confirmedBillingDetails: PaymentMethod.BillingDetails? = null

        runProductIntegrationTest(
            networkRule = networkRule,
            integrationType = integrationType,
            builder = {
                confirmCustomPaymentMethodCallback { customPaymentMethod, billingDetails ->
                    calledConfirmCallback = true
                    confirmedCustomPaymentMethod = customPaymentMethod
                    confirmedBillingDetails = billingDetails

                    CustomPaymentMethodResultHandler.handleCustomPaymentMethodResult(
                        context = applicationContext,
                        customPaymentMethodResult = CustomPaymentMethodResult.completed(),
                    )
                }
            },
            resultCallback = {
                assertThat(calledConfirmCallback).isTrue()
                assertThat(confirmedCustomPaymentMethod).isEqualTo(customPaymentMethod)

                assertThat(confirmedBillingDetails?.name).isEqualTo("John Doe")
                assertThat(confirmedBillingDetails?.email).isEqualTo("email@email.com")

                assertThat(confirmedBillingDetails?.address?.line1).isEqualTo("123 Apple Street")
                assertThat(confirmedBillingDetails?.address?.line2).isEmpty()
                assertThat(confirmedBillingDetails?.address?.city).isEqualTo("South San Francisco")
                assertThat(confirmedBillingDetails?.address?.state).isEqualTo("CA")
                assertThat(confirmedBillingDetails?.address?.postalCode).isEqualTo("12345")
                assertThat(confirmedBillingDetails?.address?.country).isEqualTo("US")

                assertCompleted(it)
            },
        ) { context ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("GET"),
                path("/v1/elements/sessions"),
            ) { response ->
                response.testBodyFromFile("elements-sessions-cpms.json")
            }

            context.launch(
                configuration = PaymentSheet.Configuration.Builder(merchantDisplayName = "Merchant, Inc.")
                    .customPaymentMethods(listOf(customPaymentMethod))
                    .billingDetailsCollectionConfiguration(
                        billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                            name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                            phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
                        )
                    )
                    .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Horizontal)
                    .paymentMethodOrder(listOf("cpmt_123", "card"))
                    .build()
            )

            page.fillOutBillingCollectionDetails()

            page.clickPrimaryButton()
        }
    }

    @Test
    fun testSuccessfulWithBillingDetailsCollectionDisabled() {
        val customPaymentMethod = PaymentSheet.CustomPaymentMethod(
            id = "cpmt_123",
            subtitle = "Pay now",
            disableBillingDetailCollection = true,
        )

        var calledConfirmCallback = false
        var confirmedCustomPaymentMethod: PaymentSheet.CustomPaymentMethod? = null
        var confirmedBillingDetails: PaymentMethod.BillingDetails? = null

        runProductIntegrationTest(
            networkRule = networkRule,
            integrationType = integrationType,
            builder = {
                confirmCustomPaymentMethodCallback { customPaymentMethod, billingDetails ->
                    calledConfirmCallback = true
                    confirmedCustomPaymentMethod = customPaymentMethod
                    confirmedBillingDetails = billingDetails

                    CustomPaymentMethodResultHandler.handleCustomPaymentMethodResult(
                        context = applicationContext,
                        customPaymentMethodResult = CustomPaymentMethodResult.completed(),
                    )
                }
            },
            resultCallback = {
                assertThat(calledConfirmCallback).isTrue()
                assertThat(confirmedCustomPaymentMethod).isEqualTo(customPaymentMethod)
                assertThat(confirmedBillingDetails).isEqualTo(PaymentMethod.BillingDetails())

                assertCompleted(it)
            },
        ) { context ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("GET"),
                path("/v1/elements/sessions"),
            ) { response ->
                response.testBodyFromFile("elements-sessions-cpms.json")
            }

            context.launch(
                configuration = PaymentSheet.Configuration.Builder(merchantDisplayName = "Merchant, Inc.")
                    .customPaymentMethods(listOf(customPaymentMethod))
                    .billingDetailsCollectionConfiguration(
                        billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                            name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                            phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
                        )
                    )
                    .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Horizontal)
                    .paymentMethodOrder(listOf("cpmt_123", "card"))
                    .build()
            )

            page.clickPrimaryButton()
        }
    }
}
