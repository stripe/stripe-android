package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.StripeIntentResult
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LuxeNextActionRepositoryTest {

    @Test
    fun `test get terminal status when intent status requires_action`() {
        assertThat(
            LuxeNextActionRepository.Instance.getTerminalStatus(
                "konbini",
                StripeIntent.Status.RequiresAction
            )
        ).isEqualTo(StripeIntentResult.Outcome.CANCELED)
    }

    @Test
    fun `test get terminal status when intent status success`() {
        assertThat(
            LuxeNextActionRepository.Instance.getTerminalStatus(
                "afterpay_clearpay",
                StripeIntent.Status.Succeeded
            )
        ).isEqualTo(StripeIntentResult.Outcome.SUCCEEDED)
    }

    @Test
    fun `test get next action for konbini`() {
        val nextAction = LuxeNextActionRepository.Instance.getNextAction(
            PaymentIntentFixtures.KONBINI_REQUIES_ACTION
        ) as StripeIntent.NextActionData.RedirectToUrl

        assertThat(nextAction.returnUrl.toString()).isEqualTo(
            "stripesdk://payment_return_url/com.stripe.android.paymentsheet.example"
        )
        assertThat(nextAction.url.path).isEqualTo(
            "https://hooks.stripe.com/afterpay_clearpay/" +
                "acct_1HvTI7Lu5o3P18Zp/pa_nonce_M5WcnAEWqB7mMANvtyWuxOWAXIHw9T9/redirect"
        )
    }

    @Test
    fun `test get next action for afterpay_clearpay`() {
        val nextAction = LuxeNextActionRepository.Instance.getNextAction(
            PaymentIntentFixtures.AFTERPAY_REQUIRES_ACTION
        ) as StripeIntent.NextActionData.RedirectToUrl

        assertThat(nextAction.returnUrl.toString()).isEqualTo(
            "stripesdk://payment_return_url/com.stripe.android.paymentsheet.example"
        )
        assertThat(nextAction.url.path).isEqualTo(
            "https://hooks.stripe.com/afterpay_clearpay/" +
                "acct_1HvTI7Lu5o3P18Zp/pa_nonce_M5WcnAEWqB7mMANvtyWuxOWAXIHw9T9/redirect"
        )
    }


    @Test
    fun `test requires action if the status is not an expected state`() {
        // TODO: This should trigger analytics
        val lpmNextActionRepository = LuxeNextActionRepository()
        lpmNextActionRepository.update(
            mapOf(
                "afterpay_clearpay" to
                    LuxeNextActionRepository.LuxeNextAction(
                        handleNextActionSpec = mapOf(
                            StripeIntent.Status.RequiresAction to
                                LuxeNextActionRepository.RedirectNextActionSpec(
                                    hostedPagePath = "next_action[redirect_to_url][url]",
                                    returnToUrlPath = "next_action[redirect_to_url][return_url]"
                                )
                        ),
                        handlePiStatus = listOf(
                            LuxeNextActionRepository.PiStatusSpec(
                                associatedStatuses = listOf(StripeIntent.Status.Succeeded),
                                outcome = StripeIntentResult.Outcome.SUCCEEDED
                            )
                        )
                    )
            )
        )

        assertThat(
            lpmNextActionRepository.requiresAction(
                PaymentIntentFixtures.AFTERPAY_REQUIRES_ACTION.copy(
                    status = StripeIntent.Status.RequiresPaymentMethod
                )
            )
        ).isFalse()
    }
}
