package com.stripe.android.paymentsheet.forms

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class RequirementMatcherTest {
    private val formatPretty = Json { prettyPrint = true }
    private val format = Json { prettyPrint = false }

    @Test
    fun `Test supported payment method baseline`() {
        println(
            "lpm, ${PaymentIntentTestInput.toCsvHeader()}, ${TestOutput.toCsvHeader()}"
        )
        SupportedPaymentMethod.values()
            .forEach { lpm ->
                val resource = File(javaClass.classLoader.getResource("${lpm.type.code}-support.json").file)
                val baseline = resource.readText().replace(" ", "").replace("\n", "")

                val newScenariosList = generatePaymentIntentScenarios()
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
                            supportsAdding =// lpm == SupportedPaymentMethod.Card
                            //TODO: When add in more PMS
                            getPMsToAdd(
                                testInput.getIntent(lpm), testInput.getConfig()
                            ).contains(lpm)
                        )
                        println("${lpm.type.code}, ${testInput.toCsv()}, ${testOutput.toCsv()}")

                        PaymentIntentTestCase(
                            testInput,
                            testOutput
                        )
                    }

                val newScenarios = format.encodeToString(newScenariosList).replace(" ", "").replace("\n", "")

                if (baseline != newScenarios) {
                    println(formatPretty.encodeToString(newScenariosList))
                }
                assertThat(baseline).isEqualTo(newScenarios)
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
                                intentLpms = setOf(SupportedPaymentMethod.Card.type.code),
                                intentSetupFutureUsage = usage,
                                intentHasShipping = false,
                                allowsDelayedPayment = delayed
                            ),
                            PaymentIntentTestInput(
                                hasCustomer = customer,
                                intentLpms = setOf(SupportedPaymentMethod.Card.type.code, SupportedPaymentMethod.Eps.type.code),
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
        val intentLpms: Set<String>,
        val intentHasShipping: Boolean,
        val allowsDelayedPayment: Boolean = true
    ) {
        companion object {
            fun toCsvHeader() = "hasCustomer, allowsDelayedPayment, intentSetupFutureUsage, intentHasShipping, intentLpms"
        }

        fun toCsv() = "$hasCustomer, $allowsDelayedPayment, $intentSetupFutureUsage, $intentHasShipping, ${intentLpms.joinToString("/")}"

        fun getIntent(lpm: SupportedPaymentMethod) = when (intentHasShipping) {
            false ->
                PaymentIntentFixtures.PI_OFF_SESSION.copy(
                    setupFutureUsage = intentSetupFutureUsage,
                    paymentMethodTypes = intentLpms.plus(lpm.type.code).toList()
                )
            true ->
                PaymentIntentFixtures.PI_WITH_SHIPPING.copy(
                    setupFutureUsage = intentSetupFutureUsage,
                    paymentMethodTypes = intentLpms.plus(lpm.type.code).toList()
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
                    formShowsSaveCheckbox == false && formShowsCheckboxControlledFields == false -> "oneTimeUse"
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
