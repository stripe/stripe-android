package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.StripeIntentResult
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LuxeNextActionTest {

    @Test
    fun `test get terminal status when intent status requires_action`() {
        assertThat(
            LpmNextActionData.Instance.getTerminalStatus(
                "konbini",
                StripeIntent.Status.RequiresAction
            )
        ).isEqualTo(StripeIntentResult.Outcome.CANCELED)
    }

    @Test
    fun `test get terminal status when intent status success`() {
        assertThat(
            LpmNextActionData.Instance.getTerminalStatus(
                "afterpay_clearpay",
                StripeIntent.Status.Succeeded
            )
        ).isEqualTo(StripeIntentResult.Outcome.SUCCEEDED)
    }

    @Test
    fun `test get next action for konbini`() {
        val nextAction = LpmNextActionData.Instance.getNextAction(
            PaymentIntentFixtures.KONBINI_REQUIES_ACTION
        ) as StripeIntent.NextActionData.RedirectToUrl

        assertThat(nextAction.returnUrl.toString()).isEqualTo("stripesdk://payment_return_url/com.stripe.android.paymentsheet.example")
        assertThat(nextAction.url.path).isEqualTo("https://hooks.stripe.com/afterpay_clearpay/acct_1HvTI7Lu5o3P18Zp/pa_nonce_M5WcnAEWqB7mMANvtyWuxOWAXIHw9T9/redirect")
    }

    @Test
    fun `test get next action for afterpay_clearpay`() {
        val nextAction = LpmNextActionData.Instance.getNextAction(
            PaymentIntentFixtures.AFTERPAY_REQUIRES_ACTION
        ) as StripeIntent.NextActionData.RedirectToUrl

        assertThat(nextAction.returnUrl.toString()).isEqualTo("stripesdk://payment_return_url/com.stripe.android.paymentsheet.example")
        assertThat(nextAction.url.path).isEqualTo("https://hooks.stripe.com/afterpay_clearpay/acct_1HvTI7Lu5o3P18Zp/pa_nonce_M5WcnAEWqB7mMANvtyWuxOWAXIHw9T9/redirect")
    }
}
