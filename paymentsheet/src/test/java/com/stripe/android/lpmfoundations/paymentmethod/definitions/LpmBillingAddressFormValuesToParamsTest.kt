package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
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
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector

@RunWith(RobolectricTestParameterInjector::class)
internal class LpmBillingAddressFormValuesToParamsTest {
    @get:Rule
    val viewModelStoreRule = ViewModelStoreTestRule()

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
    fun `covers every billing mode for every payment method`() {
        lpmBillingAddressTestConfigurations
            .groupBy { it.paymentMethodType }
            .values
            .forEach { configs ->
                assertThat(configs.map { it.billingDetailsCollectionMode })
                    .containsAtLeastElementsIn(LpmBillingDetailsCollectionMode.entries)
            }
    }

    private suspend fun createFormParamsFromFormValues(
        config: LpmBillingAddressTestConfiguration,
        rawValues: Map<IdentifierSpec, String?>,
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
            rawValues = rawValues,
        )
    }

    private fun createFormViewModel(
        paymentMethodType: PaymentMethod.Type,
        metadata: PaymentMethodMetadata,
        initialValues: Map<IdentifierSpec, String?>,
    ): FormViewModel {
        val formElements = requireNotNull(
            metadata.formElementsForCode(
                code = paymentMethodType.code,
                uiDefinitionFactoryArgumentsFactory = TestUiDefinitionFactoryArgumentsFactory.create(
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
        rawValues: Map<IdentifierSpec, String?>,
    ): LpmBillingAddressFormParams {
        val supportedPaymentMethod = requireNotNull(
            metadata.supportedPaymentMethodForCode(paymentMethodType.code),
        )
        val formFieldValues = requireNotNull(completeFormValues.first()) {
            "The ${paymentMethodType.code} form never completed. A required field was not seeded, " +
                "which means its definition ignores UiDefinitionFactory.Arguments.initialValues."
        }

        // A form with elements must report at least one seeded value, otherwise its definition
        // ignored initialValues and the assertion in the test body would be vacuous.
        if (elements.isNotEmpty()) {
            assertThat(formFieldValues.fieldValuePairs.keys).containsAnyIn(rawValues.keys)
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

internal data class LpmBillingAddressFormValuesToParamsTestCase(
    val name: String,
    val config: LpmBillingAddressTestConfiguration,
    val rawValues: Map<IdentifierSpec, String?>,
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
    addAll(boletoTestCases)
    addAll(sepaDebitTestCases)
    addAll(weroTestCases)
    addAll(klarnaTestCases)
    addAll(bacsDebitTestCases)
    addAll(oxxoTestCases)
    addAll(auBecsDebitTestCases)
    addAll(blikTestCases)
    addAll(p24TestCases)
    addAll(epsTestCases)
    addAll(konbiniTestCases)
    addAll(mobilePayTestCases)
    addAll(multibancoTestCases)
    addAll(promptPayTestCases)
    addAll(idealFormParamsTestCases)
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
