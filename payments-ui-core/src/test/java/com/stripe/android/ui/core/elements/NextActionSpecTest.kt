package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.StripeIntentResult
import com.stripe.android.model.LuxeActionCreator
import com.stripe.android.model.LuxeConfirmResponseActionRepository
import com.stripe.android.model.StripeIntent
import org.junit.Test

class NextActionSpecTest {
    @Test
    fun `Verify no next action correlates to not supported`() {
        val luxeNextAction: NextActionSpec? = null
        assertThat(
            luxeNextAction.transform()
        ).isEqualTo(
            LuxeConfirmResponseActionRepository.LuxeAction(
                postConfirmStatusNextStatus = emptyMap(),
                postAuthorizeIntentStatus = emptyMap()
            )
        )
    }

    @Test
    fun `Verify no post confirm status`() {
        assertThat(
            NextActionSpec(
                confirmResponseStatusSpecs = null,
                postConfirmHandlingPiStatusSpecs = null
            ).transform()
        ).isEqualTo(
            LuxeConfirmResponseActionRepository.LuxeAction(
                postConfirmStatusNextStatus = emptyMap(),
                postAuthorizeIntentStatus = emptyMap()
            )
        )
    }

    @Test
    fun `Verify confirm status only correlates to no next action`() {
        val nextActionSpec = NextActionSpec(
            confirmResponseStatusSpecs = ConfirmStatusSpecAssociation(
                requires_action = ConfirmResponseStatusSpecs.FinishedSpec
            ),
            postConfirmHandlingPiStatusSpecs = null
        ).transform()

        assertThat(
            nextActionSpec.postConfirmStatusNextStatus[StripeIntent.Status.RequiresAction]
        ).isInstanceOf(LuxeActionCreator.NoActionCreator::class.java)

        assertThat(
            nextActionSpec.postAuthorizeIntentStatus
        ).isEqualTo(
            mapOf(
                StripeIntent.Status.RequiresAction to StripeIntentResult.Outcome.SUCCEEDED
            )
        )
    }

    @Test
    fun `Verify postStatus values`() {
        assertThat(
            NextActionSpec(
                confirmResponseStatusSpecs = null,
                postConfirmHandlingPiStatusSpecs = PostConfirmStatusSpecAssociation(
                    requires_action = PostConfirmHandlingPiStatusSpecs.CanceledSpec
                )
            ).transform()
        ).isEqualTo(
            LuxeConfirmResponseActionRepository.LuxeAction(
                postConfirmStatusNextStatus = emptyMap(),
                postAuthorizeIntentStatus = mapOf(
                    StripeIntent.Status.RequiresAction to StripeIntentResult.Outcome.CANCELED
                )
            )
        )
    }

    @Test
    fun `Verify confirm status overwrite postStatus values`() {
        val nextActionSpec = NextActionSpec(
            confirmResponseStatusSpecs = ConfirmStatusSpecAssociation(
                requires_action = ConfirmResponseStatusSpecs.FinishedSpec
            ),
            postConfirmHandlingPiStatusSpecs = PostConfirmStatusSpecAssociation(
                requires_action = PostConfirmHandlingPiStatusSpecs.CanceledSpec
            )
        ).transform()

        assertThat(
            nextActionSpec.postConfirmStatusNextStatus[StripeIntent.Status.RequiresAction]
        ).isInstanceOf(LuxeActionCreator.NoActionCreator::class.java)

        assertThat(
            nextActionSpec.postAuthorizeIntentStatus
        ).isEqualTo(
            mapOf(
                StripeIntent.Status.RequiresAction to StripeIntentResult.Outcome.SUCCEEDED
            )
        )
    }
}
