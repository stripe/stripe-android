package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodRegistry
import com.stripe.android.lpmfoundations.paymentmethod.TestUiDefinitionFactoryArgumentsFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentsheet.forms.FormArgumentsFactory
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.transformToPaymentSelection
import com.stripe.android.paymentsheet.utils.ViewModelStoreTestRule
import com.stripe.android.testing.CleanupTestRule
import com.stripe.android.uicore.elements.FormFieldId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector

@RunWith(RobolectricTestParameterInjector::class)
internal class LpmBillingAddressFormValuesToParamsTest {
    private val viewModelStoreRule = ViewModelStoreTestRule()

    private val coroutineScopeCleanupRule = CleanupTestRule<CoroutineScope> { cancel() }

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(viewModelStoreRule)
        .around(coroutineScopeCleanupRule)

    private val coroutineScope = coroutineScopeCleanupRule.track(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun `creates the expected form params`(
        @TestParameter(valuesProvider = LpmBillingAddressFormValuesToParamsTestCaseProvider::class)
        testCase: LpmBillingAddressFormValuesToParamsTestCase,
    ) = runTest {
        val actual = createFormParamsFromFormValues(
            config = testCase.config,
            rawValues = testCase.rawValues,
        )

        assertThat(actual).isEqualTo(testCase.expectedParams)
    }

    @Test
    fun `has unique configs`() {
        assertThat(lpmBillingAddressTestConfigurations).containsNoDuplicates()
    }

    @Test
    fun `specialized flow definitions remain registered`() {
        assertThat(PaymentMethodRegistry.all).containsAtLeastElementsIn(specializedFlowDefinitions)
    }

    @Test
    fun `covers every registered LPM without a specialized billing flow`() {
        val expected = PaymentMethodRegistry.all - specializedFlowDefinitions
        val covered = lpmBillingAddressTestConfigurations
            .map { it.paymentMethodType }
            .distinct()

        assertThat(covered).containsExactlyElementsIn(expected.map { it.type })
    }

    @Test
    fun `covers every preservation billing mode for every payment method`() {
        lpmBillingAddressTestConfigurations
            .groupBy { it.paymentMethodType }
            .values
            .forEach { configs ->
                assertThat(configs.map { it.billingDetailsCollectionMode })
                    .containsAtLeastElementsIn(
                        listOf(
                            LpmBillingDetailsCollectionMode.Never,
                            LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
                            LpmBillingDetailsCollectionMode.Full,
                        )
                    )
            }
    }

    private suspend fun createFormParamsFromFormValues(
        config: LpmBillingAddressTestConfiguration,
        rawValues: Map<FormFieldId, String?>,
    ): LpmBillingAddressFormParams {
        val metadata = config.metadata()
        val formViewModel = createFormViewModel(
            paymentMethodType = config.paymentMethodType,
            metadata = metadata,
            initialValues = rawValues,
        )

        return formViewModel.createFormParams(
            paymentMethodType = config.paymentMethodType,
            metadata = metadata,
        )
    }

    private fun createFormViewModel(
        paymentMethodType: PaymentMethod.Type,
        metadata: PaymentMethodMetadata,
        initialValues: Map<FormFieldId, String?>,
    ): FormViewModel {
        val formElements = requireNotNull(
            metadata.formElementsForCode(
                code = paymentMethodType.code,
                uiDefinitionFactoryArgumentsFactory = TestUiDefinitionFactoryArgumentsFactory.create(
                    coroutineScope = coroutineScope,
                    initialValues = initialValues,
                ),
            ),
        )

        return viewModelStoreRule.track(
            FormViewModel(
                formElements = formElements,
                formArguments = FormArgumentsFactory.create(
                    paymentMethodCode = paymentMethodType.code,
                    metadata = metadata,
                ),
            ),
        )
    }

    private suspend fun FormViewModel.createFormParams(
        paymentMethodType: PaymentMethod.Type,
        metadata: PaymentMethodMetadata,
    ): LpmBillingAddressFormParams {
        val supportedPaymentMethod = requireNotNull(
            metadata.supportedPaymentMethodForCode(paymentMethodType.code),
        )
        val formFieldValues = requireNotNull(completeFormValues.first()) {
            "The ${paymentMethodType.code} form never completed. A required field was not seeded, " +
                "which means its definition ignores UiDefinitionFactory.Arguments.initialValues."
        }

        val paymentSelection = formFieldValues.transformToPaymentSelection(supportedPaymentMethod, metadata)

        require(paymentSelection is PaymentSelection.New)

        return LpmBillingAddressFormParams(
            createParams = paymentSelection.paymentMethodCreateParams,
            optionsParams = paymentSelection.paymentMethodOptionsParams,
            extraParams = paymentSelection.paymentMethodExtraParams,
        )
    }
}

// These definitions render or collect billing details through specialized flows outside the shared LPM form harness.
private val specializedFlowDefinitions: Set<PaymentMethodDefinition> = setOf(
    CardDefinition,
    InstantDebitsDefinition,
    UsBankAccountDefinition,
)

internal data class LpmBillingAddressFormValuesToParamsTestCase(
    val name: String,
    val config: LpmBillingAddressTestConfiguration,
    val rawValues: Map<FormFieldId, String?>,
    val expectedParams: LpmBillingAddressFormParams,
) {
    override fun toString(): String = name
}

internal data class LpmBillingAddressFormParams(
    val createParams: PaymentMethodCreateParams,
    val optionsParams: PaymentMethodOptionsParams?,
    val extraParams: PaymentMethodExtraParams?,
)

internal val lpmBillingAddressFormValuesToParamsTestCases = buildList {
    addAll(affirmTestCases)
    addAll(afterpayClearpayTestCases)
    addAll(alipayTestCases)
    addAll(almaTestCases)
    addAll(amazonPayTestCases)
    addAll(auBecsDebitTestCases)
    addAll(bacsDebitTestCases)
    addAll(bancontactTestCases)
    addAll(billieTestCases)
    addAll(blikTestCases)
    addAll(boletoTestCases)
    addAll(cashAppPayTestCases)
    addAll(cryptoTestCases)
    addAll(epsTestCases)
    addAll(fpxTestCases)
    addAll(grabPayTestCases)
    addAll(idealFormParamsTestCases)
    addAll(klarnaTestCases)
    addAll(konbiniTestCases)
    addAll(krCardTestCases)
    addAll(mobilePayTestCases)
    addAll(multibancoTestCases)
    addAll(naverPayTestCases)
    addAll(oxxoTestCases)
    addAll(p24TestCases)
    addAll(payByBankTestCases)
    addAll(paycoTestCases)
    addAll(payNowTestCases)
    addAll(payPalTestCases)
    addAll(payPayTestCases)
    addAll(promptPayTestCases)
    addAll(revolutPayTestCases)
    addAll(satispayTestCases)
    addAll(sepaDebitTestCases)
    addAll(sequraTestCases)
    addAll(sunbitTestCases)
    addAll(swishTestCases)
    addAll(twintTestCases)
    addAll(weChatPayTestCases)
    addAll(weroTestCases)
    addAll(zipTestCases)
}

internal val lpmBillingAddressTestConfigurations =
    lpmBillingAddressFormValuesToParamsTestCases.map { it.config }

internal object LpmBillingAddressFormValuesToParamsTestCaseProvider : TestParameterValuesProvider() {
    override fun provideValues(context: Context?): List<LpmBillingAddressFormValuesToParamsTestCase> {
        return lpmBillingAddressFormValuesToParamsTestCases
    }
}

internal object LpmBillingAddressTestConfigurationProvider : TestParameterValuesProvider() {
    override fun provideValues(context: Context?): List<LpmBillingAddressTestConfiguration> {
        return lpmBillingAddressTestConfigurations
    }
}
