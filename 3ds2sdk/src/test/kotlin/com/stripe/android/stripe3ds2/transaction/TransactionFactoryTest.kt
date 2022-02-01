package com.stripe.android.stripe3ds2.transaction

import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.CertificateFixtures
import com.stripe.android.stripe3ds2.ChallengeMessageFixtures
import com.stripe.android.stripe3ds2.observability.FakeErrorReporter
import com.stripe.android.stripe3ds2.security.EphemeralKeyPairGenerator
import com.stripe.android.stripe3ds2.security.StripeEphemeralKeyPairGenerator
import com.stripe.android.stripe3ds2.views.Brand
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.runner.RunWith
import org.mockito.Mockito.verify
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.security.PublicKey
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class TransactionFactoryTest {
    private val errorReporter = FakeErrorReporter()

    private val keyPair = StripeEphemeralKeyPairGenerator(errorReporter).generate()
    private val sdkPublicKey: PublicKey = keyPair.public
    private val dsPublicKey: PublicKey = CertificateFixtures.DS_CERTIFICATE_RSA.publicKey
    private val ephemeralKeyPairGenerator = EphemeralKeyPairGenerator { keyPair }

    private val testDispatcher = TestCoroutineDispatcher()

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `createInitChallengeArgs() should return expected Args`() {
        val args = createTransaction().createInitChallengeArgs(
            CHALLENGE_PARAMETERS,
            timeoutMins = 3,
            IntentDataFixtures.DEFAULT
        )

        assertThat(args.challengeParameters)
            .isEqualTo(CHALLENGE_PARAMETERS)
        // force timeout minutes to be >= 5
        assertThat(args.timeoutMins)
            .isEqualTo(StripeTransaction.MIN_TIMEOUT)
    }

    @Test
    fun createAuthenticationRequestParameters_shouldReturnSuccessfully() =
        testDispatcher.runBlockingTest {
            val areqParamsFactory = mock<AuthenticationRequestParametersFactory>().also {
                whenever(
                    it.create(
                        DIRECTORY_SERVER_ID,
                        dsPublicKey,
                        null,
                        SDK_TRANSACTION_ID,
                        sdkPublicKey
                    )
                ).thenReturn(AuthenticationRequestParametersFixtures.DEFAULT)
            }

            requireNotNull(
                createTransaction(areqParamsFactory).createAuthenticationRequestParameters()
            )
            verify(areqParamsFactory)
                .create(DIRECTORY_SERVER_ID, dsPublicKey, null, SDK_TRANSACTION_ID, sdkPublicKey)
        }

    private fun createTransaction(
        areqParamsFactory: AuthenticationRequestParametersFactory = FakeAuthenticationRequestParametersFactory()
    ): Transaction {
        val transactionFactory = DefaultTransactionFactory(
            areqParamsFactory,
            ephemeralKeyPairGenerator,
            SDK_REFERENCE_NUMBER,
        )
        return transactionFactory.create(
            DIRECTORY_SERVER_ID,
            emptyList(),
            dsPublicKey,
            null,
            SDK_TRANSACTION_ID,
            isLiveMode = true,
            Brand.Visa
        )
    }

    private class FakeAuthenticationRequestParametersFactory :
        AuthenticationRequestParametersFactory {
        override suspend fun create(
            directoryServerId: String,
            directoryServerPublicKey: PublicKey,
            keyId: String?,
            sdkTransactionId: SdkTransactionId,
            sdkPublicKey: PublicKey
        ) = AuthenticationRequestParametersFixtures.DEFAULT
    }

    private companion object {
        private const val DIRECTORY_SERVER_ID = "F000000000"
        private val SDK_TRANSACTION_ID = ChallengeMessageFixtures.SDK_TRANS_ID
        private const val ACS_SIGNED_CONTENT = "acs_signed_content"
        private const val SDK_REFERENCE_NUMBER = "3DS_LOA_SDK_12345"

        private val CHALLENGE_PARAMETERS = ChallengeParameters(
            threeDsServerTransactionId = UUID.randomUUID().toString(),
            acsTransactionId = UUID.randomUUID().toString(),
            acsRefNumber = SDK_REFERENCE_NUMBER,
            acsSignedContent = ACS_SIGNED_CONTENT
        )
    }
}
