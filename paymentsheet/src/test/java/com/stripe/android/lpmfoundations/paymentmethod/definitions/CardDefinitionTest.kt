package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.TestFactory
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.lpmfoundations.paymentmethod.link.LinkFormElement
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.TestAutocompleteAddressInteractor
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.ui.core.elements.CardBillingAddressElement
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.ui.core.elements.SetAsDefaultPaymentMethodElement
import com.stripe.android.uicore.elements.AutocompleteAddressController
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.SameAsShippingElement
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CardDefinitionTest {
    @Test
    fun `createFormElements returns minimal set of fields`() {
        val formElements = CardDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
                )
            )
        )
        assertThat(formElements).hasSize(1)
        assertThat(formElements[0].identifier.v1).isEqualTo("card_details")
    }

    @Test
    fun `createFormElements returns default set of fields`() {
        val formElements = CardDefinition.formElements(PaymentMethodMetadataFactory.create())
        assertThat(formElements).hasSize(2)
        assertThat(formElements[0].identifier.v1).isEqualTo("card_details")
        assertThat(formElements[1].identifier.v1).isEqualTo("credit_billing_section")
    }

    @Test
    fun `createFormElements returns requested billing details fields`() {
        val formElements = CardDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                )
            )
        )
        assertThat(formElements).hasSize(3)
        assertThat(formElements[0].identifier.v1).isEqualTo("billing_details[email]_section")
        assertThat(formElements[1].identifier.v1).isEqualTo("card_details")
        assertThat(formElements[2].identifier.v1).isEqualTo("credit_billing_section")

        val contactElement = formElements[0] as SectionElement
        assertThat(contactElement.fields).hasSize(2)
        assertThat(contactElement.fields[0].identifier.v1).isEqualTo("billing_details[email]")
        assertThat(contactElement.fields[1].identifier.v1).isEqualTo("billing_details[phone]")
    }

    @Test
    fun `createFormElements adds a field for same as shipping`() {
        val formElements = CardDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                shippingDetails = AddressDetails(isCheckboxSelected = true)
            )
        )
        assertThat(formElements).hasSize(3)
        assertThat(formElements[0].identifier.v1).isEqualTo("card_details")
        assertThat(formElements[1].identifier.v1).isEqualTo("credit_billing_section")
        assertThat(formElements[2].identifier.v1).isEqualTo("same_as_shipping")
        assertThat(formElements[2]).isInstanceOf(SameAsShippingElement::class.java)
    }

    @Test
    fun `createFormElements returns save_for_future_use`() {
        val formElements = getFormElementsWithSaveForFutureUseAndSetAsDefaultPaymentMethod(
            isPaymentMethodSetAsDefaultEnabled = false,
        )
        assertThat(formElements).hasSize(2)
        assertThat(formElements[0].identifier.v1).isEqualTo("card_details")
        assertThat(formElements[1].identifier.v1).isEqualTo("save_for_future_use")
    }

    @Test
    fun `createFormElements returns setAsDefaultPaymentMethod when isPaymentMethodSetAsDefaultEnabled`() {
        val formElements = getFormElementsWithSaveForFutureUseAndSetAsDefaultPaymentMethod(
            isPaymentMethodSetAsDefaultEnabled = true,
        )
        assertThat(formElements).hasSize(3)
        assertThat(formElements[0].identifier.v1).isEqualTo("card_details")
        assertThat(formElements[1].identifier.v1).isEqualTo("save_for_future_use")
        assertThat(formElements[2].identifier.v1).isEqualTo("set_as_default_payment_method")
    }

    @Test
    fun `setAsDefaultPaymentMethod shown when saveForFutureUse checked & setAsDefaultMatchesSaveForFutureUse false`() {
        testSetAsDefaultElements(
            setAsDefaultMatchesSaveForFutureUse = false,
        ) { saveForFutureUseElement, setAsDefaultPaymentMethodElement ->
            val saveForFutureUseController = saveForFutureUseElement.controller

            saveForFutureUseController.onValueChange(true)

            assertThat(setAsDefaultPaymentMethodElement.shouldShowElementFlow.value).isTrue()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `setAsDefaultPaymentMethod hidden when saveForFutureUse unchecked & setAsDefaultMatchesSaveForFutureUse false`() {
        testSetAsDefaultElements(
            setAsDefaultMatchesSaveForFutureUse = false,
        ) { saveForFutureUseElement, setAsDefaultPaymentMethodElement ->
            val saveForFutureUseController = saveForFutureUseElement.controller

            saveForFutureUseController.onValueChange(true)

            assertThat(setAsDefaultPaymentMethodElement.shouldShowElementFlow.value).isTrue()

            saveForFutureUseController.onValueChange(false)

            assertThat(setAsDefaultPaymentMethodElement.shouldShowElementFlow.value).isFalse()
        }
    }

    @Test
    fun `setAsDefaultPaymentMethod hidden when saveForFutureUse unchecked & setAsDefaultMatchesSaveForFutureUse`() {
        testSetAsDefaultElements(
            setAsDefaultMatchesSaveForFutureUse = true,
        ) { saveForFutureUseElement, setAsDefaultPaymentMethodElement ->
            val saveForFutureUseController = saveForFutureUseElement.controller

            saveForFutureUseController.onValueChange(false)
            assertThat(setAsDefaultPaymentMethodElement.shouldShowElementFlow.value).isFalse()
        }
    }

    @Test
    fun `setAsDefaultPM field false when saveForFutureUse unchecked & setAsDefaultMatchesSaveForFutureUse`() {
        testSetAsDefaultElements(
            setAsDefaultMatchesSaveForFutureUse = true,
        ) { saveForFutureUseElement, setAsDefaultPaymentMethodElement ->
            val saveForFutureUseController = saveForFutureUseElement.controller

            saveForFutureUseController.onValueChange(false)
            assertThat(setAsDefaultPaymentMethodElement.controller.fieldValue.value.toBoolean()).isFalse()
        }
    }

    @Test
    fun `setAsDefaultPaymentMethod hidden when saveForFutureUse checked & setAsDefaultMatchesSaveForFutureUse`() {
        testSetAsDefaultElements(
            setAsDefaultMatchesSaveForFutureUse = true,
        ) { saveForFutureUseElement, setAsDefaultPaymentMethodElement ->
            val saveForFutureUseController = saveForFutureUseElement.controller

            saveForFutureUseController.onValueChange(true)
            assertThat(setAsDefaultPaymentMethodElement.shouldShowElementFlow.value).isFalse()
        }
    }

    @Test
    fun `setAsDefaultPaymentMethod field true when saveForFutureUse checked & setAsDefaultMatchesSaveForFutureUse`() {
        testSetAsDefaultElements(
            setAsDefaultMatchesSaveForFutureUse = true,
        ) { saveForFutureUseElement, setAsDefaultPaymentMethodElement ->
            val saveForFutureUseController = saveForFutureUseElement.controller

            saveForFutureUseController.onValueChange(true)
            assertThat(setAsDefaultPaymentMethodElement.controller.fieldValue.value.toBoolean()).isTrue()
        }
    }

    @Test
    fun `when initialized with setupFutureUsage set, saveForFutureUse checked, setAsDefault unchecked`() {
        testSetAsDefaultElements(
            setAsDefaultMatchesSaveForFutureUse = true,
            paymentMethodOptionsParams = PaymentMethodOptionsParams.Card(
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
            ),
            paymentMethodExtraParams = null
        ) { saveForFutureUseElement, setAsDefaultPaymentMethodElement ->

            assertThat(saveForFutureUseElement.controller.saveForFutureUse.value).isTrue()
            assertThat(setAsDefaultPaymentMethodElement.controller.setAsDefaultPaymentMethodChecked.value).isFalse()
        }
    }

    @Test
    fun `when initialized with setupFutureUsage & setAsDefault set, saveForFutureUse checked, setAsDefault checked`() {
        testSetAsDefaultElements(
            setAsDefaultMatchesSaveForFutureUse = true,
            paymentMethodOptionsParams = PaymentMethodOptionsParams.Card(
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
            ),
            paymentMethodExtraParams = PaymentMethodExtraParams.Card(
                setAsDefault = true
            )
        ) { saveForFutureUseElement, setAsDefaultPaymentMethodElement ->

            assertThat(saveForFutureUseElement.controller.saveForFutureUse.value).isTrue()
            assertThat(setAsDefaultPaymentMethodElement.controller.setAsDefaultPaymentMethodChecked.value).isTrue()
        }
    }

    @Test
    fun `createFormElements returns mandate when has intent to setup`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            linkState = LinkState(
                signupMode = null,
                configuration = createLinkConfiguration().copy(linkSignUpOptInFeatureEnabled = true),
                loginState = LinkState.LoginState.LoggedOut,
            ),
        )

        val formElements = CardDefinition.formElements(
            metadata,
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
        )

        assertThat(formElements).hasSize(3)

        testStaticMandateElement(metadata, formElements[2])
    }

    @Test
    fun `createFormElements returns no mandate if linkSignUpOptInFeatureEnabled but no email passed in`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            linkState = LinkState(
                signupMode = null,
                configuration = createLinkConfiguration().copy(
                    linkSignUpOptInFeatureEnabled = true,
                    customerInfo = LinkConfiguration.CustomerInfo(
                        name = null,
                        email = null,
                        phone = null,
                        billingCountryCode = null,
                    ),
                ),
                loginState = LinkState.LoginState.LoggedOut,
            ),
        )

        val formElements = CardDefinition.formElements(
            metadata,
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
        )

        assertThat(formElements).hasSize(2)
        assertThat(formElements.filterIsInstance<CombinedLinkMandateElement>()).isEmpty()
    }

    @Test
    fun `createFormElements returns static mandate when signup toggle feature is disabled`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
        )

        val formElements = CardDefinition.formElements(
            metadata,
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
        )

        assertThat(formElements).hasSize(3)

        testStaticMandateElement(metadata, formElements[2])
    }

    @Test
    fun `createFormElements returns link_form`() {
        val formElements = CardDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                linkState = LinkState(
                    signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
                    configuration = createLinkConfiguration(),
                    loginState = LinkState.LoginState.LoggedOut,
                ),
            ),
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
        )

        assertThat(formElements).hasSize(3)
        assertThat(formElements[2].identifier.v1).isEqualTo("link_form")
        assertThat(formElements[2]).isInstanceOf(LinkFormElement::class.java)
    }

    @Test
    fun `createFormElements returns mandate below link_form when has intent to setup`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            linkState = LinkState(
                signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
                configuration = createLinkConfiguration().copy(linkSignUpOptInFeatureEnabled = true),
                loginState = LinkState.LoginState.LoggedOut,
            ),
        )

        val formElements = CardDefinition.formElements(
            metadata,
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
        )

        assertThat(formElements).hasSize(4)
        assertThat(formElements[2].identifier.v1).isEqualTo("link_form")

        testCombinedLinkMandateElement(formElements[3])
    }

    @Test
    fun `createFormElements should have autocomplete element if factory is used`() {
        val formElements = CardDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                )
            ),
            autocompleteAddressInteractorFactory = {
                TestAutocompleteAddressInteractor.noOp()
            }
        )

        val cardBillingAddressElements = formElements.filterIsInstance<SectionElement>().map {
            it.fields.filterIsInstance<CardBillingAddressElement>().firstOrNull()
        }

        assertThat(cardBillingAddressElements).hasSize(1)
        assertThat(cardBillingAddressElements.firstOrNull()?.sectionFieldErrorController())
            .isInstanceOf<AutocompleteAddressController>()
    }

    private fun createLinkConfiguration(): LinkConfiguration {
        return TestFactory.LINK_CONFIGURATION.copy(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        )
    }

    private fun testCombinedLinkMandateElement(formElement: FormElement) {
        assertThat(formElement.identifier.v1).isEqualTo("card_mandate")
        assertThat(formElement).isInstanceOf(CombinedLinkMandateElement::class.java)
    }

    private fun testStaticMandateElement(metadata: PaymentMethodMetadata, formElement: FormElement) {
        assertThat(formElement.identifier.v1).isEqualTo("card_mandate")
        assertThat(formElement).isInstanceOf(MandateTextElement::class.java)

        val mandateElement = formElement.asMandateTextElement()

        assertThat(mandateElement.stringResId).isEqualTo(R.string.stripe_paymentsheet_card_mandate)
        assertThat(mandateElement.args).containsExactly(metadata.merchantName)
    }

    private fun FormElement.asMandateTextElement(): MandateTextElement {
        return this as MandateTextElement
    }

    private fun testSetAsDefaultElements(
        setAsDefaultMatchesSaveForFutureUse: Boolean,
        paymentMethodExtraParams: PaymentMethodExtraParams? = null,
        paymentMethodOptionsParams: PaymentMethodOptionsParams? = null,
        block: (SaveForFutureUseElement, SetAsDefaultPaymentMethodElement) -> Unit
    ) {
        val formElements = getFormElementsWithSaveForFutureUseAndSetAsDefaultPaymentMethod(
            isPaymentMethodSetAsDefaultEnabled = true,
            setAsDefaultMatchesSaveForFutureUse = setAsDefaultMatchesSaveForFutureUse,
            paymentMethodExtraParams = paymentMethodExtraParams,
            paymentMethodOptionsParams = paymentMethodOptionsParams,
        )

        val saveForFutureUseElement = formElements[1] as SaveForFutureUseElement
        val setAsDefaultPaymentMethodElement = formElements[2] as SetAsDefaultPaymentMethodElement

        block(saveForFutureUseElement, setAsDefaultPaymentMethodElement)
    }

    private fun getFormElementsWithSaveForFutureUseAndSetAsDefaultPaymentMethod(
        isPaymentMethodSetAsDefaultEnabled: Boolean,
        setAsDefaultMatchesSaveForFutureUse: Boolean = false,
        paymentMethodExtraParams: PaymentMethodExtraParams? = null,
        paymentMethodOptionsParams: PaymentMethodOptionsParams? = null,
    ): List<FormElement> {
        return CardDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
                ),
                hasCustomerConfiguration = true,
                isPaymentMethodSetAsDefaultEnabled = isPaymentMethodSetAsDefaultEnabled,
            ),
            setAsDefaultMatchesSaveForFutureUse = setAsDefaultMatchesSaveForFutureUse,
            paymentMethodExtraParams = paymentMethodExtraParams,
            paymentMethodOptionsParams = paymentMethodOptionsParams,
        )
    }
}
