package com.stripe.android.uicore.elements

import androidx.compose.ui.autofill.ContentType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class ContentTypeUtilsTest {
    @Test
    fun `toReadableString returns expected values for mapped content types`() {
        val expectedMappings = listOf(
            ContentType.Username to "username",
            ContentType.Password to "password",
            ContentType.EmailAddress to "emailAddress",
            ContentType.NewUsername to "newUsername",
            ContentType.NewPassword to "newPassword",
            ContentType.PostalAddress to "postalAddress",
            ContentType.PostalCode to "postalCode",
            ContentType.CreditCardNumber to "creditCardNumber",
            ContentType.CreditCardSecurityCode to "creditCardSecurityCode",
            ContentType.CreditCardExpirationDate to "creditCardExpirationDate",
            ContentType.CreditCardExpirationMonth to "creditCardExpirationMonth",
            ContentType.CreditCardExpirationYear to "creditCardExpirationYear",
            ContentType.CreditCardExpirationDay to "creditCardExpirationDay",
            ContentType.AddressCountry to "addressCountry",
            ContentType.AddressRegion to "addressRegion",
            ContentType.AddressLocality to "addressLocality",
            ContentType.AddressStreet to "addressStreet",
            ContentType.AddressAuxiliaryDetails to "addressAuxiliaryDetails",
            ContentType.PostalCodeExtended to "postalCodeExtended",
            ContentType.PersonFullName to "personFullName",
            ContentType.PersonFirstName to "personFirstName",
            ContentType.PersonLastName to "personLastName",
            ContentType.PersonMiddleName to "personMiddleName",
            ContentType.PersonMiddleInitial to "personMiddleInitial",
            ContentType.PersonNamePrefix to "personNamePrefix",
            ContentType.PersonNameSuffix to "personNameSuffix",
            ContentType.PhoneNumber to "phoneNumber",
            ContentType.PhoneNumberDevice to "phoneNumberDevice",
            ContentType.PhoneCountryCode to "phoneCountryCode",
            ContentType.PhoneNumberNational to "phoneNumberNational",
            ContentType.Gender to "gender",
            ContentType.BirthDateFull to "birthDateFull",
            ContentType.BirthDateDay to "birthDateDay",
            ContentType.BirthDateMonth to "birthDateMonth",
            ContentType.BirthDateYear to "birthDateYear",
            ContentType.SmsOtpCode to "smsOtpCode",
            ContentType("custom_hint") to "unknown"
        )

        expectedMappings.forEach { (contentType, expected) ->
            assertThat(contentType.toReadableString()).isEqualTo(expected)
        }
    }
}
