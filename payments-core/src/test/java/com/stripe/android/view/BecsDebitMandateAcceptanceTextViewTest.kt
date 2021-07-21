package com.stripe.android.view

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class BecsDebitMandateAcceptanceTextViewTest {

    private val textView = BecsDebitMandateAcceptanceTextView(
        ApplicationProvider.getApplicationContext()
    )

    @Test
    fun companyName_whenUpdated_shouldUpdatedText() {
        assertThat(textView.text.toString())
            .isEqualTo("")

        textView.companyName = "Rocketship Inc."

        assertThat(textView.text.toString().trim())
            .isEqualTo("By providing your bank account details and confirming this payment, you agree to this Direct Debit Request and the Direct Debit Request service agreement, and authorise Stripe Payments Australia Pty Ltd ACN 160 180 343 Direct Debit User ID number 507156 (“Stripe”) to debit your account through the Bulk Electronic Clearing System (BECS) on behalf of Rocketship Inc. (the \"Merchant\") for any amounts separately communicated to you by the Merchant. You certify that you are either an account holder or an authorised signatory on the account listed above.")
    }
}
