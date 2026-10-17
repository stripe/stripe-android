package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.elements.SimpleDropdownElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.SectionElement
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EpsDefinitionTest {
    private val epsMetadata = PaymentMethodMetadataFactory.create(
        stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("eps"),
        )
    )

    @Test
    fun `createFormElements returns minimal set of fields with name and bank dropdown`() {
        val formElements = EpsDefinition.formElements(epsMetadata)

        assertThat(formElements).hasSize(2)

        checkNameField(formElements, 0)
        checkEpsDropdownField(formElements, 1)
    }

    @Test
    fun `createFormElements returns billing address collection fields`() {
        val formElements = EpsDefinition.formElements(
            metadata = epsMetadata.copy(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                )
            )
        )

        assertThat(formElements).hasSize(5)

        checkNameField(formElements, 0)
        checkEmailField(formElements, 1)
        checkPhoneField(formElements, 2)
        checkEpsDropdownField(formElements, 3)
        checkBillingField(formElements, 4)
    }

    @Test
    fun `createFormElements returns contact information fields`() {
        val formElements = EpsDefinition.formElements(
            metadata = epsMetadata.copy(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                )
            )
        )

        assertThat(formElements).hasSize(4)

        checkNameField(formElements, 0)
        checkEmailField(formElements, 1)
        checkPhoneField(formElements, 2)
        checkEpsDropdownField(formElements, 3)
    }

    private fun checkEpsDropdownField(
        formElements: List<FormElement>,
        position: Int,
    ) {
        val bankSection = formElements[position] as SectionElement
        assertThat(bankSection.fields).hasSize(1)

        val dropdownElement = bankSection.fields[0] as SimpleDropdownElement
        assertThat(dropdownElement.identifier.v1).isEqualTo("eps[bank]")

        val displayItems = dropdownElement.controller.displayItems
        assertThat(displayItems).containsExactly(
            "Ärzte- und Apothekerbank",
            "Austrian Anadi Bank AG",
            "Bank Austria",
            "bank99 AG",
            "Bankhaus Carl Spängler & Co.AG",
            "Bankhaus Schelhammer & Schattera AG",
            "BAWAG P.S.K. AG",
            "BKS Bank AG",
            "BTV VIER LÄNDER BANK",
            "Capital Bank Grawe Gruppe AG",
            "Dolomitenbank",
            "Easybank AG",
            "Erste Bank und Sparkassen",
            "Hypo Alpe-Adria-Bank International AG",
            "HYPO NOE LB für Niederösterreich u. Wien",
            "HYPO Oberösterreich,Salzburg,Steiermark",
            "Hypo Tirol Bank AG",
            "Hypo Vorarlberg Bank AG",
            "HYPO-BANK BURGENLAND Aktiengesellschaft",
            "Marchfelder Bank",
            "Oberbank AG",
            "Raiffeisen Bankengruppe Österreich",
            "Schoellerbank AG",
            "Sparda-Bank Wien",
            "Volksbank Gruppe",
            "Volkskreditbank AG",
            "VR-Bank Braunau",
        ).inOrder()
    }
}
