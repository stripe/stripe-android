package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.TestUiDefinitionFactoryArgumentsFactory
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.forms.FormArgumentsFactory
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.ui.transformToPaymentMethodCreateParams
import com.stripe.android.paymentsheet.utils.ViewModelStoreTestRule
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
    fun `creates the expected payment method params`(
        @TestParameter(valuesProvider = TestCaseProvider::class)
        testCase: LpmBillingAddressFormValuesToParamsTestCase,
    ) = runTest {
        assertThat(
            createPaymentMethodCreateParamsFromFormValues(
                testCase = testCase,
            ),
        ).isEqualTo(testCase.expectedPaymentMethodParams)
    }

    private suspend fun createPaymentMethodCreateParamsFromFormValues(
        testCase: LpmBillingAddressFormValuesToParamsTestCase,
    ): PaymentMethodCreateParams {
        val metadata = createMetadata(
            paymentMethodType = testCase.paymentMethodType,
            mode = testCase.mode,
            defaultBillingDetails = testCase.defaultBillingDetails,
        )
        val formViewModel = createFormViewModel(
            paymentMethodType = testCase.paymentMethodType,
            metadata = metadata,
            uiDefinitionFactoryArgumentsFactory = TestUiDefinitionFactoryArgumentsFactory.create(
                paymentMethodCreateParams = testCase.paymentMethodCreateParams,
                paymentMethodExtraParams = testCase.paymentMethodExtraParams,
            ),
        )

        return formViewModel.createPaymentMethodCreateParams(testCase.paymentMethodType, metadata)
    }

    private fun createMetadata(
        paymentMethodType: PaymentMethod.Type,
        mode: LpmBillingAddressBaselineMode,
        defaultBillingDetails: PaymentSheet.BillingDetails,
    ) = PaymentMethodMetadataFactory.create(
        stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf(paymentMethodType.code),
        ),
        billingDetailsCollectionConfiguration = mode.billingDetailsCollectionConfiguration(),
        defaultBillingDetails = defaultBillingDetails,
    )

    private fun createFormViewModel(
        paymentMethodType: PaymentMethod.Type,
        metadata: PaymentMethodMetadata,
        uiDefinitionFactoryArgumentsFactory: UiDefinitionFactory.Arguments.Factory,
    ): FormViewModel {
        val formElements = requireNotNull(
            metadata.formElementsForCode(
                code = paymentMethodType.code,
                uiDefinitionFactoryArgumentsFactory = uiDefinitionFactoryArgumentsFactory,
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

    private suspend fun FormViewModel.createPaymentMethodCreateParams(
        paymentMethodType: PaymentMethod.Type,
        metadata: PaymentMethodMetadata,
    ): PaymentMethodCreateParams {
        return requireNotNull(completeFormValues.first())
            .transformToPaymentMethodCreateParams(
                paymentMethodCode = paymentMethodType.code,
                paymentMethodMetadata = metadata,
            )
    }

    private companion object {
        object TestCaseProvider : TestParameterValuesProvider() {
            override fun provideValues(
                context: Context?,
            ): List<LpmBillingAddressFormValuesToParamsTestCase> = buildList {
                addAll(boletoTestCases)
                addAll(sepaDebitTestCases)
                addAll(weroTestCases)
                addAll(klarnaTestCases)
                addAll(bacsDebitTestCases)
                addAll(oxxoTestCases)
            }
        }
    }
}

internal data class LpmBillingAddressFormValuesToParamsTestCase(
    val name: String,
    val paymentMethodType: PaymentMethod.Type,
    val mode: LpmBillingAddressBaselineMode,
    val defaultBillingDetails: PaymentSheet.BillingDetails,
    val paymentMethodCreateParams: PaymentMethodCreateParams?,
    val paymentMethodExtraParams: PaymentMethodExtraParams?,
    val expectedPaymentMethodParams: PaymentMethodCreateParams,
) {
    override fun toString(): String = name
}
