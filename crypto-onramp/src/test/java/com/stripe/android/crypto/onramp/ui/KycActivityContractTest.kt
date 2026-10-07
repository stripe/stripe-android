package com.stripe.android.crypto.onramp.ui

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import com.stripe.android.crypto.onramp.model.KycQuestion
import com.stripe.android.crypto.onramp.model.KycQuestionnaire
import com.stripe.android.crypto.onramp.model.KycRequirement
import com.stripe.android.crypto.onramp.model.KycRequirements
import com.stripe.android.link.LinkAppearance
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class KycActivityContractTest {
    private val context = RuntimeEnvironment.getApplication()
    private val contract = KycActivityContract()

    @Test
    fun `createIntent includes requirements and appearance`() {
        val requirements = requirements()

        val intent = contract.createIntent(
            context,
            KycActivityArgs(
                requirements = requirements,
                linkAppearance = LinkAppearance(),
                submissionHandlerKey = "handler_key",
            ),
        )

        assertThat(intent.component?.className).isEqualTo(KycActivity::class.java.name)
        val args = KycActivity.argsFrom(intent)
        assertThat(args?.requirements).isEqualTo(requirements)
        assertThat(args?.appearance).isNotNull()
        assertThat(args?.submissionHandlerKey).isEqualTo("handler_key")
    }

    @Test
    fun `parseResult returns submitted action`() {
        val intent = KycActivity.createResultIntent(
            KycScreenAction.Submitted
        )

        val result = contract.parseResult(Activity.RESULT_OK, intent)

        assertThat(result.action).isEqualTo(KycScreenAction.Submitted)
    }

    @Test
    fun `parseResult without action returns cancelled`() {
        val result = contract.parseResult(Activity.RESULT_CANCELED, null)

        assertThat(result.action).isEqualTo(KycScreenAction.Cancelled)
    }

    private companion object {
        fun requirements(): KycRequirements {
            return KycRequirements(
                userActionRequired = listOf(
                    KycRequirement(
                        description = "screening_questions",
                        requestedBy = "swapped",
                        awaitingActionFrom = "user",
                        errors = emptyList(),
                        document = null,
                        questionnaire = KycQuestionnaire(
                            questions = listOf(
                                KycQuestion(
                                    id = "purchase_purpose",
                                    prompt = "Why are you purchasing cryptocurrency?",
                                    answerType = "free_text",
                                    required = true,
                                )
                            )
                        ),
                    )
                ),
                pendingPartnerAction = emptyList(),
                pendingStripeAction = emptyList(),
                unrecognizedActionOwner = emptyList(),
            )
        }
    }
}
