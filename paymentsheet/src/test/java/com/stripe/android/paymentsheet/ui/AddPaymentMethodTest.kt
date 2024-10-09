package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.os.Build
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams.Companion.getNameFromParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentsheet.ViewActionRecorder
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.elements.CheckboxFieldElement
import com.stripe.android.uicore.elements.DEFAULT_CHECKBOX_TEST_TAG
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class AddPaymentMethodTest {

    @get:Rule
    val composeRule = createComposeRule()

    val context: Context = getApplicationContext()
    val metadata = PaymentMethodMetadataFactory.create(
        stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("card", "klarna")
        ),
        externalPaymentMethodSpecs = listOf(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC)
    )

    @Test
    fun `transformToPaymentSelection transforms cards correctly`() {
        val cardBrand = "visa"
        val name = "Joe"
        val customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse
        val formFieldValues = FormFieldValues(
            fieldValuePairs = mapOf(
                IdentifierSpec.CardBrand to FormFieldEntry(cardBrand, true),
                IdentifierSpec.Name to FormFieldEntry(name, true),
            ),
            userRequestedReuse = customerRequestedSave,
        )
        val cardPaymentMethod = metadata.supportedPaymentMethodForCode("card")!!
        val cardPaymentSelection = formFieldValues.transformToPaymentSelection(
            cardPaymentMethod,
            metadata
        ) as PaymentSelection.New.Card
        assertThat(cardPaymentSelection.brand.code).isEqualTo(cardBrand)
        assertThat(cardPaymentSelection.customerRequestedSave).isEqualTo(customerRequestedSave)
        assertThat(getNameFromParams(cardPaymentSelection.paymentMethodCreateParams)).isEqualTo(name)
    }

    @Test
    fun `transformToPaymentSelection transforms external payment methods correctly`() {
        val customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse
        val paypalSpec = PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC
        val name = "Joe"
        val addressLine1 = "123 Main Street"
        val formFieldValues = FormFieldValues(
            fieldValuePairs = mapOf(
                IdentifierSpec.Name to FormFieldEntry(name, true),
                IdentifierSpec.Line1 to FormFieldEntry(addressLine1, true)
            ),
            userRequestedReuse = customerRequestedSave,
        )
        val externalPaymentMethod = metadata.supportedPaymentMethodForCode(paypalSpec.type)!!
        val externalPaymentSelection = formFieldValues.transformToPaymentSelection(
            externalPaymentMethod,
            metadata
        ) as PaymentSelection.ExternalPaymentMethod
        assertThat(externalPaymentSelection.type)
            .isEqualTo(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC.type)
        assertThat(externalPaymentSelection.label.resolve(context)).isEqualTo(paypalSpec.label)
        assertThat(externalPaymentSelection.lightThemeIconUrl).isEqualTo(paypalSpec.lightImageUrl)
        assertThat(externalPaymentSelection.darkThemeIconUrl).isEqualTo(paypalSpec.darkImageUrl)
        assertThat(externalPaymentSelection.iconResource).isEqualTo(0)
        assertThat(externalPaymentSelection.billingDetails?.name).isEqualTo(name)
        assertThat(externalPaymentSelection.billingDetails?.address?.line1).isEqualTo(addressLine1)
    }

    @Test
    fun `transformToPaymentSelection transforms generic payment methods correctly`() {
        val customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse
        val name = "Joe"
        val formFieldValues = FormFieldValues(
            fieldValuePairs = mapOf(
                IdentifierSpec.Name to FormFieldEntry(name, true),
            ),
            userRequestedReuse = customerRequestedSave,
        )
        val klarnaPaymentMethod = metadata.supportedPaymentMethodForCode("klarna")!!
        val klarnaPaymentSelection = formFieldValues.transformToPaymentSelection(
            klarnaPaymentMethod,
            metadata
        ) as PaymentSelection.New.GenericPaymentMethod
        assertThat(klarnaPaymentSelection.paymentMethodCreateParams.typeCode).isEqualTo(klarnaPaymentMethod.code)
        assertThat(klarnaPaymentSelection.label.resolve(context))
            .isEqualTo(klarnaPaymentMethod.displayName.resolve(context))
        assertThat(klarnaPaymentSelection.lightThemeIconUrl).isEqualTo(klarnaPaymentMethod.lightThemeIconUrl)
        assertThat(klarnaPaymentSelection.darkThemeIconUrl).isEqualTo(klarnaPaymentMethod.darkThemeIconUrl)
        assertThat(klarnaPaymentSelection.iconResource).isEqualTo(klarnaPaymentMethod.iconResource)
        assertThat(getNameFromParams(klarnaPaymentSelection.paymentMethodCreateParams)).isEqualTo(name)
    }

    @Test
    fun `selecting a new payment method starts an OnPaymentMethodSelected ViewAction`() = runTest {
        runScenario {
            assertThat(viewActionRecorder.viewActions).isEmpty()

            composeRule.onNodeWithTag("PaymentMethodsUITestTagcard", useUnmergedTree = true).performClick()
            composeRule.waitForIdle()

            viewActionRecorder.consume(
                AddPaymentMethodInteractor.ViewAction.OnPaymentMethodSelected("card")
            )
            assertThat(viewActionRecorder.viewActions).isEmpty()
        }
    }

    @Test
    fun `interacting with a form field sends form events`() = runTest {
        runScenario {
            assertThat(viewActionRecorder.viewActions).isEmpty()

            composeRule.onNodeWithTag(DEFAULT_CHECKBOX_TEST_TAG, useUnmergedTree = true).performClick()
            composeRule.waitForIdle()

            viewActionRecorder.consume(
                AddPaymentMethodInteractor.ViewAction.ReportFieldInteraction("card")
            )
            viewActionRecorder.consume(
                AddPaymentMethodInteractor.ViewAction.OnFormFieldValuesChanged(
                    formValues = FormFieldValues(
                        fieldValuePairs = mapOf(
                            IdentifierSpec.SameAsShipping to FormFieldEntry(
                                value = "true",
                                isComplete = true
                            )
                        ),
                        userRequestedReuse = PaymentSelection.CustomerRequestedSave.NoRequest,
                    ),
                    selectedPaymentMethodCode = "card",
                )
            )
            assertThat(viewActionRecorder.viewActions).isEmpty()
        }
    }

    @Test
    fun `when customer reuse is not requested, should have allow_redisplay in params`() {
        val metadata = PaymentMethodMetadataFactory.create(
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Enabled,
        )

        val formValues = FormFieldValues(
            fieldValuePairs = mapOf(IdentifierSpec.Name to FormFieldEntry("test", true)),
            userRequestedReuse = PaymentSelection.CustomerRequestedSave.NoRequest,
        )

        val params = formValues.transformToPaymentMethodCreateParams(
            paymentMethodCode = "card",
            paymentMethodMetadata = metadata,
        )

        assertThat(params.toParamMap()).containsEntry("allow_redisplay", "unspecified")
    }

    @Test
    fun `when customer reuse is requested with reuse, should have allow_redisplay in params`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Enabled,
        )

        val formValues = FormFieldValues(
            fieldValuePairs = mapOf(IdentifierSpec.Name to FormFieldEntry("test", true)),
            userRequestedReuse = PaymentSelection.CustomerRequestedSave.RequestReuse,
        )

        val params = formValues.transformToPaymentMethodCreateParams(
            paymentMethodCode = "card",
            paymentMethodMetadata = metadata,
        )

        assertThat(params.toParamMap()).containsEntry("allow_redisplay", "always")
    }

    @Test
    fun `when customer reuse is requested with no reuse, should have allow_redisplay in params`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Enabled,
        )

        val formValues = FormFieldValues(
            fieldValuePairs = mapOf(IdentifierSpec.Name to FormFieldEntry("test", true)),
            userRequestedReuse = PaymentSelection.CustomerRequestedSave.NoRequest,
        )

        val params = formValues.transformToPaymentMethodCreateParams(
            paymentMethodCode = "card",
            paymentMethodMetadata = metadata,
        )

        assertThat(params.toParamMap()).containsEntry("allow_redisplay", "limited")
    }

    private fun runScenario(
        initiallySelectedPaymentMethodType: PaymentMethodCode = PaymentMethod.Type.Card.code,
        block: Scenario.() -> Unit
    ) {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna"),
            ),
        )
        val initialState = AddPaymentMethodInteractor.State(
            selectedPaymentMethodCode = initiallySelectedPaymentMethodType,
            supportedPaymentMethods = paymentMethodMetadata.sortedSupportedPaymentMethods(),
            arguments = FormArguments(
                paymentMethodCode = initiallySelectedPaymentMethodType,
                cbcEligibility = CardBrandChoiceEligibility.Ineligible,
                merchantName = "Example, Inc.",
                hasIntentToSetup = false,
                paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
            ),
            formElements = listOf(
                CheckboxFieldElement(
                    identifier = IdentifierSpec.SameAsShipping,
                )
            ),
            paymentSelection = null,
            processing = false,
            usBankAccountFormArguments = mock(),
        )

        val viewActionRecorder = ViewActionRecorder<AddPaymentMethodInteractor.ViewAction>()

        val addPaymentMethodInteractor = FakeAddPaymentMethodInteractor(initialState, viewActionRecorder)

        composeRule.setContent {
            AddPaymentMethod(interactor = addPaymentMethodInteractor)
        }

        viewActionRecorder.consume(
            AddPaymentMethodInteractor.ViewAction.OnFormFieldValuesChanged(
                formValues = null,
                selectedPaymentMethodCode = initiallySelectedPaymentMethodType
            )
        )

        Scenario(viewActionRecorder).apply(block)
    }

    private data class Scenario(
        val viewActionRecorder: ViewActionRecorder<AddPaymentMethodInteractor.ViewAction>,
    )
}
