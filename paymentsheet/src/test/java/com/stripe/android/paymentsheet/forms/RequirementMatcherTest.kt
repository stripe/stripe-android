package com.stripe.android.paymentsheet.forms

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import kotlinx.serialization.Serializable
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class RequirementMatcherTest {
    @Test
    fun `Test supported payment method baseline`() {
        SupportedPaymentMethod.values()
            .forEach { lpm ->
                val resource = File(javaClass.classLoader.getResource("${lpm.type.code}-support.csv").file)
                val baseline = resource.readText()

                val csvOutput = StringBuilder()
                csvOutput.append(
                    "lpm, ${PaymentIntentTestInput.toCsvHeader()}, ${TestOutput.toCsvHeader()}\n"
                )
                generatePaymentIntentScenarios()
                    .map { testInput ->

                        val formDescriptor = getSpecWithFullfilledRequirements(lpm, testInput.getIntent(lpm), testInput.getConfig())
                        val testOutput = TestOutput.create(
                            supportCustomerSavedCard = getSupportedSavedCustomerPMs(
                                testInput.getIntent(lpm),
                                testInput.getConfig()
                            ).contains(lpm),
                            formExists = formDescriptor != null,
                            formShowsSaveCheckbox = formDescriptor?.showCheckbox,
                            formShowsCheckboxControlledFields = formDescriptor?.showCheckboxControlledFields,
                            supportsAdding = //lpm == SupportedPaymentMethod.Card
                            // TODO: When add in more PMS
                            getPMsToAdd(
                                testInput.getIntent(lpm), testInput.getConfig()
                            ).contains(lpm)
                        )
                        csvOutput.append(
                            "${lpm.type.code}, ${
                                testInput.copy(
                                    intentPMs = testInput.intentPMs.plus(lpm.type.code)
                                ).toCsv()
                            }, ${testOutput.toCsv()}\n"
                        )

                        PaymentIntentTestCase(
                            testInput,
                            testOutput
                        )
                    }

                if (baseline != csvOutput.toString()) {
                    println(csvOutput.toString())
                }
                assertThat(baseline).isEqualTo(csvOutput.toString())
            }
    }

    /**
     * This will generate payment intent scenarios for all combinations of customers, lpm types in the intent, shipping, and SFU states
     */
    private fun generatePaymentIntentScenarios(): List<PaymentIntentTestInput> {
        val scenarios = mutableListOf<PaymentIntentTestInput>()
        val customerStates = setOf(true, false)
        val setupFutureUsage = setOf(StripeIntent.Usage.OffSession, StripeIntent.Usage.OnSession, StripeIntent.Usage.OneTime)
        val allowsDelayedPayment = setOf(true, false)

        customerStates.forEach { customer ->
            setupFutureUsage.forEach { usage ->
                allowsDelayedPayment.forEach { delayed ->
                    scenarios.addAll(
                        listOf(
                            PaymentIntentTestInput(
                                hasCustomer = customer,
                                intentPMs = setOf(SupportedPaymentMethod.Card.type.code),
                                intentSetupFutureUsage = usage,
                                intentHasShipping = false,
                                allowsDelayedPayment = delayed
                            ),
                            PaymentIntentTestInput(
                                hasCustomer = customer,
                                intentPMs = setOf(SupportedPaymentMethod.Card.type.code, SupportedPaymentMethod.Eps.type.code),
                                intentSetupFutureUsage = usage,
                                intentHasShipping = false,
                                allowsDelayedPayment = delayed
                            )
                        )
                    )
                }
            }
        }
        return scenarios
    }

    @Serializable
    internal data class SetupIntentTestCase(
        val testInput: SetupIntentTestInput,
        val testOutput: TestOutput
    )

    @Serializable
    internal data class SetupIntentTestInput(
        val lpmTypeFormCode: String,
        val intentLpms: List<String>,
    )

    @Serializable
    internal data class PaymentIntentTestCase(
        val testInput: PaymentIntentTestInput,
        val testOutput: TestOutput
    )

    @Serializable
    internal data class PaymentIntentTestInput(
        val hasCustomer: Boolean,
        val intentSetupFutureUsage: StripeIntent.Usage?,
        val intentPMs: Set<String>,
        val intentHasShipping: Boolean,
        val allowsDelayedPayment: Boolean = true
    ) {
        companion object {
            fun toCsvHeader() = "hasCustomer, allowsDelayedPayment, intentSetupFutureUsage, intentHasShipping, intentLpms"
        }

        fun toCsv() = "$hasCustomer, $allowsDelayedPayment, $intentSetupFutureUsage, $intentHasShipping, ${intentPMs.joinToString("/")}"

        fun getIntent(lpm: SupportedPaymentMethod) = when (intentHasShipping) {
            false ->
                PaymentIntentFixtures.PI_OFF_SESSION.copy(
                    setupFutureUsage = intentSetupFutureUsage,
                    paymentMethodTypes = intentPMs.plus(lpm.type.code).toList()
                )
            true ->
                PaymentIntentFixtures.PI_WITH_SHIPPING.copy(
                    setupFutureUsage = intentSetupFutureUsage,
                    paymentMethodTypes = intentPMs.plus(lpm.type.code).toList()
                )
        }

        fun getConfig() = if (hasCustomer) {
            PaymentSheetFixtures.ARGS_CUSTOMER_WITHOUT_GOOGLEPAY.config?.copy(
                allowsDelayedPaymentMethods = allowsDelayedPayment
            )
        } else {
            PaymentSheetFixtures.ARGS_WITHOUT_CUSTOMER.config?.copy(
                allowsDelayedPaymentMethods = allowsDelayedPayment
            )
        }
    }

    @Serializable
    internal data class TestOutput(
        val supportCustomerSavedCard: Boolean,
        val formExists: Boolean,
        val supportsAdding: Boolean,
        val formType: String
    ) {
        companion object {
            fun create(
                supportCustomerSavedCard: Boolean,
                formExists: Boolean,
                formShowsSaveCheckbox: Boolean?, // null if no form
                formShowsCheckboxControlledFields: Boolean?, // null if let form decide
                supportsAdding: Boolean,
            ) = TestOutput(
                supportCustomerSavedCard = supportCustomerSavedCard,
                formExists = formExists,
                supportsAdding = supportsAdding,
                formType = when {
                    formShowsSaveCheckbox == false && formShowsCheckboxControlledFields == false -> "null"
                    formShowsSaveCheckbox == false && formShowsCheckboxControlledFields == true -> "merchantRequiredSave"
                    formShowsSaveCheckbox == true && formShowsCheckboxControlledFields == true -> "userSelectedSave"
                    else -> "not available"
                }
            )

            fun toCsvHeader() =
                "supportCustomerSavedCard, formExists, formType, supportsAdding"
        }

        fun toCsv() =
            "$supportCustomerSavedCard, $formExists, $formType, $supportsAdding"
    }
}
