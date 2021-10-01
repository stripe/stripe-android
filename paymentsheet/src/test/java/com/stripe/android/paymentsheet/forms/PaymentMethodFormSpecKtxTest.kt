package com.stripe.android.paymentsheet.forms

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.elements.Requirement
import com.stripe.android.paymentsheet.elements.SaveForFutureUseSpec
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PaymentMethodFormSpecKtxTest {
    private val formatPretty = Json { prettyPrint = true }
    private val format = Json { prettyPrint = false }

    private val expectedMandateRequiredLPMs = listOf("bancontact", "sofort", "ideal", "sepa_debit", "eps", "future_unknown")
    private val paymentMethodSupportsUserRequestedSaveForFutureUsage =
        listOf(
            PaymentMethod.Type.Alipay.code,
            PaymentMethod.Type.Card.code,
            PaymentMethod.Type.SepaDebit.code,
            PaymentMethod.Type.AuBecsDebit.code,
            PaymentMethod.Type.Bancontact.code,
            PaymentMethod.Type.Sofort.code,
            PaymentMethod.Type.BacsDebit.code,
            PaymentMethod.Type.Ideal.code,
        )

    @Test
    fun `Intent that is off session should have merchant requires set to true`() {
        assertThat(
            getAllCapabilities(
                stripeIntent = PaymentIntentFixtures.PI_OFF_SESSION,
                config = null
            )
        ).isEqualTo(
            setOf(
                Requirement.MerchantRequiresSave
            )
        )
    }

    @Test
    fun `sepa debit`() {

        val testInput = PaymentIntentTestInput(
            hasCustomer = true,
            lpmTypeFormCode = "sepa_debit",
            intentSetupFutureUsage = StripeIntent.Usage.OneTime,
            intentLpms = listOf("card", "sepa_debit"),
            intentHasShipping = false
        )
        val requestedPaymentMethod = SupportedPaymentMethod.fromCode(testInput.lpmTypeFormCode)
//        val form = requestedPaymentMethod?.formSpec?.getForm(
//            getAllCapabilities(testInput.getIntent(), testInput.getConfig())
//        )


//        val testOutput = TestOutput(
//            supportCustomerSavedCard = getSupportedSavedCustomerCards(
//                testInput.getIntent(),
//                testInput.getConfig()
//            ).contains(requestedPaymentMethod),
//            formExists = form != null,
//            formHasSaveCheckbox = form?.items?.filterIsInstance<SaveForFutureUseSpec>()?.isNotEmpty() == true,
//            supportsAdding = getSupportedPaymentMethods(
//                testInput.getIntent(), testInput.getConfig()
//            ).contains(requestedPaymentMethod)
//        )

        val supported = getSupportedPaymentMethods(
            testInput.getIntent(), testInput.getConfig()
        )
        assertThat(supported).contains(requestedPaymentMethod)
    }

    @Test
    fun `test prefer user selectable over not selectable if there is a customer`() {

        val testInput = PaymentIntentTestInput(
            hasCustomer = true,
            lpmTypeFormCode = "card",
            intentSetupFutureUsage = StripeIntent.Usage.OneTime,
            intentLpms = listOf("card", "eps"),
            intentHasShipping = false
        )
        val requestedPaymentMethod = SupportedPaymentMethod.fromCode(testInput.lpmTypeFormCode)
        val form = requestedPaymentMethod?.formSpec?.getForm(
            getAllCapabilities(testInput.getIntent(), testInput.getConfig())
        )
        assertThat(
            form?.items?.filterIsInstance<SaveForFutureUseSpec>()
        ).isEmpty()

    }

    /**
     * This "test" will generate test cases with test outputs hard coded
     * to false, these values will need to be manually set in the json output file.
     */
    @Test
    fun `Output baseline payment intent test inputs`() {
        val resource = File(javaClass.classLoader.getResource("FormSaveCheckboxPaymentIntent.json").file)
        val baseline = resource.readText().replace(" ", "").replace("\n", "")
        val newScenariosList = generatePaymentIntentScenarios()
            .map { testInput ->
                val requestedPaymentMethod = SupportedPaymentMethod.fromCode(testInput.lpmTypeFormCode)
                val form = requestedPaymentMethod?.formSpec?.getForm(
                    getAllCapabilities(testInput.getIntent(), testInput.getConfig())
                )
                val testOutput = TestOutput(
                    supportCustomerSavedCard = getSupportedSavedCustomerCards(
                        testInput.getIntent(),
                        testInput.getConfig()
                    ).contains(requestedPaymentMethod),
                    formExists = form != null,
                    formHasSaveCheckbox = form?.items?.filterIsInstance<SaveForFutureUseSpec>()?.isNotEmpty() == true,
                    supportsAdding = getSupportedPaymentMethods(
                        testInput.getIntent(), testInput.getConfig()
                    ).contains(requestedPaymentMethod)
                )
                println("${testOutput.toCsv()}, ${testInput.toCsv()}")

                PaymentIntentTestCase(
                    testInput,
                    testOutput
                )

            }
        val newScenarios = format.encodeToString(newScenariosList).replace(" ", "").replace("\n", "")

        if (baseline != newScenarios) {
            println(formatPretty.encodeToString(newScenariosList))
        }
//        println(formatPretty.encodeToString(newScenariosList))
//        assertThat(baseline).isEqualTo(newScenarios)
    }

    private fun generatePaymentIntentScenarios(): List<PaymentIntentTestInput> {
        val scenarios = mutableListOf<PaymentIntentTestInput>()
        val customerStates = setOf(true, false)
        val setupFutureUsage = setOf(StripeIntent.Usage.OffSession, StripeIntent.Usage.OnSession, StripeIntent.Usage.OneTime)

        customerStates.forEach { customer ->
            setupFutureUsage.forEach { usage ->
                scenarios.addAll(
                    listOf(
                        /**
                         * Save for future use is allowed when card is shown and only card is in the PI
                         */
                        PaymentIntentTestInput(
                            hasCustomer = customer,
                            lpmTypeFormCode = SupportedPaymentMethod.Card.type.code,
                            intentLpms = listOf(SupportedPaymentMethod.Card.type.code),
                            intentSetupFutureUsage = usage,
                            intentHasShipping = false
                        ),
                        /**
                         * Save for future use not allowed if the PI contains an LPM that does not support Save for future use on confirm
                         */
                        PaymentIntentTestInput(
                            hasCustomer = customer,
                            lpmTypeFormCode = SupportedPaymentMethod.Card.type.code,
                            intentLpms = listOf(SupportedPaymentMethod.Card.type.code, SupportedPaymentMethod.Eps.type.code),
                            intentSetupFutureUsage = usage,
                            intentHasShipping = false,
                        ),
                        /**
                         * Save for future use not allowed if the form requested requires a mandate
                         */
                        PaymentIntentTestInput(
                            hasCustomer = customer,
                            lpmTypeFormCode = SupportedPaymentMethod.SepaDebit.type.code,
                            intentLpms = listOf(SupportedPaymentMethod.Card.type.code, SupportedPaymentMethod.SepaDebit.type.code),
                            intentSetupFutureUsage = usage,
                            intentHasShipping = false,
                        ),
                        /**
                         * Afterpay not allowed when no AfterPayCancelSupport offered by SDK
                         */
                        PaymentIntentTestInput(
                            hasCustomer = customer,
                            lpmTypeFormCode = SupportedPaymentMethod.AfterpayClearpay.type.code,
                            intentLpms = listOf(SupportedPaymentMethod.Card.type.code, SupportedPaymentMethod.AfterpayClearpay.type.code),
                            intentSetupFutureUsage = usage,
                            intentHasShipping = true,
                        ),
                    )
                )
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
        val lpmTypeFormCode: String,
        val intentSetupFutureUsage: StripeIntent.Usage?,
        val intentLpms: List<String>,
        val intentHasShipping: Boolean,
        val allowsDelayedPayment: Boolean = true
    ) {
        fun toCsv() = "$hasCustomer, $lpmTypeFormCode, $intentSetupFutureUsage, $intentHasShipping, $intentLpms"

        fun getIntent() = when (intentHasShipping) {
            false ->
                PaymentIntentFixtures.PI_OFF_SESSION.copy(
                    setupFutureUsage = intentSetupFutureUsage,
                    paymentMethodTypes = intentLpms
                )
            true ->
                PaymentIntentFixtures.PI_WITH_SHIPPING.copy(
                    setupFutureUsage = intentSetupFutureUsage,
                    paymentMethodTypes = intentLpms
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
        val formHasSaveCheckbox: Boolean,
        val supportsAdding: Boolean
    ) {
        fun toCsv() = "$supportCustomerSavedCard, $formExists, $formHasSaveCheckbox, $supportsAdding"
    }
}
