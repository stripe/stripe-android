package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.DEFAULT_DATA
import com.stripe.android.LUXE_NEXT_ACTION
import com.stripe.android.StripeIntentResult
import com.stripe.android.model.LuxeActionCreatorForStatus.Companion.getPath
import com.stripe.android.model.LuxeNextActionRepository.Result
import com.stripe.android.model.PaymentIntentFixtures.AFTERPAY_REQUIRES_ACTION
import com.stripe.android.model.PaymentIntentFixtures.AFTERPAY_REQUIRES_ACTION_JSON
import com.stripe.android.model.PaymentIntentFixtures.KONBINI_REQUIRES_ACTION
import com.stripe.android.model.PaymentIntentFixtures.KONBINI_REQUIRES_ACTION_JSON
import com.stripe.android.model.PaymentIntentFixtures.OXXO_REQUIES_ACTION
import org.json.JSONObject
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LuxeActionRepositoryTest {

    @Test
    fun `test get terminal status when intent status for lpm is found in the luxe repo`() {
        val lpmNextActionRepository = LuxeNextActionRepository()
        lpmNextActionRepository.update(
            mapOf(
                "afterpay_clearpay" to
                    LuxeNextActionRepository.LuxeAction(
                        postConfirmStatusNextStatus = LuxeActionCreatorForStatus(
                            StripeIntent.Status.RequiresAction,
                            LuxeActionCreatorForStatus.ActionCreator.RedirectActionCreator(
                                redirectPagePath = "next_action[redirect_to_url][url]",
                                returnToUrlPath = "next_action[redirect_to_url][return_url]"
                            )
                        ),
                        postAuthorizeIntentStatus = mapOf(
                            StripeIntent.Status.Succeeded to StripeIntentResult.Outcome.SUCCEEDED,
                            StripeIntent.Status.RequiresPaymentMethod to StripeIntentResult.Outcome.FAILED,
                            StripeIntent.Status.RequiresAction to StripeIntentResult.Outcome.CANCELED
                        )
                    )
            )
        )
        assertThat(
            lpmNextActionRepository.getPostAuthorizeIntentOutcome(
                AFTERPAY_REQUIRES_ACTION
            )
        ).isEqualTo(StripeIntentResult.Outcome.CANCELED)
    }

    @Test
    fun `test get terminal status when status is requires_action but next action data not supported`() {
        val lpmNextActionRepository = LuxeNextActionRepository()
        lpmNextActionRepository.update(
            mapOf(
                "konbini" to
                    LuxeNextActionRepository.LuxeAction(
                        postConfirmStatusNextStatus = LuxeActionCreatorForStatus(
                            StripeIntent.Status.RequiresAction,
                            LuxeActionCreatorForStatus.ActionCreator.RedirectActionCreator(
                                redirectPagePath = "next_action[konbini_display_details][hosted_voucher_url]",
                                returnToUrlPath = "next_action[konbini_display_details][return_url]"
                            )
                        ),
                        postAuthorizeIntentStatus = mapOf(
                            StripeIntent.Status.Succeeded to StripeIntentResult.Outcome.SUCCEEDED
                        )
                    )
            )
        )

        // This makes it appear as if the next action could not be parsed.
        val stripeIntent = KONBINI_REQUIRES_ACTION.copy(
            nextActionData = null
        )

        assertThat(
            lpmNextActionRepository.getPostAuthorizeIntentOutcome(
                stripeIntent
            )
        ).isEqualTo(StripeIntentResult.Outcome.FAILED)
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

    @Ignore("Ignored because konbini does not have a return url")
    fun `test get next action when return url not required and not found`() {
        val konbiniPaymentMethodCode = "konbini"
        val lpmNextActionRepository = LuxeNextActionRepository()
        lpmNextActionRepository.update(
            mapOf(
                konbiniPaymentMethodCode to
                    LuxeNextActionRepository.LuxeAction(
                        postConfirmStatusNextStatus = LuxeActionCreatorForStatus(
                            StripeIntent.Status.RequiresAction,
                            LuxeActionCreatorForStatus.ActionCreator.RedirectActionCreator(
                                redirectPagePath = "next_action[konbini_display_details][hosted_voucher_url]",
                                returnToUrlPath = ""
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
                konbiniPaymentMethodCode,
                StripeIntent.Status.RequiresAction,
                KONBINI_REQUIRES_ACTION_JSON
            ) as Result.Action
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
        val lpmNextActionRepository = LuxeNextActionRepository()
        lpmNextActionRepository.update(
            mapOf(
                PaymentMethod.Type.SepaDebit.code to
                    LUXE_NEXT_ACTION.copy(
                        postConfirmStatusNextStatus = LuxeActionCreatorForStatus(
                            StripeIntent.Status.Processing,
                            LuxeActionCreatorForStatus.ActionCreator.NoActionCreator
                        )
                    )
            )
        )

        assertThat(
            lpmNextActionRepository.getAction(
                PaymentMethod.Type.SepaDebit.code,
                StripeIntent.Status.Processing,
                JSONObject()
            )
        ).isEqualTo(Result.NoAction)
    }

    @Test
    fun `test requires action if the status is expected and there is a next action`() {
        val lpmNextActionRepository = LuxeNextActionRepository()
        lpmNextActionRepository.update(
            mapOf(
                PaymentMethod.Type.AfterpayClearpay.code to
                    LUXE_NEXT_ACTION.copy(
                        postConfirmStatusNextStatus = LuxeActionCreatorForStatus(
                            StripeIntent.Status.RequiresAction,
                            LuxeActionCreatorForStatus.ActionCreator.RedirectActionCreator(
                                redirectPagePath = "next_action[redirect_to_url][url]",
                                returnToUrlPath = "next_action[redirect_to_url][return_url]"
                            )
                        )
                    )
            )
        )

        val actionResult =
            lpmNextActionRepository.getAction(
                PaymentMethod.Type.AfterpayClearpay.code,
                StripeIntent.Status.RequiresAction,
                AFTERPAY_REQUIRES_ACTION_JSON
            ) as Result.Action
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
        val lpmNextActionRepository = LuxeNextActionRepository()
        lpmNextActionRepository.update(
            mapOf(
                PaymentMethod.Type.AfterpayClearpay.code to
                    LUXE_NEXT_ACTION.copy(
                        postConfirmStatusNextStatus = LuxeActionCreatorForStatus(
                            StripeIntent.Status.RequiresAction,
                            LuxeActionCreatorForStatus.ActionCreator.RedirectActionCreator(
                                redirectPagePath = "next_action[redirect_to_url][url]",
                                returnToUrlPath = "next_action[redirect_to_url][return_url]"
                            )
                        )
                    )
            )
        )

        assertThat(
            getPath(
                "next_action[redirect_to_url][return_url]",
                PaymentIntentFixtures.AFTERPAY_REQUIRES_ACTION_JSON_NO_RETURN_URL
            )
        ).isNull()

        assertThat(
            lpmNextActionRepository.getAction(
                PaymentMethod.Type.AfterpayClearpay.code,
                StripeIntent.Status.RequiresAction,
                PaymentIntentFixtures.AFTERPAY_REQUIRES_ACTION_JSON_NO_RETURN_URL
            )
        ).isEqualTo(Result.NotSupported)
    }

    @Test
    fun `test requires action if the status is not an expected state`() {
        val lpmNextActionRepository = LuxeNextActionRepository()
        lpmNextActionRepository.update(
            mapOf(
                PaymentMethod.Type.AfterpayClearpay.code to
                    LUXE_NEXT_ACTION.copy(
                        postConfirmStatusNextStatus = LuxeActionCreatorForStatus(
                            StripeIntent.Status.RequiresAction,
                            LuxeActionCreatorForStatus.ActionCreator.RedirectActionCreator(
                                redirectPagePath = "next_action[redirect_to_url][url]",
                                returnToUrlPath = "next_action[redirect_to_url][return_url]"
                            )
                        )
                    )
            )
        )

        assertThat(
            lpmNextActionRepository.getAction(
                PaymentMethod.Type.AfterpayClearpay.code,
                StripeIntent.Status.RequiresPaymentMethod,
                JSONObject()
            )
        ).isEqualTo(Result.NotSupported)
    }
}
