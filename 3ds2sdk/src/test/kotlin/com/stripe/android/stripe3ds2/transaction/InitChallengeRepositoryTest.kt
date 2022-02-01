package com.stripe.android.stripe3ds2.transaction

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.ChallengeMessageFixtures
import com.stripe.android.stripe3ds2.init.ui.StripeUiCustomization
import com.stripe.android.stripe3ds2.observability.FakeErrorReporter
import com.stripe.android.stripe3ds2.security.DefaultMessageTransformer
import com.stripe.android.stripe3ds2.security.StripeEphemeralKeyPairGenerator
import com.stripe.android.stripe3ds2.transactions.ChallengeRequestData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertIs

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class InitChallengeRepositoryTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val errorReporter = FakeErrorReporter()
    private val keyPair = StripeEphemeralKeyPairGenerator(errorReporter).generate()
    private val acsDataParser = DefaultAcsDataParser(errorReporter)
    private val errorRequestExecutorFactory = ErrorRequestExecutor.Factory { _, _ -> mock() }
    private val jwsValidator = JwsValidator {
        AcsDataFixtures.create(ChallengeRequestExecutorFixtures.CONFIG.acsUrl)
    }
    private val uiCustomization = StripeUiCustomization()
    private val sdkTransactionId = ChallengeMessageFixtures.CREQ.sdkTransId
    private val isLiveMode = true
    private val challengeParameters = ChallengeParameters(
        threeDsServerTransactionId = ChallengeMessageFixtures.CREQ.threeDsServerTransId,
        acsTransactionId = ChallengeMessageFixtures.CREQ.acsTransId,
        acsRefNumber = null,
        acsSignedContent = UUID.randomUUID().toString()
    )

    private val args = InitChallengeArgs(
        ChallengeRequestExecutorFixtures.CONFIG.sdkReferenceId,
        keyPair,
        challengeParameters,
        5,
        IntentDataFixtures.DEFAULT
    )

    private val viewModel = DefaultInitChallengeRepository(
        sdkTransactionId,
        MessageVersionRegistry(),
        jwsValidator,
        messageTransformer = DefaultMessageTransformer(isLiveMode),
        acsDataParser = acsDataParser,
        challengeRequestResultRepository = FakeChallengeRequestResultRepository(),
        errorRequestExecutorFactory = errorRequestExecutorFactory,
        uiCustomization,
        errorReporter,
        Logger.Noop
    )

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `startChallenge() should return expected result`() = testDispatcher.runBlockingTest {
        val startChallenge = assertIs<InitChallengeResult.Start>(
            viewModel.startChallenge(args)
        )
        val challengeViewArgs = startChallenge.challengeViewArgs
        assertThat(challengeViewArgs.cresData)
            .isEqualTo(SUCCESS_RESULT.cresData)
        assertThat(challengeViewArgs.creqData)
            .isEqualTo(SUCCESS_RESULT.creqData)
        assertThat(challengeViewArgs.uiCustomization)
            .isEqualTo(uiCustomization)
        assertThat(challengeViewArgs.timeoutMins)
            .isEqualTo(5)
        assertThat(challengeViewArgs.intentData)
            .isEqualTo(IntentDataFixtures.DEFAULT)
    }

    @Test
    fun `InitChallengeRepositoryFactory create() should return instance`() {
        val initChallengeRepository = InitChallengeRepositoryFactory(
            ApplicationProvider.getApplicationContext(),
            isLiveMode,
            sdkTransactionId,
            uiCustomization,
            rootCerts = emptyList(),
            enableLogging = false,
            testDispatcher
        ).create()

        assertThat(initChallengeRepository)
            .isNotNull()
    }

    private class FakeChallengeRequestResultRepository : ChallengeRequestResultRepository {
        override suspend fun get(
            creqExecutorConfig: ChallengeRequestExecutor.Config,
            challengeRequestData: ChallengeRequestData
        ): ChallengeRequestResult {
            return SUCCESS_RESULT
        }
    }

    private companion object {
        private val SUCCESS_RESULT = ChallengeRequestResult.Success(
            ChallengeMessageFixtures.CREQ,
            ChallengeMessageFixtures.CRES,
            ChallengeRequestExecutorFixtures.CONFIG
        )
    }
}
