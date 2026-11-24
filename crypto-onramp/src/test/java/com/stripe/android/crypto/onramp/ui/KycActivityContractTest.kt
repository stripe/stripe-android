@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.stripe.android.crypto.onramp.ui

import android.app.Activity
import android.os.Bundle
import android.os.Parcel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.crypto.onramp.di.OnrampComponentHolder
import com.stripe.android.crypto.onramp.model.KycQuestion
import com.stripe.android.crypto.onramp.model.KycQuestionnaire
import com.stripe.android.crypto.onramp.model.KycRequirement
import com.stripe.android.crypto.onramp.model.KycRequirements
import com.stripe.android.crypto.onramp.model.KycSubmission
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkAppearance
import com.stripe.android.link.LinkController
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.model.ConsumerSession
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
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
            ),
        )

        assertThat(intent.component?.className).isEqualTo(KycActivity::class.java.name)
        val args = KycActivity.argsFrom(intent)
        assertThat(args?.requirements).isEqualTo(requirements)
        assertThat(args?.appearance).isNotNull()
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

    @Test
    fun `KYC restores questionnaire progress after process death before the host returns`() {
        val intent = contract.createIntent(context, KycActivityArgs(requirements(), null))
        OnrampComponentHolder.reset()
        val component = OnrampComponentHolder.getOrCreate(context, SavedStateHandle())
        seedVerifiedAccount(component.savedStateHandle)
        val savedState = Bundle()
        lateinit var submitted: KycSubmission
        Robolectric.buildActivity(KycActivity::class.java, intent).use { controller ->
            controller.create()
            val holder = ViewModelProvider(controller.get())[KycViewModel::class.java].stateHolder
            holder.onContinue()
            holder.onQuestionAnswerChanged("purchase_purpose", "Investment")
            submitted = requireNotNull(holder.startSubmission())
            controller.saveInstanceState(savedState)
        }

        val parcel = Parcel.obtain()
        val restoredState = try {
            parcel.writeBundle(savedState)
            parcel.setDataPosition(0)
            requireNotNull(parcel.readBundle(KycActivity::class.java.classLoader))
        } finally {
            parcel.recycle()
        }
        OnrampComponentHolder.reset()
        try {
            Robolectric.buildActivity(KycActivity::class.java, intent).use { controller ->
                controller.create(restoredState)
                val holder = ViewModelProvider(controller.get())[KycViewModel::class.java].stateHolder

                assertThat(controller.get().isFinishing).isFalse()
                assertThat(holder.state.page).isEqualTo(KycCollectionPage.Questionnaire)
                assertThat(holder.state.questions.single().answer).isEqualTo("Investment")
                assertThat(holder.state.canSubmit).isTrue()
                assertThat(holder.startSubmission()).isEqualTo(submitted)
                val restoredComponent = OnrampComponentHolder.getOrCreate(context, SavedStateHandle())
                val account = LinkController.create(context, restoredComponent.savedStateHandle)
                    .state(context).value.internalLinkAccount
                assertThat(account?.linkSessionKey).isEqualTo("test_link_session_key")
                assertThat(account?.consumerSessionClientSecret).isEqualTo("test_consumer_secret")
                assertThat(account?.sessionState).isEqualTo(LinkController.SessionState.LoggedIn)
            }
        } finally {
            OnrampComponentHolder.reset()
        }
    }

    private fun seedVerifiedAccount(handle: SavedStateHandle) {
        LinkAccountHolder(handle).set(
            LinkAccountUpdate.Value(
                LinkAccount(
                    ConsumerSession(
                        clientSecret = "test_consumer_secret",
                        emailAddress = "restoration@example.com",
                        redactedFormattedPhoneNumber = "***",
                        redactedPhoneNumber = "***",
                        linkSessionKey = "test_link_session_key",
                        currentAuthenticationLevel = ConsumerSession.AuthenticationLevel.TwoFactorAuthentication,
                        minimumAuthenticationLevel = ConsumerSession.AuthenticationLevel.TwoFactorAuthentication,
                        verificationSessions = listOf(
                            ConsumerSession.VerificationSession(
                                ConsumerSession.VerificationSession.SessionType.Sms,
                                ConsumerSession.VerificationSession.SessionState.Verified,
                            ),
                        ),
                    ),
                ),
            ),
        )
    }

    private companion object {
        fun requirements(): KycRequirements {
            return KycRequirements(
                userActionRequired = listOf(
                    KycRequirement(
                        description = "source_of_funds_questions",
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
                                    maxQuestionnaireAnswerCharacters = 5_000,
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
