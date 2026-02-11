package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.IbanConfig
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.ui.core.elements.SetAsDefaultPaymentMethodElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.SimpleTextFieldController
import org.junit.Test

class SepaDebitDefinitionTest {
    @Test
    fun `'createFormElements' includes default elements for payment intent`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("sepa_debit")
            ),
        )

        val formElements = SepaDebitDefinition.formElements(metadata)

        assertThat(formElements).hasSize(5)

        checkNameField(formElements, 0)
        checkEmailField(formElements, 1)
        checkIbanField(formElements, 2)
        checkBillingField(formElements, 3)
        checkMandateField(formElements, metadata, 4)
    }

    @Test
    fun `'createFormElements' includes default elements for setup intent`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("sepa_debit")
            ),
        )

        val formElements = SepaDebitDefinition.formElements(metadata)

        assertThat(formElements).hasSize(5)

        checkNameField(formElements, 0)
        checkEmailField(formElements, 1)
        checkIbanField(formElements, 2)
        checkBillingField(formElements, 3)
        checkMandateField(formElements, metadata, 4)
    }

    @Test
    fun `'createFormElements' includes phone number element when required`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("sepa_debit")
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            )
        )

        val formElements = SepaDebitDefinition.formElements(metadata)

        assertThat(formElements).hasSize(6)

        checkNameField(formElements, 0)
        checkEmailField(formElements, 1)
        checkPhoneField(formElements, 2)
        checkIbanField(formElements, 3)
        checkBillingField(formElements, 4)
        checkMandateField(formElements, metadata, 5)
    }

    @Test
    fun `'createFormElements' does not includes contact & address fields when explicitly not requested`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("sepa_debit")
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
            )
        )

        val formElements = SepaDebitDefinition.formElements(metadata)

        assertThat(formElements).hasSize(2)

        checkIbanField(formElements, 0)
        checkMandateField(formElements, metadata, 1)
    }

    @Test
    fun `'createFormElements' sets iban element with initial value when provided`() {
        val iban = "AT611904300234573201"
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("sepa_debit")
            ),
        )

        val formElements = SepaDebitDefinition.formElements(
            metadata = metadata,
            paymentMethodCreateParams = PaymentMethodCreateParams.create(
                sepaDebit = PaymentMethodCreateParams.SepaDebit(
                    iban = iban,
                ),
            ),
        )

        val ibanController = ibanController(formElements, 2)

        assertThat(ibanController.fieldValue.value).isEqualTo(iban)
    }

    @Test
    fun `'createFormElements' includes 'SaveForFutureUseElement' when changeable`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("sepa_debit")
            ),
            hasCustomerConfiguration = true
        )

        val formElements = SepaDebitDefinition.formElements(metadata)

        assertThat(formElements).hasSize(6)

        checkNameField(formElements, 0)
        checkEmailField(formElements, 1)
        checkIbanField(formElements, 2)
        checkBillingField(formElements, 3)
        checkSaveField(formElements, 4)
        checkMandateField(formElements, metadata, 5)
    }

    @Test
    fun `'createFormElements' includes 'SetAsDefaultPaymentMethodElement' when enabled`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("sepa_debit")
            ),
            hasCustomerConfiguration = true,
            isPaymentMethodSetAsDefaultEnabled = true
        )

        val formElements = SepaDebitDefinition.formElements(metadata)

        assertThat(formElements).hasSize(7)

        checkNameField(formElements, 0)
        checkEmailField(formElements, 1)
        checkIbanField(formElements, 2)
        checkBillingField(formElements, 3)
        checkSaveField(formElements, 4)
        checkSetAsDefaultField(formElements, 5)
        checkMandateField(formElements, metadata, 6)
    }

    @Test
    fun `'createFormElements' does not include 'SetAsDefaultPaymentMethodElement' when disabled`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("sepa_debit")
            ),
            hasCustomerConfiguration = true,
            isPaymentMethodSetAsDefaultEnabled = false
        )

        val formElements = SepaDebitDefinition.formElements(metadata)

        assertThat(formElements).hasSize(6)

        checkNameField(formElements, 0)
        checkEmailField(formElements, 1)
        checkIbanField(formElements, 2)
        checkBillingField(formElements, 3)
        checkSaveField(formElements, 4)
        checkMandateField(formElements, metadata, 5)
    }

    @Test
    fun `'createFormElements' with SetupIntent includes 'SaveForFutureUseElement' when save behavior is enabled`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("sepa_debit")
            ),
            customerMetadataPermissions = PaymentMethodMetadataFixtures.DEFAULT_CUSTOMER_METADATA_PERMISSIONS.copy(
                saveConsent = PaymentMethodSaveConsentBehavior.Enabled
            ),
            hasCustomerConfiguration = true
        )

        val formElements = SepaDebitDefinition.formElements(metadata)

        assertThat(formElements).hasSize(6)

        checkNameField(formElements, 0)
        checkEmailField(formElements, 1)
        checkIbanField(formElements, 2)
        checkBillingField(formElements, 3)
        checkSaveField(formElements, 4)
        checkMandateField(formElements, metadata, 5)
    }

    private fun checkIbanField(
        formElements: List<FormElement>,
        position: Int,
    ) {
        val ibanController = ibanController(formElements, position)

        assertThat(ibanController.textFieldConfig).isInstanceOf<IbanConfig>()
    }

    private fun checkMandateField(
        formElements: List<FormElement>,
        metadata: PaymentMethodMetadata,
        position: Int,
    ) {
        val element = formElements[position]

        assertThat(element).isInstanceOf<MandateTextElement>()

        val mandateElement = element as MandateTextElement

        assertThat(mandateElement.stringResId).isEqualTo(R.string.stripe_sepa_mandate)
        assertThat(mandateElement.args).isEqualTo(listOf(metadata.merchantName))
    }

    private fun checkSaveField(
        formElements: List<FormElement>,
        position: Int,
    ) {
        assertThat(formElements[position]).isInstanceOf<SaveForFutureUseElement>()
    }

    private fun checkSetAsDefaultField(
        formElements: List<FormElement>,
        position: Int,
    ) {
        assertThat(formElements[position]).isInstanceOf<SetAsDefaultPaymentMethodElement>()
    }

    private fun ibanController(
        formElements: List<FormElement>,
        position: Int,
    ): SimpleTextFieldController {
        val nameSection = checkSectionField(formElements, "sepa_debit[iban]_section", position)
        assertThat(nameSection.fields).hasSize(1)

        val field = nameSection.fields[0]

        assertThat(field.identifier.v1).isEqualTo("sepa_debit[iban]")
        assertThat(field).isInstanceOf<SimpleTextElement>()

        val ibanField = field as SimpleTextElement

        assertThat(ibanField.controller).isInstanceOf<SimpleTextFieldController>()

        return ibanField.controller as SimpleTextFieldController
    }
}
