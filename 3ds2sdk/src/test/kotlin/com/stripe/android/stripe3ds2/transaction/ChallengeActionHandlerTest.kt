package com.stripe.android.stripe3ds2.transaction

import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.ChallengeMessageFixtures
import com.stripe.android.stripe3ds2.observability.FakeErrorReporter
import com.stripe.android.stripe3ds2.transactions.ChallengeRequestData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ChallengeActionHandlerTest {
    private val challengeRequestExecutor = mock<ChallengeRequestExecutor>()
    private val creqDataArgumentCaptor = argumentCaptor<ChallengeRequestData>()
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun submitNativeForm_shouldPopulateCorrectField() = runTest {
        createChallengeActionHandler().submit(ChallengeAction.NativeForm("123456", whitelistingValue = false))
        advanceTime()
        assertThat(getCreqPayload().optString(ChallengeRequestData.FIELD_CHALLENGE_DATA_ENTRY))
            .isEqualTo("123456")
    }

    @Test
    fun submitNativeForm_withEmptyUserEntry_shouldPopulateCorrectField() = runTest {
        createChallengeActionHandler().submit(ChallengeAction.NativeForm("", whitelistingValue = false))
        advanceTime()
        assertThat(getCreqPayload().optString(ChallengeRequestData.FIELD_CHALLENGE_DATA_ENTRY))
            .isEmpty()
    }

    @Test
    fun submitHtmlForm_withEmptyUserEntry_shouldPopulateCorrectField() = runTest {
        createChallengeActionHandler().submit(ChallengeAction.HtmlForm("123456"))
        advanceTime()
        assertThat(getCreqPayload().optString(ChallengeRequestData.FIELD_CHALLENGE_HTML_DATA_ENTRY))
            .isEqualTo("123456")
    }

    @Test
    fun cancel_shouldPopulateChallengeCancelField() = runTest {
        createChallengeActionHandler().submit(ChallengeAction.Cancel)
        advanceTime()
        assertThat(getCreqPayload().optString(ChallengeRequestData.FIELD_CHALLENGE_CANCEL))
            .isEqualTo(ChallengeRequestData.CancelReason.UserSelected.code)
    }

    @Test
    fun resend_shouldPopulateResendChallengeField() = runTest {
        createChallengeActionHandler().submit(ChallengeAction.Resend)
        advanceTime()
        assertThat(getCreqPayload().optString(ChallengeRequestData.FIELD_RESEND_CHALLENGE))
            .isEqualTo("Y")
    }

    @Test
    fun submitOob_shouldPopulateOobContinue() = runTest {
        createChallengeActionHandler().submit(ChallengeAction.Oob(false))
        advanceTime()
        assertThat(getCreqPayload().optBoolean(ChallengeRequestData.FIELD_OOB_CONTINUE))
            .isTrue()
    }

    private suspend fun getCreqPayload(): JSONObject {
        verify(challengeRequestExecutor)
            .execute(creqDataArgumentCaptor.capture())
        return creqDataArgumentCaptor.firstValue.toJson()
    }

    private fun TestScope.advanceTime() {
        advanceTimeBy(ChallengeActionHandler.Default.CREQ_DELAY + 100)
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
