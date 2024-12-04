package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.luxe.LpmRepositoryTestHelpers
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.definitions.CardDefinition
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParams.Companion.getNameFromParams
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.NullCardAccountRangeRepositoryFactory
import com.stripe.android.ui.core.Amount
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class FormHelperTest {

    @Test
    fun `formElementsForCode with unknown code returns empty list`() {
        val formHelper = createFormHelper(
            newPaymentSelectionProvider = { null },
        )
        assertThat(formHelper.formElementsForCode("blah")).isEmpty()
    }

    @Test
    fun `formElementsForCode returns klarna form elements`() {
        val formHelper = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "klarna"),
                )
            ),
            newPaymentSelectionProvider = { null },
        )
        val formElements = formHelper.formElementsForCode("klarna")
        assertThat(formElements).hasSize(3)
        // Email field has an empty string for value
        assertThat(formElements[1].getFormFieldValueFlow().value[0].first.v1).isEqualTo("billing_details[email]")
        assertThat(formElements[1].getFormFieldValueFlow().value[0].second.value).isEqualTo("")
    }

    @Test
    fun `formElementsForCode returns klarna form elements without using current selection values`() {
        val formHelper = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "klarna"),
                )
            ),
            newPaymentSelectionProvider = {
                NewOrExternalPaymentSelection.New(
                    PaymentSelection.New.GenericPaymentMethod(
                        label = "Cash App".resolvableString,
                        iconResource = 0,
                        lightThemeIconUrl = null,
                        darkThemeIconUrl = null,
                        paymentMethodCreateParams = PaymentMethodCreateParams.createCashAppPay(
                            billingDetails = PaymentMethod.BillingDetails(email = "example@email.com")
                        ),
                        customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
                        paymentMethodOptionsParams = null,
                        paymentMethodExtraParams = null,
                    )
                )
            },
        )
        val formElements = formHelper.formElementsForCode("klarna")
        assertThat(formElements).hasSize(3)
        assertThat(formElements[1].getFormFieldValueFlow().value[0].first.v1).isEqualTo("billing_details[email]")
        assertThat(formElements[1].getFormFieldValueFlow().value[0].second.value).isEqualTo("")
    }

    @Test
    fun `formElementsForCode returns klarna form elements using current selection values`() {
        val formHelper = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "klarna"),
                )
            ),
            newPaymentSelectionProvider = {
                NewOrExternalPaymentSelection.New(
                    PaymentSelection.New.GenericPaymentMethod(
                        label = "Klarna".resolvableString,
                        iconResource = 0,
                        lightThemeIconUrl = null,
                        darkThemeIconUrl = null,
                        paymentMethodCreateParams = PaymentMethodCreateParams.createKlarna(
                            billingDetails = PaymentMethod.BillingDetails(email = "example@email.com")
                        ),
                        customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
                        paymentMethodOptionsParams = null,
                        paymentMethodExtraParams = null,
                    )
                )
            },
        )
        val formElements = formHelper.formElementsForCode("klarna")
        assertThat(formElements).hasSize(3)
        assertThat(formElements[1].getFormFieldValueFlow().value[0].first.v1).isEqualTo("billing_details[email]")
        assertThat(formElements[1].getFormFieldValueFlow().value[0].second.value).isEqualTo("example@email.com")
    }

    @Test
    fun `createFormArguments produces the correct form arguments when payment intent is off-session`() {
        val observedArgs = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_OFF_SESSION
            )
        ).createFormArguments(
            paymentMethodCode = LpmRepositoryTestHelpers.card.code,
        )

        assertThat(observedArgs).isEqualTo(
            PaymentSheetFixtures.COMPOSE_FRAGMENT_ARGS.copy(
                paymentMethodCode = CardDefinition.type.code,
                amount = Amount(
                    value = 1099,
                    currencyCode = "usd",
                ),
                hasIntentToSetup = true,
                billingDetails = PaymentSheet.BillingDetails(),
            )
        )
    }

    @Test
    fun `onFormFieldValuesChanged calls selection updater with transformed card`() {
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
        var hasCalledSelectionUpdater = false
        createFormHelper(
            selectionUpdater = { paymentSelection ->
                val cardPaymentSelection = paymentSelection as PaymentSelection.New.Card
                assertThat(cardPaymentSelection.brand.code).isEqualTo(cardBrand)
                assertThat(cardPaymentSelection.customerRequestedSave).isEqualTo(customerRequestedSave)
                assertThat(getNameFromParams(cardPaymentSelection.paymentMethodCreateParams)).isEqualTo(name)
                hasCalledSelectionUpdater = true
            }
        ).onFormFieldValuesChanged(formFieldValues, "card")
        assertThat(hasCalledSelectionUpdater).isTrue()
    }

    @Test
    fun `requiresFormScreen returns false for an LPM with no fields`() {
        val formHelper = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "cashapp"),
                )
            ),
            newPaymentSelectionProvider = { null },
        )
        assertThat(formHelper.requiresFormScreen("cashapp")).isFalse()
    }

    @Test
    fun `requiresFormScreen returns true for an LPM with no fields, but requires name`() {
        val formHelper = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "cashapp"),
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                ),
            ),
            newPaymentSelectionProvider = { null },
        )
        assertThat(formHelper.requiresFormScreen("cashapp")).isTrue()
    }

    @Test
    fun `requiresFormScreen returns true for an LPM with fields`() {
        val formHelper = createFormHelper(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "klarna"),
                ),
            ),
            newPaymentSelectionProvider = { null },
        )
        assertThat(formHelper.requiresFormScreen("klarna")).isTrue()
    }

    @Test
    fun `requiresFormScreen returns true for non form field based LPM us_bank_account`() {
        val formHelper = createFormHelper(
            newPaymentSelectionProvider = { null },
        )
        assertThat(formHelper.requiresFormScreen("us_bank_account")).isTrue()
        assertThat(formHelper.requiresFormScreen("link")).isTrue()
    }

    private fun createFormHelper(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        newPaymentSelectionProvider: () -> NewOrExternalPaymentSelection? = { throw AssertionError("Not implemented") },
        selectionUpdater: (PaymentSelection?) -> Unit = { throw AssertionError("Not implemented") },
    ): FormHelper {
        return FormHelper(
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
            paymentMethodMetadata = paymentMethodMetadata,
            newPaymentSelectionProvider = newPaymentSelectionProvider,
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
            onLinkInlineSignupStateChanged = { throw AssertionError("Not implemented") },
            selectionUpdater = selectionUpdater,
        )
    }
}
