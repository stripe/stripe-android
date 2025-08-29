package com.stripe.android.payments

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.LocaleTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class StripeErrorLocalizationsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @get:Rule
    val localeRule = LocaleTestRule(Locale.US)

    private fun assertLocalizedMessage(code: String, declineCode: String? = null, expected: String) {
        val result = StripeErrorLocalizations.forCode(
            context = localeRule.contextForLocale(context),
            code = code,
            declineCode = declineCode
        )
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun codeWithoutMappingReturnsNull() {
        assertThat(StripeErrorLocalizations.forCode(context, "hi_mom")).isNull()
    }

    @Test
    fun errorsAreLocalizedInNonUsLocales() {
        localeRule.setTemporarily(Locale.GERMAN)
        assertLocalizedMessage(code = "incorrect_number", expected = "Die Kartennummer ist ungültig.")
    }

    @Test
    fun incorrectNumber_returnsInvalidCardNumberMessage() {
        assertLocalizedMessage(code = "incorrect_number", expected = "Your card's number is invalid.")
    }

    @Test
    fun invalidNumber_returnsInvalidCardNumberMessage() {
        assertLocalizedMessage(code = "invalid_number", expected = "Your card's number is invalid.")
    }

    @Test
    fun invalidExpiryMonth_returnsInvalidExpiryMonthMessage() {
        assertLocalizedMessage(code = "invalid_expiry_month", expected = "Your card's expiration month is invalid.")
    }

    @Test
    fun invalidExpiryYear_returnsInvalidExpiryYearMessage() {
        assertLocalizedMessage(code = "invalid_expiry_year", expected = "Your card's expiration year is invalid.")
    }

    @Test
    fun invalidCvc_returnsInvalidCvcMessage() {
        assertLocalizedMessage(code = "invalid_cvc", expected = "Your card's security code is invalid.")
    }

    @Test
    fun expiredCard_returnsExpiredCardMessage() {
        assertLocalizedMessage(code = "expired_card", expected = "Your card has expired")
    }

    @Test
    fun incorrectCvc_returnsInvalidCvcMessage() {
        assertLocalizedMessage(code = "incorrect_cvc", expected = "Your card's security code is invalid.")
    }

    @Test
    fun cardDeclined_returnsCardDeclinedMessage() {
        assertLocalizedMessage(code = "card_declined", expected = "Your card was declined")
    }

    @Test
    fun processingError_returnsProcessingErrorMessage() {
        assertLocalizedMessage(
            code = "processing_error",
            expected = "There was an error processing your card -- try again in a few seconds",
        )
    }

    @Test
    fun invalidOwnerName_returnsInvalidOwnerNameMessage() {
        assertLocalizedMessage(code = "invalid_owner_name", expected = "Your name is invalid.")
    }

    @Test
    fun invalidBankAccountIban_returnsInvalidIbanMessage() {
        assertLocalizedMessage(code = "invalid_bank_account_iban", expected = "The IBAN you entered is invalid.")
    }

    @Test
    fun genericDecline_returnsGenericDeclineMessage() {
        assertLocalizedMessage(code = "generic_decline", expected = "Your payment method was declined.")
    }

    @Test
    fun declineCodeAndGeneralCode_returnsDeclineCodeMessage() {
        assertLocalizedMessage(
            code = "generic_decline",
            declineCode = "incorrect_cvc",
            expected = "Your card's security code is invalid."
        )
    }
}
