package com.stripe.android.paymentsheet

import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.nio.charset.Charset

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class FormSaveCheckboxTest {
    private val formatPretty = Json { prettyPrint = true }
    private val format = Json { prettyPrint = false }

    private val expectedMandateRequiredLPMs = listOf("bancontact", "sofort", "ideal", "sepa_debit", "eps", "future_unknown")
    private val paymentMethodSupportsUserRequestedSaveForFutureUsage =
        listOf("card", "bancontact", "sofort", "ideal", "sepa_debit", "afterpay_clearpay", "future_unknown")

    @Test
    fun `Verify correct list of pms requiring a mandate`() {
        SupportedPaymentMethod.values().filter {
            it.type.requiresMandate
        }.forEach {
            Truth.assertThat(expectedMandateRequiredLPMs).contains(it.type.code)
        }
        SupportedPaymentMethod.values().filter {
            !it.type.requiresMandate
        }.forEach {
            Truth.assertThat(expectedMandateRequiredLPMs).doesNotContain(it.type.code)
        }
    }

    @Test
    fun `Verify correct list of pms support user confirm time save for future usage`() {
        SupportedPaymentMethod.values().filter {
            it.userRequestedConfirmSaveForFutureSupported
        }.forEach {
            Truth.assertThat(paymentMethodSupportsUserRequestedSaveForFutureUsage).contains(it.type.code)
        }

        SupportedPaymentMethod.values().filter {
            !it.userRequestedConfirmSaveForFutureSupported
        }.forEach {
            Truth.assertThat(paymentMethodSupportsUserRequestedSaveForFutureUsage).doesNotContain(it.type.code)
        }
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
            .map {
                PaymentIntentTestCase(
                    it,
                    TestOutput(
                        formArgumentFromPaymentIntentTestInput(it).displayUIRequiredForSaving,
                        formArgumentFromPaymentIntentTestInput(it).allowUserInitiatedReuse
                    )
                )
            }
        val newScenarios = format.encodeToString(newScenariosList).replace(" ", "").replace("\n", "")

        if (baseline != newScenarios) {
            println(formatPretty.encodeToString(newScenariosList))
        }
        assertThat(baseline).isEqualTo(newScenarios)
    }

    /**
     * This "test" will generate test cases with test outputs hard coded
     * to false, these values will need to be manually set in the json output file.
     */
    @Test
    fun `Output baseline setup intent test inputs`() {
        println(
            format.encodeToString(
                generateSetupIntentScenarios().map {
                    SetupIntentTestCase(
                        it,
                        TestOutput(
                            formArgumentFromSetupIntentTestInput(it).displayUIRequiredForSaving,
                            formArgumentFromSetupIntentTestInput(it).allowUserInitiatedReuse
                        )
                    )
                }
            )
        )
    }

    private fun generateSetupIntentScenarios(): List<SetupIntentTestInput> {
        val scenarios = mutableListOf<SetupIntentTestInput>()
        val customerStates = listOf(true, false)
        val setupIntentLPM = listOf("card", "ideal", "sepa_debit", "bancontact", "sofort")

        customerStates.forEach { customer ->
            setupIntentLPM.forEach { lpmCode ->
                scenarios.add(
                    SetupIntentTestInput(
                        hasCustomer = customer,
                        lpmTypeFormCode = lpmCode,
                        intentLpms = setupIntentLPM
                    )
                )
            }
        }
        return scenarios
    }

    @Test
    fun `Verify form argument in setup intent`() {
        format.decodeFromString<List<SetupIntentTestCase>>(
            javaClass.classLoader!!.getResource("FormSaveCheckboxSetupIntent.json").readText(Charset.defaultCharset())
        ).forEach {
            Truth.assertThat(
                formArgumentFromSetupIntentTestInput(it.testInput)
            ).isEqualTo(
                FormFragmentArguments(
                    SupportedPaymentMethod.fromCode(it.testInput.lpmTypeFormCode)!!,
                    allowUserInitiatedReuse = it.testOutput.expectedAllowUserInitiatedReuse,
                    displayUIRequiredForSaving = it.testOutput.expectedDisplayUIForSaving,
                    merchantName = "Example, Inc",
                )
            )
        }
    }

    @Test
    fun `Verify form argument in payment intent`() {
        format.decodeFromString<List<PaymentIntentTestCase>>(
            javaClass.classLoader!!.getResource("FormSaveCheckboxPaymentIntent.json").readText(Charset.defaultCharset())
        ).forEach {
            println(it.testInput)
            Truth.assertThat(
                formArgumentFromPaymentIntentTestInput(it.testInput)
            ).isEqualTo(
                FormFragmentArguments(
                    SupportedPaymentMethod.fromCode(it.testInput.lpmTypeFormCode)!!,
                    allowUserInitiatedReuse = it.testOutput.expectedAllowUserInitiatedReuse,
                    displayUIRequiredForSaving = it.testOutput.expectedDisplayUIForSaving,
                    merchantName = "Example, Inc",
                )
            )
        }
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
                        ),
                        /**
                         * Save for future use not allowed if the PI contains an LPM that does not support Save for future use on confirm
                         */
                        PaymentIntentTestInput(
                            hasCustomer = customer,
                            lpmTypeFormCode = SupportedPaymentMethod.Card.type.code,
                            intentLpms = listOf(SupportedPaymentMethod.Card.type.code, SupportedPaymentMethod.Eps.type.code),
                            intentSetupFutureUsage = usage,
                        ),
                        /**
                         * Save for future use not allowed if the form requested requires a mandate
                         */
                        PaymentIntentTestInput(
                            hasCustomer = customer,
                            lpmTypeFormCode = SupportedPaymentMethod.SepaDebit.type.code,
                            intentLpms = listOf(SupportedPaymentMethod.Card.type.code, SupportedPaymentMethod.SepaDebit.type.code),
                            intentSetupFutureUsage = usage,
                        ),
                    )
                )
            }
        }
        return scenarios
    }

    private fun formArgumentFromPaymentIntentTestInput(testInput: PaymentIntentTestInput) = BaseAddPaymentMethodFragment.getFormArguments(
        hasCustomer = testInput.hasCustomer,
        stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = testInput.intentLpms,
            setupFutureUsage = testInput.intentSetupFutureUsage
        ),
        supportedPaymentMethod = SupportedPaymentMethod.fromCode(testInput.lpmTypeFormCode)!!,
        merchantName = "Example, Inc"
    )

    private fun formArgumentFromSetupIntentTestInput(testInput: SetupIntentTestInput) = BaseAddPaymentMethodFragment.getFormArguments(
        hasCustomer = testInput.hasCustomer,
        stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = testInput.intentLpms
        ),
        supportedPaymentMethod = SupportedPaymentMethod.fromCode(testInput.lpmTypeFormCode)!!,
        merchantName = "Example, Inc"
    )

    @Serializable
    internal data class SetupIntentTestCase(
        val testInput: SetupIntentTestInput,
        val testOutput: TestOutput
    )

    @Serializable
    internal data class SetupIntentTestInput(
        val hasCustomer: Boolean,
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
        val intentLpms: List<String>
    )

    @Serializable
    internal data class TestOutput(
        val expectedDisplayUIForSaving: Boolean,
        val expectedAllowUserInitiatedReuse: Boolean
    )
}
