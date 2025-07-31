package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.RequestMatchers.query
import com.stripe.android.networktesting.ResponseReplacement
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.EmbeddedContentPage
import com.stripe.android.paymentelement.EmbeddedFormPage
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.elements.Address
import com.stripe.android.paymentelement.assertCompleted
import com.stripe.android.paymentelement.runEmbeddedPaymentElementTest
import com.stripe.android.elements.AddressDetails
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.runFlowControllerTest
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(SharedPaymentTokenSessionPreview::class)
@RunWith(TestParameterInjector::class)
internal class PreparePaymentMethodTest {
    @get:Rule
    val testRules: TestRules = TestRules.create()

    private val composeTestRule = testRules.compose
    private val networkRule = testRules.networkRule

    private val embeddedContentPage = EmbeddedContentPage(composeTestRule)
    private val embeddedFormPage = EmbeddedFormPage(testRules.compose)

    private val paymentSheetPage: PaymentSheetPage = PaymentSheetPage(composeTestRule)

    @Test
    fun withPaymentSheet() {
        val completablePaymentMethod = CompletableDeferred<PaymentMethod>()
        val completableShippingAddress = CompletableDeferred<AddressDetails?>()

        runPaymentSheetTest(
            networkRule = networkRule,
            builder = {
                preparePaymentMethodHandler { paymentMethod, shippingAddress ->
                    completablePaymentMethod.complete(paymentMethod)
                    completableShippingAddress.complete(shippingAddress)
                }
            },
            resultCallback = ::assertCompleted,
        ) { context ->
            enqueueElementsSession(
                networkId = "network_123",
                externalId = "external_123",
            )

            context.presentPaymentSheet {
                presentWithIntentConfiguration(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        sharedPaymentTokenSessionWithMode = PaymentSheet.IntentConfiguration.Mode.Payment(
                            amount = 5000L,
                            currency = "USD",
                        ),
                        sellerDetails = PaymentSheet.IntentConfiguration.SellerDetails(
                            networkId = "network_123",
                            externalId = "external_123",
                        )
                    ),
                    configuration = PaymentSheet.Configuration.Builder(merchantDisplayName = "Example, Inc.")
                        .shippingDetails(SHIPPING_ADDRESS)
                        .build()
                )
            }

            paymentSheetPage.fillOutCardDetails()

            enqueuePaymentMethodCreation()

            paymentSheetPage.clickPrimaryButton()

            val providedPaymentMethod = completablePaymentMethod.await()

            assertThat(providedPaymentMethod.id).isEqualTo("pm_12345")
            assertThat(providedPaymentMethod.type).isEqualTo(PaymentMethod.Type.Card)

            val shippingAddress = completableShippingAddress.await()

            assertThat(shippingAddress).isEqualTo(SHIPPING_ADDRESS)
        }
    }

    @Test
    fun withFlowController() {
        val completablePaymentMethod = CompletableDeferred<PaymentMethod>()
        val completableShippingAddress = CompletableDeferred<AddressDetails?>()
        val completableFlow = CompletableDeferred<Unit>()

        runFlowControllerTest(
            networkRule = networkRule,
            builder = {
                preparePaymentMethodHandler { paymentMethod, shippingAddress ->
                    completablePaymentMethod.complete(paymentMethod)
                    completableShippingAddress.complete(shippingAddress)
                }
            },
            resultCallback = {
                assertCompleted(it)
                completableFlow.complete(Unit)
            },
        ) { context ->
            enqueueElementsSession(
                networkId = "network_456",
                externalId = "external_456",
            )

            context.configureFlowController {
                configureWithIntentConfiguration(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        sharedPaymentTokenSessionWithMode = PaymentSheet.IntentConfiguration.Mode.Payment(
                            amount = 5000L,
                            currency = "USD",
                        ),
                        sellerDetails = PaymentSheet.IntentConfiguration.SellerDetails(
                            networkId = "network_456",
                            externalId = "external_456",
                        )
                    ),
                    configuration = PaymentSheet.Configuration.Builder(merchantDisplayName = "Example, Inc.")
                        .shippingDetails(SHIPPING_ADDRESS)
                        .build(),
                    callback = { success, error ->
                        assertThat(success).isTrue()
                        assertThat(error).isNull()
                        presentPaymentOptions()
                    }
                )
            }

            paymentSheetPage.fillOutCardDetails()

            enqueuePaymentMethodCreation()

            paymentSheetPage.clickPrimaryButton()

            val paymentOption = context.configureCallbackTurbine.awaitItem()

            assertThat(paymentOption?.label).endsWith("4242")
            assertThat(paymentOption?.paymentMethodType).isEqualTo("card")

            composeTestRule.waitForIdle()

            val providedPaymentMethod = completablePaymentMethod.await()

            assertThat(providedPaymentMethod.id).isEqualTo("pm_12345")
            assertThat(providedPaymentMethod.type).isEqualTo(PaymentMethod.Type.Card)

            val shippingAddress = completableShippingAddress.await()

            assertThat(shippingAddress).isEqualTo(SHIPPING_ADDRESS)

            completableFlow.await()

            assertThat(context.flowController.getPaymentOption()).isNotNull()
        }
    }

    @Test
    fun withEmbedded() {
        val completablePaymentMethod = CompletableDeferred<PaymentMethod>()
        val completableShippingAddress = CompletableDeferred<AddressDetails?>()

        runEmbeddedPaymentElementTest(
            networkRule = networkRule,
            builderInstance = EmbeddedPaymentElement.Builder(
                preparePaymentMethodHandler = { paymentMethod, shippingAddress ->
                    completablePaymentMethod.complete(paymentMethod)
                    completableShippingAddress.complete(shippingAddress)
                },
                resultCallback = ::assertCompleted
            ),
        ) { context ->
            enqueueElementsSession(
                networkId = "network_789",
                externalId = "external_789",
            )

            context.embeddedPaymentElement.configure(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    sharedPaymentTokenSessionWithMode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 5000L,
                        currency = "USD",
                    ),
                    sellerDetails = PaymentSheet.IntentConfiguration.SellerDetails(
                        networkId = "network_789",
                        externalId = "external_789",
                    )
                ),
                configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
                    .shippingDetails(SHIPPING_ADDRESS)
                    .formSheetAction(EmbeddedPaymentElement.FormSheetAction.Confirm)
                    .build()
            )

            embeddedContentPage.clickOnLpm(code = "card")
            embeddedFormPage.fillOutCardDetails()

            enqueuePaymentMethodCreation()

            embeddedFormPage.clickPrimaryButton()
            embeddedFormPage.waitUntilMissing()

            val providedPaymentMethod = completablePaymentMethod.await()

            assertThat(providedPaymentMethod.id).isEqualTo("pm_12345")
            assertThat(providedPaymentMethod.type).isEqualTo(PaymentMethod.Type.Card)

            val shippingAddress = completableShippingAddress.await()

            assertThat(shippingAddress).isEqualTo(SHIPPING_ADDRESS)
        }
    }

    private fun enqueueElementsSession(
        networkId: String,
        externalId: String,
    ) {
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
            query(urlEncode("seller_details[network_id]"), networkId),
            query(urlEncode("seller_details[external_id]"), externalId),
        ) { response ->
            response.testBodyFromFile(
                filename = "elements-sessions-requires_pm_with_cs.json",
                replacements = listOf(
                    ResponseReplacement(
                        original = "[PAYMENT_METHODS_HERE]",
                        new = "[]",
                    ),
                    ResponseReplacement(
                        original = "CARD_BRAND_CHOICE_ELIGIBILITY",
                        new = "false",
                    ),
                    ResponseReplacement(
                        original = "PAYMENT_METHOD_SYNC_DEFAULT_FEATURE",
                        new = "disabled",
                    ),
                    ResponseReplacement(
                        original = "DEFAULT_PAYMENT_METHOD",
                        new = "null",
                    )
                ),
            )
        }
    }

    private fun enqueuePaymentMethodCreation() {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }
    }

    private companion object {
        val SHIPPING_ADDRESS = AddressDetails(
            address = Address(
                city = "South San Francisc",
                line1 = "123 Apple Street",
                line2 = "Unit #2",
                state = "CA",
                postalCode = "99999",
                country = "US"
            ),
            phoneNumber = "11234567890",
            name = "John Doe"
        )
    }
}
