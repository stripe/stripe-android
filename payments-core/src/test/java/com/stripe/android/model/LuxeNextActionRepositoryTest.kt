package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.LUXE_NEXT_ACTION
import com.stripe.android.StripeIntentResult
import com.stripe.android.model.LuxeNextActionRepository.Companion.DEFAULT_DATA
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class LuxeNextActionRepositoryTest {

    @Test
    fun `test get terminal status when intent status requires_action`() {
        val lpmNextActionRepository = LuxeNextActionRepository()
        lpmNextActionRepository.update(DEFAULT_DATA)
        assertThat(
            lpmNextActionRepository.getTerminalStatus(
                "konbini",
                StripeIntent.Status.RequiresAction
            )
        ).isEqualTo(StripeIntentResult.Outcome.SUCCEEDED)
    }

    @Test
    fun `test get terminal status when the LPM is not known to LUXE`() {
        val lpmNextActionRepository = LuxeNextActionRepository()
        lpmNextActionRepository.update(DEFAULT_DATA)
        assertThat(
            lpmNextActionRepository.getTerminalStatus(
                "oxxo",
                StripeIntent.Status.Processing
            )
        ).isNull()
    }

    @Test
    fun `test get terminal status when intent status is not valid`() {
        val lpmNextActionRepository = LuxeNextActionRepository()
        lpmNextActionRepository.update(DEFAULT_DATA)
        assertThat(
            lpmNextActionRepository.getTerminalStatus(
                "afterpay_clearpay",
                StripeIntent.Status.Processing
            )
        ).isNull()
    }

    @Test
    fun `test get next action when return url not required and not found`() {
        // TODO: What is the proper handling of this if the path to the next action is not found?
        val lpmNextActionRepository = LuxeNextActionRepository()
        lpmNextActionRepository.update(
            mapOf(
                "konbini" to
                    LuxeNextActionRepository.LuxeNextAction(
                        handleNextActionSpec = mapOf(
                            StripeIntent.Status.RequiresAction to
                                LuxeNextActionRepository.RedirectNextActionSpec(
                                    hostedPagePath = "next_action[konbini_display_details][hosted_voucher_url]",
                                    returnToUrlPath = null
                                )
                        ),
                        handlePiStatus = listOf(
                            LuxeNextActionRepository.PiStatusSpec(
                                associatedStatuses = listOf(StripeIntent.Status.RequiresAction),
                                outcome = StripeIntentResult.Outcome.SUCCEEDED
                            )
                        )
                    )
            )
        )

        val nextAction = lpmNextActionRepository.getNextAction(
            PaymentIntentFixtures.KONBINI_REQUIES_ACTION
        ) as StripeIntent.NextActionData.RedirectToUrl

        assertThat(nextAction.returnUrl).isNull()
        assertThat(nextAction.url.path).isEqualTo(
            "https://payments.stripe.com/" +
                "konbini/voucher/test_YWNjdF8xSWN1c1VMMzJLbFJvdDAxLF9KRlBtckVBMERWM0lBZEUyb"
        )
    }

    @Test
    fun `test requires action if the status legitimately has no next action`() {
        val sepaDebitIntentProcessing = PaymentIntentFixtures.AFTERPAY_REQUIRES_ACTION.copy(
            jsonString = PaymentIntentFixtures.AFTERPAY_REQUIRES_ACTION.jsonString?.replace(
                "afterpay_clearpay",
                "sepa_debit"
            ),
            paymentMethod = PaymentIntentFixtures.AFTERPAY_REQUIRES_ACTION.paymentMethod?.copy(
                type = PaymentMethod.Type.SepaDebit
            ),
            status = StripeIntent.Status.Processing
        )
        // TODO: This should trigger analytics?
        val lpmNextActionRepository = LuxeNextActionRepository()
        lpmNextActionRepository.update(
            mapOf(
                "sepa_debit" to
                    LUXE_NEXT_ACTION.copy(
                        handleNextActionSpec = mapOf(
                            StripeIntent.Status.Processing to null
                        )
                    )
            )
        )

        assertThat(
            lpmNextActionRepository.supportsAction(sepaDebitIntentProcessing)
        ).isTrue()

        assertThat(
            lpmNextActionRepository.getNextAction(sepaDebitIntentProcessing)
        ).isNull()
    }

    @Test
    fun `test requires action if the status is expected and there is a next action`() {
        // TODO: This should trigger analytics?
        val afterpayIntentRequiresAction = PaymentIntentFixtures.AFTERPAY_REQUIRES_ACTION.copy(
            status = StripeIntent.Status.RequiresAction
        )
        val lpmNextActionRepository = LuxeNextActionRepository()
        lpmNextActionRepository.update(
            mapOf(
                "afterpay_clearpay" to
                    LUXE_NEXT_ACTION.copy(
                        handleNextActionSpec = mapOf(
                            StripeIntent.Status.RequiresAction to
                                LuxeNextActionRepository.RedirectNextActionSpec(
                                    hostedPagePath = "next_action[redirect_to_url][url]",
                                    returnToUrlPath = "next_action[redirect_to_url][return_url]"
                                )
                        )
                    )
            )
        )

        assertThat(
            lpmNextActionRepository.supportsAction(afterpayIntentRequiresAction)
        ).isTrue()

        val nextAction = lpmNextActionRepository.getNextAction(afterpayIntentRequiresAction)
        assertThat(nextAction?.returnUrl.toString()).isEqualTo(
            "stripesdk://payment_return_url/com.stripe.android.paymentsheet.example"
        )
        assertThat(nextAction?.url?.path).isEqualTo(
            "https://hooks.stripe.com/afterpay_clearpay/" +
                "acct_1HvTI7Lu5o3P18Zp/pa_nonce_M5WcnAEWqB7mMANvtyWuxOWAXIHw9T9/redirect"
        )
    }

    @Test
    fun `test requires action if the returnUrl is expected and there is not one`() {
        // TODO: This should trigger analytics?
        val afterpayIntentRequiresAction = PaymentIntentFixtures.AFTERPAY_REQUIRES_ACTION_NO_RETURN_URL.copy(
            status = StripeIntent.Status.RequiresAction
        )
        val lpmNextActionRepository = LuxeNextActionRepository()
        lpmNextActionRepository.update(
            mapOf(
                "afterpay_clearpay" to
                    LUXE_NEXT_ACTION.copy(
                        handleNextActionSpec = mapOf(
                            StripeIntent.Status.RequiresAction to
                                LuxeNextActionRepository.RedirectNextActionSpec(
                                    hostedPagePath = "next_action[redirect_to_url][url]",
                                    returnToUrlPath = "next_action[redirect_to_url][return_url]"
                                )
                        )
                    )
            )
        )

        assertThat(
            lpmNextActionRepository.supportsAction(afterpayIntentRequiresAction)
        ).isTrue()

        assertFailsWith(Exception::class) {
            lpmNextActionRepository.getNextAction(afterpayIntentRequiresAction)
        }
    }

    @Test
    fun `test requires action if the status is not an expected state`() {
        val afterpayIntentRequiresPaymentMethod =
            PaymentIntentFixtures.AFTERPAY_REQUIRES_ACTION.copy(
                status = StripeIntent.Status.RequiresPaymentMethod
            )
        // TODO: This should trigger analytics?
        val lpmNextActionRepository = LuxeNextActionRepository()
        lpmNextActionRepository.update(
            mapOf(
                "afterpay_clearpay" to
                    LUXE_NEXT_ACTION.copy(
                        handleNextActionSpec = mapOf(
                            StripeIntent.Status.RequiresAction to
                                LuxeNextActionRepository.RedirectNextActionSpec(
                                    hostedPagePath = "next_action[redirect_to_url][url]",
                                    returnToUrlPath = "next_action[redirect_to_url][return_url]"
                                )
                        )
                    )
            )
        )

        assertThat(
            lpmNextActionRepository.supportsAction(afterpayIntentRequiresPaymentMethod)
        ).isFalse()
    }
}
