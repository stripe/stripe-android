package com.stripe.android.lpmfoundations

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.EmailElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.PhoneNumberElement
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.SimpleTextFieldConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.core.R as CoreR

internal enum class ContactInformationCollectionMode {
    Name {
        override fun collectionMode(
            configuration: PaymentSheet.BillingDetailsCollectionConfiguration
        ): PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode = configuration.name

        override fun formElement(
            initialValues: Map<IdentifierSpec, String?>
        ): FormElement = SectionElement.wrap(
            SimpleTextElement(
                identifier = IdentifierSpec.Name,
                controller = SimpleTextFieldController(
                    textFieldConfig = SimpleTextFieldConfig(
                        label = resolvableString(CoreR.string.stripe_address_label_full_name),
                        capitalization = KeyboardCapitalization.Words,
                        keyboard = KeyboardType.Text,
                        optional = false,
                    ),
                    initialValue = initialValues[IdentifierSpec.Name],
                )
            )
        )
    },
    Phone {
        override fun collectionMode(
            configuration: PaymentSheet.BillingDetailsCollectionConfiguration
        ): PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode = configuration.phone

        override fun formElement(
            initialValues: Map<IdentifierSpec, String?>
        ): FormElement = SectionElement.wrap(
            PhoneNumberElement(
                identifier = IdentifierSpec.Phone,
                controller = PhoneNumberController.createPhoneNumberController(
                    initialValue = initialValues[IdentifierSpec.Phone] ?: "",
                )
            )
        )
    },
    Email {
        override fun collectionMode(
            configuration: PaymentSheet.BillingDetailsCollectionConfiguration
        ): PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode = configuration.email

        override fun formElement(
            initialValues: Map<IdentifierSpec, String?>
        ): FormElement = SectionElement.wrap(
            EmailElement(
                identifier = IdentifierSpec.Email,
                initialValue = initialValues[IdentifierSpec.Email],
            )
        )
    };

    abstract fun collectionMode(
        configuration: PaymentSheet.BillingDetailsCollectionConfiguration
    ): PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode

    abstract fun formElement(initialValues: Map<IdentifierSpec, String?>): FormElement

    fun isAllowed(configuration: PaymentSheet.BillingDetailsCollectionConfiguration): Boolean {
        val collectionMode = collectionMode(configuration = configuration)
        return collectionMode != PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never
    }

    fun isRequired(configuration: PaymentSheet.BillingDetailsCollectionConfiguration): Boolean {
        val collectionMode = collectionMode(configuration = configuration)
        return collectionMode == PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
    }
}
