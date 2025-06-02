@file:OptIn(ExperimentalEmbeddedPaymentElementApi::class)

package com.stripe.android.paymentelement

import com.google.common.truth.Truth.assertThat
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.paymentelementnetwork.CardPaymentMethodDetails
import com.stripe.paymentelementnetwork.setupPaymentMethodDetachResponse
import com.stripe.paymentelementnetwork.setupV1PaymentMethodsResponse
import com.stripe.paymentelementtestpages.EditPage
import com.stripe.paymentelementtestpages.ManagePage
import org.junit.Rule
import org.junit.Test

internal class EmbeddedPaymentElementImmediateActionRowSelectionTest {
    private val networkRule = NetworkRule()

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule)

    private val embeddedContentPage = EmbeddedContentPage(testRules.compose)
    private val managePage = ManagePage(testRules.compose)
    private val editPage = EditPage(testRules.compose)
    private val formPage = EmbeddedFormPage(testRules.compose)

    private val card1 = CardPaymentMethodDetails("pm_12345", "4242")
    private val card2 = CardPaymentMethodDetails("pm_67890", "5544")

    /**
     * Form Rows
     * - Continue
     *  - Select, fill out details, primary button
     *      - assert callback selected with correct label
     *  - Reselect, fill out details, primary button,
     *      - assert callback selected with correct label
     * - Confirm
     *  - Select, fill out details, primary button
     *      - assert no callback
     *
     * non form row, cashapp
     * - continue
     *  - assert callback selected with correct label
     * - confirm
     *  - assert callback selected with correct label
     *
     * saved, row
     * - Continue
     *  - assert callback selected with correct label
     *
     * saved, manage
     * - Continue
     *  - assert callback selected with correct label
     *
     * remove
     * - assert no callback
     */

    @Test
    fun testSuccessfulCardPayment_withFormSheetActionContinue() {
        val rowSelectionLabels: MutableList<String?> = mutableListOf()
        runEmbeddedPaymentElementTest(
            networkRule = networkRule,
            createIntentCallback = { _, shouldSavePaymentMethod ->
                assertThat(shouldSavePaymentMethod).isFalse()
                CreateIntentResult.Success("pi_example_secret_12345")
            },
            rowSelectionBehavior = EmbeddedPaymentElement.RowSelectionBehavior.ImmediateAction { embeddedPaymentElement ->
                val paymentOption = embeddedPaymentElement.paymentOption.value
                rowSelectionLabels.add(paymentOption?.label.toString())
            },
            resultCallback = ::assertCompleted,
        ) { testContext ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("GET"),
                path("/v1/elements/sessions"),
            ) { response ->
                response.testBodyFromFile("elements-sessions-requires_payment_method.json")
            }

            testContext.configure {
                formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue)
            }

            embeddedContentPage.clickOnLpm("card")
            formPage.fillOutCardDetails()
            formPage.clickPrimaryButton()
            formPage.waitUntilMissing()

            val expectedVisaLabel = getCardLabel("4242")
            assertThat(rowSelectionLabels).isEqualTo(
                listOf(expectedVisaLabel)
            )

            enqueueDeferredIntentConfirmationRequests()

            testContext.confirm()
        }
    }

    @Test
    fun testSuccessfulCardPayment_withFormEdit_withFormSheetActionContinue() {
        val rowSelectionLabels: MutableList<String?> = mutableListOf()
        runEmbeddedPaymentElementTest(
            networkRule = networkRule,
            createIntentCallback = { _, shouldSavePaymentMethod ->
                assertThat(shouldSavePaymentMethod).isFalse()
                CreateIntentResult.Success("pi_example_secret_12345")
            },
            rowSelectionBehavior = EmbeddedPaymentElement.RowSelectionBehavior.ImmediateAction { embeddedPaymentElement ->
                val paymentOption = embeddedPaymentElement.paymentOption.value
                rowSelectionLabels.add(paymentOption?.label.toString())
            },
            resultCallback = ::assertCompleted,
        ) { testContext ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("GET"),
                path("/v1/elements/sessions"),
            ) { response ->
                response.testBodyFromFile("elements-sessions-requires_payment_method.json")
            }

            testContext.configure {
                formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue)
            }

            embeddedContentPage.clickOnLpm("card")
            formPage.fillOutCardDetails("5555555555554444")
            formPage.clickPrimaryButton()
            formPage.waitUntilMissing()

            val expectedMastercardLabel = getCardLabel("4444")
            assertThat(rowSelectionLabels).isEqualTo(
                listOf(expectedMastercardLabel)
            )

            embeddedContentPage.assertHasSelectedLpm("card")
            embeddedContentPage.clickOnLpm("card")
            formPage.fillOutCardDetails("4242424242424242")
            formPage.clickPrimaryButton() // Ensures the form has the previous values.
            formPage.waitUntilMissing()

            val expectedVisaLabel = getCardLabel("4242")
            assertThat(rowSelectionLabels).isEqualTo(
                listOf(expectedMastercardLabel, expectedVisaLabel)
            )

            enqueueDeferredIntentConfirmationRequests()
            testContext.confirm()
        }
    }

    @Test
    fun testSuccessfulCardPayment_withReselection_withFormSheetActionContinue() {
        val rowSelectionLabels: MutableList<String?> = mutableListOf()
        runEmbeddedPaymentElementTest(
            networkRule = networkRule,
            createIntentCallback = { _, shouldSavePaymentMethod ->
                assertThat(shouldSavePaymentMethod).isFalse()
                CreateIntentResult.Success("pi_example_secret_12345")
            },
            rowSelectionBehavior = EmbeddedPaymentElement.RowSelectionBehavior.ImmediateAction { embeddedPaymentElement ->
                val paymentOption = embeddedPaymentElement.paymentOption.value
                rowSelectionLabels.add(paymentOption?.label.toString())
            },
            resultCallback = ::assertCompleted,
        ) { testContext ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("GET"),
                path("/v1/elements/sessions"),
            ) { response ->
                response.testBodyFromFile("elements-sessions-requires_payment_method.json")
            }

            testContext.configure {
                formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue)
            }

            embeddedContentPage.clickOnLpm("card")
            formPage.fillOutCardDetails()
            formPage.clickPrimaryButton()
            formPage.waitUntilMissing()
            val expectedVisaLabel = getCardLabel("4242")

            embeddedContentPage.assertHasSelectedLpm("card")
            embeddedContentPage.clickOnLpm("card")
            formPage.clickPrimaryButton() // Ensures the form has the previous values.
            formPage.waitUntilMissing()

            assertThat(rowSelectionLabels).isEqualTo(
                listOf(expectedVisaLabel, expectedVisaLabel)
            )

            enqueueDeferredIntentConfirmationRequests()
            testContext.confirm()
        }
    }

    @Test
    fun testSuccessfulCardPayment_withFormSheetActionConfirm() {
        val rowSelectionLabels: MutableList<String?> = mutableListOf()
        runEmbeddedPaymentElementTest(
            networkRule = networkRule,
            createIntentCallback = { _, shouldSavePaymentMethod ->
                assertThat(shouldSavePaymentMethod).isFalse()
                CreateIntentResult.Success("pi_example_secret_12345")
            },
            rowSelectionBehavior = EmbeddedPaymentElement.RowSelectionBehavior.ImmediateAction { embeddedPaymentElement ->
                val paymentOption = embeddedPaymentElement.paymentOption.value
                rowSelectionLabels.add(paymentOption?.label.toString())
            },
            resultCallback = ::assertCompleted,
        ) { testContext ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("GET"),
                path("/v1/elements/sessions"),
            ) { response ->
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

            // row selection should not be invoked
            assertThat(rowSelectionLabels).isEqualTo(
                emptyList<String>()
            )
        }
    }

    @Test
    fun testSuccessfulNonFormRowPayment_withFormSheetActionContinue() {
        val rowSelectionLabels: MutableList<String?> = mutableListOf()
        runEmbeddedPaymentElementTest(
            networkRule = networkRule,
            createIntentCallback = { _, shouldSavePaymentMethod ->
                assertThat(shouldSavePaymentMethod).isFalse()
                CreateIntentResult.Success("pi_example_secret_12345")
            },
            rowSelectionBehavior = EmbeddedPaymentElement.RowSelectionBehavior.ImmediateAction { embeddedPaymentElement ->
                val paymentOption = embeddedPaymentElement.paymentOption.value
                rowSelectionLabels.add(paymentOption?.label.toString())
            },
            resultCallback = ::assertCompleted,
        ) { testContext ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("GET"),
                path("/v1/elements/sessions"),
            ) { response ->
                response.testBodyFromFile("elements-sessions-requires_payment_method.json")
            }

            testContext.configure {
                formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue)
            }

            embeddedContentPage.clickOnLpm("cashapp")

            assertThat(rowSelectionLabels).isEqualTo(
                listOf("Cash App Pay")
            )

            enqueueDeferredIntentConfirmationRequests()

            testContext.confirm()
        }
    }

    @Test
    fun testSuccessfulNonFormRowPayment_withReselection_withFormSheetActionContinue() {
        val rowSelectionLabels: MutableList<String?> = mutableListOf()
        runEmbeddedPaymentElementTest(
            networkRule = networkRule,
            createIntentCallback = { _, shouldSavePaymentMethod ->
                assertThat(shouldSavePaymentMethod).isFalse()
                CreateIntentResult.Success("pi_example_secret_12345")
            },
            rowSelectionBehavior = EmbeddedPaymentElement.RowSelectionBehavior.ImmediateAction { embeddedPaymentElement ->
                val paymentOption = embeddedPaymentElement.paymentOption.value
                rowSelectionLabels.add(paymentOption?.label.toString())
            },
            resultCallback = ::assertCompleted,
        ) { testContext ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("GET"),
                path("/v1/elements/sessions"),
            ) { response ->
                response.testBodyFromFile("elements-sessions-requires_payment_method.json")
            }

            testContext.configure {
                formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue)
            }

            embeddedContentPage.clickOnLpm("cashapp")
            embeddedContentPage.clickOnLpm("cashapp")

            assertThat(rowSelectionLabels).isEqualTo(
                listOf("Cash App Pay", "Cash App Pay")
            )

            enqueueDeferredIntentConfirmationRequests()

            testContext.confirm()
        }
    }

    @Test
    fun testSuccessfulNonFormRowPayment_withFormSheetActionConfirm() {
        val rowSelectionLabels: MutableList<String?> = mutableListOf()
        runEmbeddedPaymentElementTest(
            networkRule = networkRule,
            createIntentCallback = { _, shouldSavePaymentMethod ->
                assertThat(shouldSavePaymentMethod).isFalse()
                CreateIntentResult.Success("pi_example_secret_12345")
            },
            rowSelectionBehavior = EmbeddedPaymentElement.RowSelectionBehavior.ImmediateAction { embeddedPaymentElement ->
                val paymentOption = embeddedPaymentElement.paymentOption.value
                rowSelectionLabels.add(paymentOption?.label.toString())
            },
            resultCallback = ::assertCompleted,
        ) { testContext ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("GET"),
                path("/v1/elements/sessions"),
            ) { response ->
                response.testBodyFromFile("elements-sessions-requires_payment_method.json")
            }

            testContext.configure {
                formSheetAction(EmbeddedPaymentElement.FormSheetAction.Confirm)
            }

            embeddedContentPage.clickOnLpm("cashapp")

            assertThat(rowSelectionLabels).isEqualTo(
                listOf("Cash App Pay")
            )

            enqueueDeferredIntentConfirmationRequests()
            testContext.confirm()
        }
    }

    @Test
    fun testSavedCardSelection_withFormSheetActionContinue() {
        val rowSelectionLabels: MutableList<String?> = mutableListOf()

        runEmbeddedPaymentElementTest(
            networkRule = networkRule,
            createIntentCallback = { _, shouldSavePaymentMethod ->
                assertThat(shouldSavePaymentMethod).isFalse()
                CreateIntentResult.Success("pi_example_secret_12345")
            },
            resultCallback = ::assertCompleted,
            rowSelectionBehavior = EmbeddedPaymentElement.RowSelectionBehavior.ImmediateAction { embeddedPaymentElement ->
                val paymentOption = embeddedPaymentElement.paymentOption.value
                rowSelectionLabels.add(paymentOption?.label.toString())
            },
        ) { testContext ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("GET"),
                path("/v1/elements/sessions"),
            ) { response ->
                response.testBodyFromFile("elements-sessions-deferred_payment_intent_no_link.json")
            }
            networkRule.setupV1PaymentMethodsResponse(card1, card2)

            testContext.configure {
                customer(PaymentSheet.CustomerConfiguration("cus_123", "ek_test"))
                formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue)
            }

            embeddedContentPage.clickViewMore()

            managePage.waitUntilVisible()
            managePage.selectPaymentMethod(card1.id)
            managePage.waitUntilNotVisible()

            assertThat(rowSelectionLabels).isEqualTo(
                listOf(getCardLabel("4242"))
            )

            testContext.markTestSucceeded()
        }
    }

    @Test
    fun testSavedCardChangeSelection_withFormSheetActionContinue() {
        val rowSelectionLabels: MutableList<String?> = mutableListOf()

        runEmbeddedPaymentElementTest(
            networkRule = networkRule,
            createIntentCallback = { _, shouldSavePaymentMethod ->
                assertThat(shouldSavePaymentMethod).isFalse()
                CreateIntentResult.Success("pi_example_secret_12345")
            },
            resultCallback = ::assertCompleted,
            rowSelectionBehavior = EmbeddedPaymentElement.RowSelectionBehavior.ImmediateAction { embeddedPaymentElement ->
                val paymentOption = embeddedPaymentElement.paymentOption.value
                rowSelectionLabels.add(paymentOption?.label.toString())
            },
        ) { testContext ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("GET"),
                path("/v1/elements/sessions"),
            ) { response ->
                response.testBodyFromFile("elements-sessions-deferred_payment_intent_no_link.json")
            }
            networkRule.setupV1PaymentMethodsResponse(card1, card2)

            testContext.configure {
                customer(PaymentSheet.CustomerConfiguration("cus_123", "ek_test"))
                formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue)
            }

            embeddedContentPage.clickViewMore()

            managePage.waitUntilVisible()
            managePage.selectPaymentMethod(card1.id)
            managePage.waitUntilNotVisible()

            embeddedContentPage.clickViewMore()
            managePage.waitUntilVisible()
            managePage.selectPaymentMethod(card2.id)
            managePage.waitUntilNotVisible()

            assertThat(rowSelectionLabels).isEqualTo(
                listOf(getCardLabel("4242"), getCardLabel("5544"))
            )

            testContext.markTestSucceeded()
        }
    }

    @Test
    fun testSavedCardReselection_withFormSheetActionContinue() {
        val rowSelectionLabels: MutableList<String?> = mutableListOf()

        runEmbeddedPaymentElementTest(
            networkRule = networkRule,
            createIntentCallback = { _, shouldSavePaymentMethod ->
                assertThat(shouldSavePaymentMethod).isFalse()
                CreateIntentResult.Success("pi_example_secret_12345")
            },
            resultCallback = ::assertCompleted,
            rowSelectionBehavior = EmbeddedPaymentElement.RowSelectionBehavior.ImmediateAction { embeddedPaymentElement ->
                val paymentOption = embeddedPaymentElement.paymentOption.value
                rowSelectionLabels.add(paymentOption?.label.toString())
            },
        ) { testContext ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("GET"),
                path("/v1/elements/sessions"),
            ) { response ->
                response.testBodyFromFile("elements-sessions-deferred_payment_intent_no_link.json")
            }
            networkRule.setupV1PaymentMethodsResponse(card1, card2)

            testContext.configure {
                customer(PaymentSheet.CustomerConfiguration("cus_123", "ek_test"))
                formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue)
            }

            embeddedContentPage.clickViewMore()

            managePage.waitUntilVisible()
            managePage.selectPaymentMethod(card1.id)
            managePage.waitUntilNotVisible()

            embeddedContentPage.clickViewMore()
            managePage.waitUntilVisible()
            managePage.selectPaymentMethod(card1.id)
            managePage.waitUntilNotVisible()

            assertThat(rowSelectionLabels).isEqualTo(
                listOf(getCardLabel("4242"), getCardLabel("4242"))
            )

            testContext.markTestSucceeded()
        }
    }

    @Test
    fun testSavedCardRowSelection_withFormSheetActionContinue() {
        val rowSelectionLabels: MutableList<String?> = mutableListOf()

        runEmbeddedPaymentElementTest(
            networkRule = networkRule,
            createIntentCallback = { _, shouldSavePaymentMethod ->
                assertThat(shouldSavePaymentMethod).isFalse()
                CreateIntentResult.Success("pi_example_secret_12345")
            },
            resultCallback = ::assertCompleted,
            rowSelectionBehavior = EmbeddedPaymentElement.RowSelectionBehavior.ImmediateAction { embeddedPaymentElement ->
                val paymentOption = embeddedPaymentElement.paymentOption.value
                rowSelectionLabels.add(paymentOption?.label.toString())
            },
        ) { testContext ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("GET"),
                path("/v1/elements/sessions"),
            ) { response ->
                response.testBodyFromFile("elements-sessions-deferred_payment_intent_no_link.json")
            }
            networkRule.setupV1PaymentMethodsResponse(card1, card2)

            testContext.configure {
                customer(PaymentSheet.CustomerConfiguration("cus_123", "ek_test"))
                formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue)
            }

            embeddedContentPage.clickOnSavedPM(card1.id)
            assertThat(rowSelectionLabels).isEqualTo(
                listOf(getCardLabel("4242"))
            )

            testContext.markTestSucceeded()
        }
    }

    @Test
    fun testSavedCardRowReselection_withFormSheetActionContinue() {
        val rowSelectionLabels: MutableList<String?> = mutableListOf()

        runEmbeddedPaymentElementTest(
            networkRule = networkRule,
            createIntentCallback = { _, shouldSavePaymentMethod ->
                assertThat(shouldSavePaymentMethod).isFalse()
                CreateIntentResult.Success("pi_example_secret_12345")
            },
            resultCallback = ::assertCompleted,
            rowSelectionBehavior = EmbeddedPaymentElement.RowSelectionBehavior.ImmediateAction { embeddedPaymentElement ->
                val paymentOption = embeddedPaymentElement.paymentOption.value
                rowSelectionLabels.add(paymentOption?.label.toString())
            },
        ) { testContext ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("GET"),
                path("/v1/elements/sessions"),
            ) { response ->
                response.testBodyFromFile("elements-sessions-deferred_payment_intent_no_link.json")
            }
            networkRule.setupV1PaymentMethodsResponse(card1, card2)

            testContext.configure {
                customer(PaymentSheet.CustomerConfiguration("cus_123", "ek_test"))
                formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue)
            }

            embeddedContentPage.clickOnSavedPM(card1.id)
            embeddedContentPage.clickOnSavedPM(card1.id)
            assertThat(rowSelectionLabels).isEqualTo(
                listOf(getCardLabel("4242"), getCardLabel("4242"))
            )

            testContext.markTestSucceeded()
        }
    }

    @Test
    fun testRemoveCard_doesNotInvokeCallback() {
        val rowSelectionLabels: MutableList<String?> = mutableListOf()

        runEmbeddedPaymentElementTest(
            networkRule = networkRule,
            createIntentCallback = { _, shouldSavePaymentMethod ->
                assertThat(shouldSavePaymentMethod).isFalse()
                CreateIntentResult.Success("pi_example_secret_12345")
            },
            resultCallback = ::assertCompleted,
            rowSelectionBehavior = EmbeddedPaymentElement.RowSelectionBehavior.ImmediateAction { embeddedPaymentElement ->
                val paymentOption = embeddedPaymentElement.paymentOption.value
                rowSelectionLabels.add(paymentOption?.label.toString())
            },
        ) { testContext ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("GET"),
                path("/v1/elements/sessions"),
            ) { response ->
                response.testBodyFromFile("elements-sessions-deferred_payment_intent_no_link.json")
            }
            networkRule.setupV1PaymentMethodsResponse(card1, card2)

            testContext.configure {
                customer(PaymentSheet.CustomerConfiguration("cus_123", "ek_test"))
            }

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

            assertThat(rowSelectionLabels).isEqualTo(
                emptyList<String>()
            )

            testContext.markTestSucceeded()
        }
    }

    private fun getCardLabel(last4: String): String {
        return "···· $last4"
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
