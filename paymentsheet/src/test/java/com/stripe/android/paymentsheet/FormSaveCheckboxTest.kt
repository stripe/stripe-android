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
import java.nio.charset.Charset

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class FormSaveCheckboxTest {
    private val format = Json { prettyPrint = true }

    private val expectedMandateRequiredLPMs = listOf("bancontact", "sofort", "ideal", "sepa_debit", "eps", "future_unknown")
    private val paymentMethodSupportsUserRequestedSaveForFutureUsage =
        listOf("card", "bancontact", "sofort", "ideal", "sepa_debit", "afterpay_clearpay", "future_unknown")

    @Test
    fun `Verify correct list of pms requiring a mandate`() {
        SupportedPaymentMethod.values().filter {
            it.requiresMandate
        }.forEach {
            Truth.assertThat(expectedMandateRequiredLPMs).contains(it.code)
        }
        SupportedPaymentMethod.values().filter {
            !it.requiresMandate
        }.forEach {
            Truth.assertThat(expectedMandateRequiredLPMs).doesNotContain(it.code)
        }
    }

    @Test
    fun `Verify correct list of pms support user confirm time save for future usage`() {
        SupportedPaymentMethod.values().filter {
            it.userRequestedConfirmSaveForFutureSupported
        }.forEach {
            Truth.assertThat(paymentMethodSupportsUserRequestedSaveForFutureUsage).contains(it.code)
        }

        SupportedPaymentMethod.values().filter {
            !it.userRequestedConfirmSaveForFutureSupported
        }.forEach {
            Truth.assertThat(paymentMethodSupportsUserRequestedSaveForFutureUsage).doesNotContain(it.code)
        }
    }

    /**
     * This "test" will generate test cases with test outputs hard coded
     * to false, these values will need to be manually set in the json output file.
     */
    @Test
    fun `Output baseline payment intent test inputs`() {
        println(
            format.encodeToString(
                generatePaymentIntentScenarios()
                    .map {
                        PaymentIntentTestCase(
                            it,
                            TestOutput(
                                formArgumentFromPaymentIntentTestInput(it).saveForFutureUseInitialValue,
                                formArgumentFromPaymentIntentTestInput(it).saveForFutureUseInitialVisibility
                            )
                        )
                    }
            )
        )
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
                            formArgumentFromSetupIntentTestInput(it).saveForFutureUseInitialValue,
                            formArgumentFromSetupIntentTestInput(it).saveForFutureUseInitialVisibility
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
            assertThat(
                formArgumentFromSetupIntentTestInput(it.testInput)
            ).isEqualTo(
                FormFragmentArguments(
                    SupportedPaymentMethod.fromCode(it.testInput.lpmTypeFormCode)!!.name,
                    saveForFutureUseInitialVisibility = it.testOutput.expectedSaveCheckboxVisible,
                    saveForFutureUseInitialValue = it.testOutput.expectedSaveCheckboxValue,
                    merchantName = "Example, Inc",
                    injectorKey = -1
                )
            )
        }
    }

    @Test
    fun `Verify form argument in payment intent`() {
        format.decodeFromString<List<PaymentIntentTestCase>>(
            javaClass.classLoader!!.getResource("FormSaveCheckboxPaymentIntent.json").readText(Charset.defaultCharset())
        ).forEach {
            assertThat(
                formArgumentFromPaymentIntentTestInput(it.testInput)
            ).isEqualTo(
                FormFragmentArguments(
                    SupportedPaymentMethod.fromCode(it.testInput.lpmTypeFormCode)!!.name,
                    saveForFutureUseInitialVisibility = it.testOutput.expectedSaveCheckboxVisible,
                    saveForFutureUseInitialValue = it.testOutput.expectedSaveCheckboxValue,
                    merchantName = "Example, Inc",
                    injectorKey = -1
                )
            )
        }
    }

    private fun generatePaymentIntentScenarios(): List<PaymentIntentTestInput> {
        val scenarios = mutableListOf<PaymentIntentTestInput>()
        val customerStates = listOf(true, false)
        val setupFutureUsage = listOf(StripeIntent.Usage.OffSession, StripeIntent.Usage.OnSession)

        customerStates.forEach { customer ->
            setupFutureUsage.forEach { usage ->
                scenarios.addAll(
                    listOf(
                        /**
                         * Save for future use is allowed when card is shown and only card is in the PI
                         */
                        PaymentIntentTestInput(
                            hasCustomer = customer,
                            lpmTypeFormCode = SupportedPaymentMethod.Card.code,
                            intentLpms = listOf(SupportedPaymentMethod.Card.code),
                            intentSetupFutureUsage = usage,
                        ),
                        /**
                         * Save for future use not allowed if the PI contains an LPM that does not support Save for future use on confirm
                         */
                        PaymentIntentTestInput(
                            hasCustomer = customer,
                            lpmTypeFormCode = SupportedPaymentMethod.Card.code,
                            intentLpms = listOf(SupportedPaymentMethod.Card.code, SupportedPaymentMethod.Eps.code),
                            intentSetupFutureUsage = usage,
                        ),
                        /**
                         * Save for future use not allowed if the form requested requires a mandate
                         */
                        PaymentIntentTestInput(
                            hasCustomer = customer,
                            lpmTypeFormCode = SupportedPaymentMethod.SepaDebit.code,
                            intentLpms = listOf(SupportedPaymentMethod.Card.code, SupportedPaymentMethod.SepaDebit.code),
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
            paymentMethodTypes = testInput.intentLpms
        ),
        supportedPaymentMethodName = SupportedPaymentMethod.fromCode(testInput.lpmTypeFormCode)!!.name,
        merchantName = "Example, Inc",
        injectorKey = -1
    )

    private fun formArgumentFromSetupIntentTestInput(testInput: SetupIntentTestInput) = BaseAddPaymentMethodFragment.getFormArguments(
        hasCustomer = testInput.hasCustomer,
        stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = testInput.intentLpms
        ),
        supportedPaymentMethodName = SupportedPaymentMethod.fromCode(testInput.lpmTypeFormCode)!!.name,
        merchantName = "Example, Inc",
        injectorKey = -1
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
        val expectedSaveCheckboxValue: Boolean,
        val expectedSaveCheckboxVisible: Boolean
    )
}
