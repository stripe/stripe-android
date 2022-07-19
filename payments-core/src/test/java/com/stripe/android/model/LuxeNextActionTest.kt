package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.StripeIntentResult
import org.junit.Test

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
                "konbini",
                StripeIntent.Status.Succeeded
            )
        ).isEqualTo(StripeIntentResult.Outcome.SUCCEEDED)
    }


    @Test
    fun `test get next action for konbini`() {
        val nextAction = LpmNextActionData.Instance.getNextAction(
            PaymentIntentFixtures.KONBINI_REQUIRES_ACTION
        ) as StripeIntent.NextActionData.RedirectToUrl
        assertThat(nextAction.url.toString()).isEqualTo("https://payments.stripe.com/konbini/voucher/test_YWNjdF8xSWN1c1VMMzJLbFJvdDAxLF9KRlBtckVBMERWM0lBZEUyb")
        assertThat(nextAction.returnUrl.toString()).isEqualTo("example://return_url")
    }
}
