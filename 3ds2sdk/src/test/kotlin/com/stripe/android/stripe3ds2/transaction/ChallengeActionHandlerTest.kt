package com.stripe.android.stripe3ds2.transaction

import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.ChallengeMessageFixtures
import com.stripe.android.stripe3ds2.observability.FakeErrorReporter
import com.stripe.android.stripe3ds2.transactions.ChallengeRequestData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.json.JSONObject
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class ChallengeActionHandlerTest {
    private val challengeRequestExecutor = mock<ChallengeRequestExecutor>()
    private val creqDataArgumentCaptor = argumentCaptor<ChallengeRequestData>()
    private val testDispatcher = TestCoroutineDispatcher()

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun submitNativeForm_shouldPopulateCorrectField() = testDispatcher.runBlockingTest {
        createChallengeActionHandler().submit(ChallengeAction.NativeForm("123456"))
        advanceTime()
        assertThat(getCreqPayload().optString(ChallengeRequestData.FIELD_CHALLENGE_DATA_ENTRY))
            .isEqualTo("123456")
    }

    @Test
    fun submitNativeForm_withEmptyUserEntry_shouldPopulateCorrectField() = testDispatcher.runBlockingTest {
        createChallengeActionHandler().submit(ChallengeAction.NativeForm(""))
        advanceTime()
        assertThat(getCreqPayload().optString(ChallengeRequestData.FIELD_CHALLENGE_DATA_ENTRY))
            .isEmpty()
    }

    @Test
    fun submitHtmlForm_withEmptyUserEntry_shouldPopulateCorrectField() = testDispatcher.runBlockingTest {
        createChallengeActionHandler().submit(ChallengeAction.HtmlForm("123456"))
        advanceTime()
        assertThat(getCreqPayload().optString(ChallengeRequestData.FIELD_CHALLENGE_HTML_DATA_ENTRY))
            .isEqualTo("123456")
    }

    @Test
    fun cancel_shouldPopulateChallengeCancelField() = testDispatcher.runBlockingTest {
        createChallengeActionHandler().submit(ChallengeAction.Cancel)
        advanceTime()
        assertThat(getCreqPayload().optString(ChallengeRequestData.FIELD_CHALLENGE_CANCEL))
            .isEqualTo(ChallengeRequestData.CancelReason.UserSelected.code)
    }

    @Test
    fun resend_shouldPopulateResendChallengeField() = testDispatcher.runBlockingTest {
        createChallengeActionHandler().submit(ChallengeAction.Resend)
        advanceTime()
        assertThat(getCreqPayload().optString(ChallengeRequestData.FIELD_RESEND_CHALLENGE))
            .isEqualTo("Y")
    }

    @Test
    fun submitOob_shouldPopulateOobContinue() = testDispatcher.runBlockingTest {
        createChallengeActionHandler().submit(ChallengeAction.Oob)
        advanceTime()
        assertThat(getCreqPayload().optBoolean(ChallengeRequestData.FIELD_OOB_CONTINUE))
            .isTrue()
    }

    private suspend fun getCreqPayload(): JSONObject {
        verify(challengeRequestExecutor)
            .execute(creqDataArgumentCaptor.capture())
        return creqDataArgumentCaptor.firstValue.toJson()
    }

    private fun advanceTime() {
        testDispatcher.advanceTimeBy(ChallengeActionHandler.Default.CREQ_DELAY + 100)
    }

    private fun createChallengeActionHandler(): ChallengeActionHandler {
        return ChallengeActionHandler.Default(
            ChallengeMessageFixtures.CREQ,
            FakeErrorReporter(),
            challengeRequestExecutor,
            testDispatcher
        )
    }
}
