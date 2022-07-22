package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.LUXE_NEXT_ACTION
import com.stripe.android.StripeIntentResult
import com.stripe.android.model.LuxeNextActionRepository.Companion.DEFAULT_DATA
import com.stripe.android.model.LuxeNextActionRepository.Result
import com.stripe.android.model.PaymentIntentFixtures.AFTERPAY_REQUIRES_ACTION
import com.stripe.android.model.PaymentIntentFixtures.KONBINI_REQUIRES_ACTION
import com.stripe.android.model.PaymentIntentFixtures.OXXO_REQUIES_ACTION
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LuxeActionRepositoryTest {

    @Test
    fun `test get terminal status when intent status for lpm is found in the luxe repo`() {
        val lpmNextActionRepository = LuxeNextActionRepository()
        lpmNextActionRepository.update(DEFAULT_DATA)
        assertThat(
            lpmNextActionRepository.getPostAuthorizeIntentOutcome(
                KONBINI_REQUIRES_ACTION
            )
        ).isEqualTo(StripeIntentResult.Outcome.SUCCEEDED)
    }

    @Test
    fun `test get terminal status when the LPM is not known to LUXE`() {
        val lpmNextActionRepository = LuxeNextActionRepository()
        lpmNextActionRepository.update(DEFAULT_DATA)
        assertThat(
            lpmNextActionRepository.getPostAuthorizeIntentOutcome(
                OXXO_REQUIES_ACTION
            )
        ).isNull()
    }

    @Test
    fun `test get terminal status when the lpm is known but the intent status is not valid`() {
        val afterpayProcessingIntent = AFTERPAY_REQUIRES_ACTION.copy(
            jsonString = AFTERPAY_REQUIRES_ACTION.jsonString?.replace(
                "requires_action",
                "processing"
            ),
            status = StripeIntent.Status.Processing
        )
        val lpmNextActionRepository = LuxeNextActionRepository()
        lpmNextActionRepository.update(DEFAULT_DATA)
        assertThat(
            lpmNextActionRepository.getPostAuthorizeIntentOutcome(
                afterpayProcessingIntent
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
                    LuxeNextActionRepository.LuxeAction(
                        postConfirmStatusNextStatus = LuxeActionCreatorForStatus(
                            StripeIntent.Status.RequiresAction,
                            LuxeActionCreatorForStatus.ActionCreator.RedirectActionCreator(
                                hostedPagePath = "next_action[konbini_display_details][hosted_voucher_url]",
                                returnToUrlPath = null
                            )
                        ),
                        postAuthorizeIntentStatus = mapOf(
                            StripeIntent.Status.RequiresAction to StripeIntentResult.Outcome.SUCCEEDED
                        )
                    )
            )
        )


        val actionResult =
            lpmNextActionRepository.getAction(
                KONBINI_REQUIRES_ACTION
            )
                as Result.Action
        val nextActionData = actionResult.nextActionData
            as StripeIntent.NextActionData.RedirectToUrl

        assertThat(nextActionData.returnUrl).isNull()
        assertThat(nextActionData.url.toString()).isEqualTo(
            "https://payments.stripe.com/" +
                "konbini/voucher/test_YWNjdF8xSWN1c1VMMzJLbFJvdDAxLF9KRlBtckVBMERWM0lBZEUyb"
        )
    }

    @Test
    fun `test requires action if the status legitimately has no next action`() {
        val sepaDebitIntentProcessing = AFTERPAY_REQUIRES_ACTION.copy(
            jsonString = AFTERPAY_REQUIRES_ACTION.jsonString?.replace(
                "afterpay_clearpay",
                "sepa_debit"
            ),
            paymentMethod = AFTERPAY_REQUIRES_ACTION.paymentMethod?.copy(
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
                        postConfirmStatusNextStatus = LuxeActionCreatorForStatus(
                            StripeIntent.Status.Processing,
                            LuxeActionCreatorForStatus.ActionCreator.NoActionCreator
                        )
                    )
            )
        )

        assertThat(
            lpmNextActionRepository.getAction(sepaDebitIntentProcessing)
        ).isEqualTo(Result.NoAction)
    }

    @Test
    fun `test requires action if the status is expected and there is a next action`() {
        // TODO: This should trigger analytics?
        val afterpayIntentRequiresAction = AFTERPAY_REQUIRES_ACTION.copy(
            status = StripeIntent.Status.RequiresAction
        )
        val lpmNextActionRepository = LuxeNextActionRepository()
        lpmNextActionRepository.update(
            mapOf(
                "afterpay_clearpay" to
                    LUXE_NEXT_ACTION.copy(
                        postConfirmStatusNextStatus = LuxeActionCreatorForStatus(
                            StripeIntent.Status.RequiresAction,
                            LuxeActionCreatorForStatus.ActionCreator.RedirectActionCreator(
                                hostedPagePath = "next_action[redirect_to_url][url]",
                                returnToUrlPath = "next_action[redirect_to_url][return_url]"
                            )
                        )
                    )
            )
        )

        val actionResult =
            lpmNextActionRepository.getAction(afterpayIntentRequiresAction)
                as Result.Action
        val nextActionData = actionResult.nextActionData
            as StripeIntent.NextActionData.RedirectToUrl

        assertThat(nextActionData.returnUrl.toString()).isEqualTo(
            "stripesdk://payment_return_url/com.stripe.android.paymentsheet.example"
        )
        assertThat(nextActionData.url.toString()).isEqualTo(
            "https://hooks.stripe.com/afterpay_clearpay/" +
                "acct_1HvTI7Lu5o3P18Zp/pa_nonce_M5WcnAEWqB7mMANvtyWuxOWAXIHw9T9/redirect"
        )
    }

    @Test
    fun `test requires action if the returnUrl is expected and there is not one`() {
        // TODO: This should trigger analytics?
        val afterpayIntentRequiresAction =
            PaymentIntentFixtures.AFTERPAY_REQUIRES_ACTION_NO_RETURN_URL.copy(
                status = StripeIntent.Status.RequiresAction
            )
        val lpmNextActionRepository = LuxeNextActionRepository()
        lpmNextActionRepository.update(
            mapOf(
                "afterpay_clearpay" to
                    LUXE_NEXT_ACTION.copy(
                        postConfirmStatusNextStatus = LuxeActionCreatorForStatus(
                            StripeIntent.Status.RequiresAction,
                            LuxeActionCreatorForStatus.ActionCreator.RedirectActionCreator(
                                hostedPagePath = "next_action[redirect_to_url][url]",
                                returnToUrlPath = "next_action[redirect_to_url][return_url]"
                            )
                        )
                    )
            )
        )

        assertThat(
            lpmNextActionRepository.getAction(afterpayIntentRequiresAction)
        ).isEqualTo(Result.NotSupported)
    }

    @Test
    fun `test requires action if the status is not an expected state`() {
        val afterpayIntentRequiresPaymentMethod =
            AFTERPAY_REQUIRES_ACTION.copy(
                status = StripeIntent.Status.RequiresPaymentMethod
            )
        // TODO: This should trigger analytics?
        val lpmNextActionRepository = LuxeNextActionRepository()
        lpmNextActionRepository.update(
            mapOf(
                "afterpay_clearpay" to
                    LUXE_NEXT_ACTION.copy(
                        postConfirmStatusNextStatus = LuxeActionCreatorForStatus(
                            StripeIntent.Status.RequiresAction,
                            LuxeActionCreatorForStatus.ActionCreator.RedirectActionCreator(
                                hostedPagePath = "next_action[redirect_to_url][url]",
                                returnToUrlPath = "next_action[redirect_to_url][return_url]"
                            )
                        )
                    )
            )
        )

        assertThat(
            lpmNextActionRepository.getAction(afterpayIntentRequiresPaymentMethod)
        ).isEqualTo(Result.NotSupported)
    }
}
