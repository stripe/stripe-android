package com.stripe.android.paymentelement

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.intent.rule.IntentsRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.RequestMatchers.query
import com.stripe.android.networktesting.TestApiKeys
import com.stripe.android.networktesting.elementsSession
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.UsBankAccountFormTestUtils
import com.stripe.paymentelementnetwork.CardPaymentMethodDetails
import com.stripe.paymentelementnetwork.setupPaymentMethodDetachResponse
import com.stripe.paymentelementnetwork.setupV1PaymentMethodsResponse
import com.stripe.paymentelementtestpages.EditPage
import com.stripe.paymentelementtestpages.ManagePage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Rule
import org.junit.Test

internal class EmbeddedPaymentElementTest {
    private val networkRule = NetworkRule()

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule) {
        around(IntentsRule())
    }

    private val embeddedContentPage = EmbeddedContentPage(testRules.compose)
    private val managePage = ManagePage(testRules.compose)
    private val editPage = EditPage(testRules.compose)
    private val formPage = EmbeddedFormPage(testRules.compose)

    private val card1 = CardPaymentMethodDetails("pm_12345", "4242")
    private val card2 = CardPaymentMethodDetails("pm_67890", "5544")

    private val paymentWithSetupFutureUsageIntentConfiguration = PaymentSheet.IntentConfiguration(
        mode = PaymentSheet.IntentConfiguration.Mode.Payment(
            amount = 5000,
            currency = "USD",
            setupFutureUse = PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession
        )
    )

    @After
    fun teardown() {
        GooglePayRepository.resetFactory()
    }

    @Test
    fun testSuccessfulCardPayment_withFormSheetActionConfirm() = runEmbeddedPaymentElementTest(
        networkRule = networkRule,
        createIntentCallback = { _, shouldSavePaymentMethod ->
            assertThat(shouldSavePaymentMethod).isFalse()
            CreateIntentResult.Success("pi_example_secret_12345")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.configure {
            formSheetAction(EmbeddedPaymentElement.FormSheetAction.Confirm)
        }

        embeddedContentPage.clickOnLpm("card")
        formPage.fillOutCardDetails()

        enqueueDeferredIntentConfirmationRequests()

        formPage.clickPrimaryButton()
        formPage.waitUntilMissing()
    }

    @Test
    fun testSuccessfulCardPayment_withFormSheetActionContinue() = runEmbeddedPaymentElementTest(
        networkRule = networkRule,
        createIntentCallback = { _, shouldSavePaymentMethod ->
            assertThat(shouldSavePaymentMethod).isFalse()
            CreateIntentResult.Success("pi_example_secret_12345")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.configure {
            formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue)
        }

        embeddedContentPage.clickOnLpm("card")
        formPage.fillOutCardDetails()
        formPage.clickPrimaryButton()
        formPage.waitUntilMissing()
        testContext.consumePaymentOptionEvent("card", "4242")

        embeddedContentPage.assertHasSelectedLpm("card")
        embeddedContentPage.clickOnLpm("card")
        formPage.clickPrimaryButton() // Ensures the form has the previous values.
        formPage.waitUntilMissing()

        enqueueDeferredIntentConfirmationRequests()

        testContext.confirm()
        assertThat(testContext.paymentOptionTurbine.awaitItem()).isNull()
    }

    @Test
    fun testCardFormValidation() = runEmbeddedPaymentElementTest(
        networkRule = networkRule,
        createIntentCallback = { _, shouldSavePaymentMethod ->
            assertThat(shouldSavePaymentMethod).isFalse()
            CreateIntentResult.Success("pi_example_secret_12345")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.configure {
            formSheetAction(EmbeddedPaymentElement.FormSheetAction.Confirm)
        }

        embeddedContentPage.clickOnLpm("card")
        formPage.clickDisabledPrimaryButton()
        formPage.assertCardNumberError("This field cannot be blank.")

        formPage.fillOutCardDetails()
        enqueueDeferredIntentConfirmationRequests()
        formPage.clickPrimaryButton()
        formPage.waitUntilMissing()
    }

    @Test
    fun testRemoveCard() = runEmbeddedPaymentElementTest(
        networkRule = networkRule,
        createIntentCallback = { _, shouldSavePaymentMethod ->
            assertThat(shouldSavePaymentMethod).isFalse()
            CreateIntentResult.Success("pi_example_secret_12345")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-deferred_payment_intent_no_link.json")
        }
        networkRule.setupV1PaymentMethodsResponse(card1, card2)
        networkRule.setupV1PaymentMethodsResponse(type = PaymentMethod.Type.USBankAccount.code)
        networkRule.setupV1PaymentMethodsResponse(type = PaymentMethod.Type.SepaDebit.code)

        testContext.configure {
            customer(PaymentSheet.CustomerConfiguration("cus_123", TestApiKeys.EPHEMERAL))
        }
        testContext.consumePaymentOptionEvent("card", "4242")

        embeddedContentPage.clickViewMore()

        managePage.waitUntilVisible()
        managePage.clickEdit()
        managePage.clickEdit(card1.id)
        editPage.waitUntilVisible()

        networkRule.setupPaymentMethodDetachResponse(card1.id)

        editPage.clickRemove()
        managePage.waitUntilVisible()
        managePage.waitUntilGone(card1.id)
        managePage.clickDone()

        testContext.markTestSucceeded()
    }

    @Test
    fun testRemoveCardPreservesPreviousSelection() = runEmbeddedPaymentElementTest(
        networkRule = networkRule,
        createIntentCallback = { _, shouldSavePaymentMethod ->
            assertThat(shouldSavePaymentMethod).isFalse()
            CreateIntentResult.Success("pi_example_secret_12345")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-deferred_payment_intent_no_link.json")
        }
        networkRule.setupV1PaymentMethodsResponse(card1)
        networkRule.setupV1PaymentMethodsResponse(type = PaymentMethod.Type.USBankAccount.code)
        networkRule.setupV1PaymentMethodsResponse(type = PaymentMethod.Type.SepaDebit.code)

        testContext.configure {
            customer(PaymentSheet.CustomerConfiguration("cus_123", TestApiKeys.EPHEMERAL))
        }
        testContext.consumePaymentOptionEvent("card", "4242")

        embeddedContentPage.clickOnLpm("cashapp")
        testContext.consumePaymentOptionEvent("cashapp", "Cash App Pay")
        embeddedContentPage.clickEdit()

        editPage.waitUntilVisible()

        networkRule.setupPaymentMethodDetachResponse(card1.id)

        editPage.clickRemove()
        editPage.waitUntilMissing()

        embeddedContentPage.assertHasSelectedLpm("cashapp")

        testContext.markTestSucceeded()
    }

    @Test
    fun testStateCanBeTakenFromOneInstanceToAnother() {
        var state: EmbeddedPaymentElement.State? = null

        // Instance 1
        runEmbeddedPaymentElementTest(
            networkRule = networkRule,
            createIntentCallback = { _, shouldSavePaymentMethod ->
                assertThat(shouldSavePaymentMethod).isFalse()
                CreateIntentResult.Success("pi_example_secret_12345")
            },
            resultCallback = ::assertCompleted,
        ) { testContext ->
            networkRule.elementsSession { response ->
                response.testBodyFromFile("elements-sessions-deferred_payment_intent_no_link.json")
            }
            networkRule.setupV1PaymentMethodsResponse(card1, card2)
            networkRule.setupV1PaymentMethodsResponse(type = PaymentMethod.Type.USBankAccount.code)
            networkRule.setupV1PaymentMethodsResponse(type = PaymentMethod.Type.SepaDebit.code)

            testContext.configure {
                customer(PaymentSheet.CustomerConfiguration("cus_123", TestApiKeys.EPHEMERAL))
                formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue)
            }
            testContext.consumePaymentOptionEvent("card", "4242")

            state = testContext.embeddedPaymentElement.state
            assertThat(state.paymentMethods()).hasSize(2)

            testContext.markTestSucceeded()
        }

        // Instance 2 - no network requests, no configure call -- just a state set.
        runEmbeddedPaymentElementTest(
            networkRule = networkRule,
            createIntentCallback = { _, shouldSavePaymentMethod ->
                assertThat(shouldSavePaymentMethod).isFalse()
                CreateIntentResult.Success("pi_example_secret_12345")
            },
            resultCallback = ::assertCompleted,
        ) { testContext ->
            withContext(Dispatchers.Main) {
                testContext.embeddedPaymentElement.state = state
            }
            assertThat(testContext.paymentOptionTurbine.awaitItem()?.paymentMethodType).isEqualTo("card")

            embeddedContentPage.clickViewMore()

            testContext.markTestSucceeded()
        }
    }

    @Test
    fun testEmbeddedPaymentElementDisplaysMandate() = runEmbeddedPaymentElementTest(
        networkRule = networkRule,
        createIntentCallback = { _, shouldSavePaymentMethod ->
            assertThat(shouldSavePaymentMethod).isFalse()
            CreateIntentResult.Success("pi_example_secret_12345")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.configure(intentConfiguration = paymentWithSetupFutureUsageIntentConfiguration)

        embeddedContentPage.clickOnLpm("card")
        formPage.assertMandateIsShown()
        testContext.markTestSucceeded()
    }

    @Test
    fun testEmbeddedPaymentElementWithTermsDisplayNeverDoesNotDisplayMandate() = runEmbeddedPaymentElementTest(
        networkRule = networkRule,
        createIntentCallback = { _, shouldSavePaymentMethod ->
            assertThat(shouldSavePaymentMethod).isFalse()
            CreateIntentResult.Success("pi_example_secret_12345")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.configure(intentConfiguration = paymentWithSetupFutureUsageIntentConfiguration) {
            termsDisplay(
                mapOf(
                    PaymentMethod.Type.Card to PaymentSheet.TermsDisplay.NEVER
                )
            )
        }

        embeddedContentPage.clickOnLpm("card")
        formPage.assertMandateIsMissing()
        testContext.markTestSucceeded()
    }

    @Test
    fun testOBO_PassedToElementsSessionCall() = runEmbeddedPaymentElementTest(
        networkRule = networkRule,
        createIntentCallback = { _, shouldSavePaymentMethod ->
            assertThat(shouldSavePaymentMethod).isFalse()
            CreateIntentResult.Success("pi_example_secret_12345")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        val oboMerchantID = "acct_connected_1234"
        networkRule.elementsSession(
            query("deferred_intent[on_behalf_of]", oboMerchantID)
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.configure(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                    amount = 5000,
                    currency = "USD",
                ),
                onBehalfOf = oboMerchantID,
            )
        )

        testContext.markTestSucceeded()
    }

    @Test
    fun testSuccessfulCashAppPayment() = runEmbeddedPaymentElementTest(
        networkRule = networkRule,
        createIntentCallback = { _, _ ->
            CreateIntentResult.Success("pi_example_secret_12345")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.configure {
            formSheetAction(EmbeddedPaymentElement.FormSheetAction.Confirm)
        }

        embeddedContentPage.clickOnLpm("cashapp")

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }
        networkRule.enqueue(
            method("GET"),
            path("/v1/payment_intents/pi_example"),
        ) { response ->
            response.testBodyFromFile("payment-intent-get-requires_payment_method.json")
        }
        networkRule.enqueue(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        testContext.consumePaymentOptionEvent("cashapp", "Cash App Pay")
        testContext.confirm()
        assertThat(testContext.paymentOptionTurbine.awaitItem()).isNull()
    }

    @Test
    fun testSuccessfulUsBankAccountPayment() = runEmbeddedPaymentElementTest(
        networkRule = networkRule,
        createIntentCallback = { _, _ ->
            CreateIntentResult.Success("pi_example_secret_example")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        UsBankAccountFormTestUtils.setupSuccessfulCompletionOfUsBankAccountForm()

        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.configure {
            allowsDelayedPaymentMethods(true)
            formSheetAction(EmbeddedPaymentElement.FormSheetAction.Confirm)
        }

        embeddedContentPage.clickOnLpm("us_bank_account")

        formPage.waitUntilVisible()
        testRules.compose.onNode(hasText("Full name"))
            .performTextReplacement("Jane Doe")
        testRules.compose.onNode(hasText("Email"))
            .performTextReplacement("janedoe@example.com")

        formPage.clickPrimaryButtonWithoutWaitingForDismissal()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
        ) { response ->
            response.testBodyFromFile("payment-methods-create-us_bank_account.json")
        }
        networkRule.enqueue(
            method("GET"),
            path("/v1/payment_intents/pi_example"),
        ) { response ->
            response.testBodyFromFile("payment-intent-get-requires_payment_method-us_bank_account.json")
        }
        networkRule.enqueue(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm-us_bank_account.json")
        }

        formPage.clickPrimaryButton()
        formPage.waitUntilMissing()
    }

    @Test
    fun testSuccessfulCardPaymentWithLinkSignUp() = runEmbeddedPaymentElementTest(
        networkRule = networkRule,
        createIntentCallback = { _, _ ->
            CreateIntentResult.Success("pi_example_secret_12345")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.configure {
            formSheetAction(EmbeddedPaymentElement.FormSheetAction.Confirm)
        }

        embeddedContentPage.clickOnLpm("card")
        formPage.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/lookup"),
        ) { response ->
            response.testBodyFromFile("consumer-session-lookup-success.json")
        }

        // Click the "Save my info" checkbox and fill out Link signup fields
        testRules.compose.onNode(hasText("Save my info for faster checkout with Link"))
            .performClick()

        testRules.compose.onNode(hasText("Email"))
            .performTextReplacement("email@email.com")

        testRules.compose.waitUntil(timeoutMillis = 15_000) {
            testRules.compose
                .onAllNodes(hasText("Phone number"))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }

        testRules.compose.onNode(hasText("Phone number"))
            .performTextReplacement("+12113526421")

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/accounts/sign_up"),
        ) { response ->
            response.testBodyFromFile("consumer-accounts-signup-success.json")
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/payment_details"),
            bodyPart("card[number]", "4242424242424242"),
        ) { response ->
            response.testBodyFromFile("consumer-payment-details-success.json")
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        networkRule.enqueue(
            method("GET"),
            path("/v1/payment_intents/pi_example"),
        ) { response ->
            response.testBodyFromFile("payment-intent-get-requires_payment_method.json")
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        formPage.clickPrimaryButton()
        formPage.waitUntilMissing()
    }

    @Test
    fun testCardMetadataQueryExecutedOncePerCardSessionForBin() {
        repeat(2) {
            runEmbeddedPaymentElementTest(
                networkRule = networkRule,
                createIntentCallback = { _, shouldSavePaymentMethod ->
                    assertThat(shouldSavePaymentMethod).isFalse()
                    CreateIntentResult.Success("pi_example_secret_12345")
                },
                resultCallback = ::assertCompleted
            ) { testContext ->
                networkRule.elementsSession { response ->
                    response.testBodyFromFile("elements-sessions-requires_payment_method_with_cbc.json")
                }

                testContext.configure{
                    formSheetAction(EmbeddedPaymentElement.FormSheetAction.Confirm)
                }

                networkRule.enqueue(
                    method("GET"),
                    path("edge-internal/card-metadata")
                ) { response ->
                    response.testBodyFromFile("card-metadata-get.json")
                }

                embeddedContentPage.clickOnLpm("card")
                formPage.fillOutCardDetails()

                enqueueDeferredIntentConfirmationRequests()

                formPage.clickPrimaryButton()
                formPage.waitUntilMissing()
            }
        }
    }

    private fun enqueueDeferredIntentConfirmationRequests() {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }
        networkRule.enqueue(
            method("GET"),
            path("/v1/payment_intents/pi_example"),
        ) { response ->
            response.testBodyFromFile("payment-intent-get-requires_payment_method.json")
        }
        networkRule.enqueue(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }
    }
}

private fun EmbeddedPaymentElement.State?.paymentMethods(): List<PaymentMethod>? {
    return this?.customer?.paymentMethods
}
