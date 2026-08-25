package com.stripe.android.crypto.onramp.ui

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import com.stripe.android.crypto.onramp.model.AdditionalKycQuestion
import com.stripe.android.crypto.onramp.model.AdditionalKycQuestionnaire
import com.stripe.android.crypto.onramp.model.AdditionalKycRequirement
import com.stripe.android.crypto.onramp.model.AdditionalKycRequirements
import com.stripe.android.link.LinkAppearance
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AdditionalKycActivityContractTest {
    private val context = RuntimeEnvironment.getApplication()
    private val contract = AdditionalKycActivityContract()

    @Test
    fun `createIntent includes requirements and appearance`() {
        val requirements = requirements()

        val intent = contract.createIntent(
            context,
            AdditionalKycActivityArgs(
                requirements = requirements,
                linkAppearance = LinkAppearance(),
                submissionHandlerKey = "handler_key",
            ),
        )

        assertThat(intent.component?.className).isEqualTo(AdditionalKycActivity::class.java.name)
        val args = AdditionalKycActivity.argsFrom(intent)
        assertThat(args?.requirements).isEqualTo(requirements)
        assertThat(args?.appearance).isNotNull()
        assertThat(args?.submissionHandlerKey).isEqualTo("handler_key")
    }

    @Test
    fun `parseResult returns submitted action`() {
        val intent = AdditionalKycActivity.createResultIntent(
            AdditionalKycScreenAction.Submitted
        )

        val result = contract.parseResult(Activity.RESULT_OK, intent)

        assertThat(result.action).isEqualTo(AdditionalKycScreenAction.Submitted)
    }

    @Test
    fun `parseResult without action returns cancelled`() {
        val result = contract.parseResult(Activity.RESULT_CANCELED, null)

        assertThat(result.action).isEqualTo(AdditionalKycScreenAction.Cancelled)
    }

    private companion object {
        fun requirements(): AdditionalKycRequirements {
            return AdditionalKycRequirements(
                userActionRequired = listOf(
                    AdditionalKycRequirement(
                        description = "screening_questions",
                        requestedBy = "swapped",
                        awaitingActionFrom = "user",
                        requestedReasons = emptyList(),
                        errors = emptyList(),
                        submissionType = "questionnaire",
                        document = null,
                        questionnaire = AdditionalKycQuestionnaire(
                            questions = listOf(
                                AdditionalKycQuestion(
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
