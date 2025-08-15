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

    private fun assertLocalizedMessage(code: String, expected: String) {
        val result = StripeErrorLocalizations.forCode(localeRule.contextForLocale(context), code)
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun codeWithoutMappingReturnsNull() {
        assertThat(StripeErrorLocalizations.forCode(context, "hi_mom")).isNull()
    }

    @Test
    fun errorsAreLocalizedInNonUsLocales() {
        localeRule.setTemporarily(Locale.GERMAN)
        assertLocalizedMessage("incorrect_number", "Die Kartennummer ist ungültig.")
    }

    @Test
    fun incorrectNumber_returnsInvalidCardNumberMessage() {
        assertLocalizedMessage("incorrect_number", "Your card's number is invalid.")
    }

    @Test
    fun invalidNumber_returnsInvalidCardNumberMessage() {
        assertLocalizedMessage("invalid_number", "Your card's number is invalid.")
    }

    @Test
    fun invalidExpiryMonth_returnsInvalidExpiryMonthMessage() {
        assertLocalizedMessage("invalid_expiry_month", "Your card's expiration month is invalid.")
    }

    @Test
    fun invalidExpiryYear_returnsInvalidExpiryYearMessage() {
        assertLocalizedMessage("invalid_expiry_year", "Your card's expiration year is invalid.")
    }

    @Test
    fun invalidCvc_returnsInvalidCvcMessage() {
        assertLocalizedMessage("invalid_cvc", "Your card's security code is invalid.")
    }

    @Test
    fun expiredCard_returnsExpiredCardMessage() {
        assertLocalizedMessage("expired_card", "Your card has expired")
    }

    @Test
    fun incorrectCvc_returnsInvalidCvcMessage() {
        assertLocalizedMessage("incorrect_cvc", "Your card's security code is invalid.")
    }

    @Test
    fun cardDeclined_returnsCardDeclinedMessage() {
        assertLocalizedMessage("card_declined", "Your card was declined")
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
        assertLocalizedMessage("invalid_owner_name", "Your name is invalid.")
    }

    @Test
    fun invalidBankAccountIban_returnsInvalidIbanMessage() {
        assertLocalizedMessage("invalid_bank_account_iban", "The IBAN you entered is invalid.")
    }

    @Test
    fun genericDecline_returnsGenericDeclineMessage() {
        assertLocalizedMessage("generic_decline", "Your payment method was declined.")
    }
}
