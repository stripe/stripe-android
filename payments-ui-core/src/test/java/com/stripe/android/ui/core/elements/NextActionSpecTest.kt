package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.StripeIntentResult
import com.stripe.android.model.LuxePostConfirmActionCreator
import com.stripe.android.model.LuxePostConfirmActionRepository
import com.stripe.android.model.StripeIntent
import org.junit.Test

class NextActionSpecTest {
    @Test
    fun `Verify no next action correlates to not supported`() {
        val luxeNextAction: NextActionSpec? = null
        assertThat(
            luxeNextAction.transform()
        ).isEqualTo(
            LuxePostConfirmActionRepository.LuxeAction(
                postConfirmStatusToAction = emptyMap(),
                postConfirmActionIntentStatus = emptyMap()
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
            LuxePostConfirmActionRepository.LuxeAction(
                postConfirmStatusToAction = emptyMap(),
                postConfirmActionIntentStatus = emptyMap()
            )
        )
    }

    @Test
    fun `Verify confirm status only correlates to no next action`() {
        val nextActionSpec = NextActionSpec(
            confirmResponseStatusSpecs = ConfirmStatusSpecAssociation(
                requiresAction = ConfirmResponseStatusSpecs.FinishedSpec
            ),
            postConfirmHandlingPiStatusSpecs = null
        ).transform()

        assertThat(
            nextActionSpec.postConfirmStatusToAction[StripeIntent.Status.RequiresAction]
        ).isInstanceOf(LuxePostConfirmActionCreator.NoActionCreator::class.java)

        assertThat(
            nextActionSpec.postConfirmActionIntentStatus
        ).isEqualTo(
            mapOf(
                StripeIntent.Status.RequiresAction to StripeIntentResult.Outcome.SUCCEEDED,
                StripeIntent.Status.Succeeded to StripeIntentResult.Outcome.SUCCEEDED
            )
        )
    }

    @Test
    fun `Verify postStatus values`() {
        assertThat(
            NextActionSpec(
                confirmResponseStatusSpecs = null,
                postConfirmHandlingPiStatusSpecs = PostConfirmStatusSpecAssociation(
                    requiresAction = PostConfirmHandlingPiStatusSpecs.CanceledSpec
                )
            ).transform()
        ).isEqualTo(
            LuxePostConfirmActionRepository.LuxeAction(
                postConfirmStatusToAction = emptyMap(),
                postConfirmActionIntentStatus = mapOf(
                    StripeIntent.Status.RequiresAction to StripeIntentResult.Outcome.CANCELED
                )
            )
        )
    }

    @Test
    fun `Verify confirm status overwrite postStatus values`() {
        val nextActionSpec = NextActionSpec(
            confirmResponseStatusSpecs = ConfirmStatusSpecAssociation(
                requiresAction = ConfirmResponseStatusSpecs.FinishedSpec
            ),
            postConfirmHandlingPiStatusSpecs = PostConfirmStatusSpecAssociation(
                requiresAction = PostConfirmHandlingPiStatusSpecs.CanceledSpec
            )
        ).transform()

        assertThat(
            nextActionSpec.postConfirmStatusToAction[StripeIntent.Status.RequiresAction]
        ).isInstanceOf(LuxePostConfirmActionCreator.NoActionCreator::class.java)

        assertThat(
            nextActionSpec.postConfirmActionIntentStatus
        ).isEqualTo(
            mapOf(
                StripeIntent.Status.RequiresAction to StripeIntentResult.Outcome.SUCCEEDED,
                StripeIntent.Status.Succeeded to StripeIntentResult.Outcome.SUCCEEDED
            )
        )
    }
}
