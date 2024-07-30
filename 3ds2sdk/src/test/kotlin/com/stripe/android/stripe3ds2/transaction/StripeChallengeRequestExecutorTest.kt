package com.stripe.android.stripe3ds2.transaction

import android.os.Bundle
import androidx.core.os.BundleCompat
import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.ChallengeMessageFixtures
import com.stripe.android.stripe3ds2.observability.FakeErrorReporter
import com.stripe.android.stripe3ds2.security.MessageTransformer
import com.stripe.android.stripe3ds2.security.StripeDiffieHellmanKeyGenerator
import com.stripe.android.stripe3ds2.security.StripeEphemeralKeyPairGenerator
import com.stripe.android.stripe3ds2.transactions.ChallengeRequestData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.InputStream
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKey
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class StripeChallengeRequestExecutorTest {
    private val errorReporter = FakeErrorReporter()
    private val ephemeralKeyPairGenerator = StripeEphemeralKeyPairGenerator(errorReporter)

    @Test
    fun `Config should serialize correctly as serializable`() {
        val bundle = Bundle().apply { putSerializable("config", ChallengeRequestExecutorFixtures.CONFIG) }

        @Suppress("DEPRECATION")
        assertThat(bundle.getSerializable("config"))
            .isEqualTo(ChallengeRequestExecutorFixtures.CONFIG)
    }

    @Test
    fun `Config should serialize correctly as parcelable`() {
        val bundle = Bundle().apply { putParcelable("config", ChallengeRequestExecutorFixtures.CONFIG) }

        assertThat(BundleCompat.getParcelable(bundle, "config", ChallengeRequestExecutor.Config::class.java))
            .isEqualTo(ChallengeRequestExecutorFixtures.CONFIG)
    }

    @Test
    fun `execute should call ChallengeResponseProcessor`() = runTest {
        val result = createChallengeRequestExecutor()
            .execute(ChallengeMessageFixtures.CREQ)

        assertThat(result)
            .isEqualTo(ChallengeRequestResultFixures.SUCCESS)
    }

    @Test
    fun `execute when challenge request times out should call listener onTimeout()`() = runTest {
        val result = createChallengeRequestExecutor(
            delay = StripeChallengeRequestExecutor.TIMEOUT + TimeUnit.SECONDS.toMillis(1)
        ).execute(
            ChallengeMessageFixtures.CREQ
        )
        advanceTimeBy(StripeChallengeRequestExecutor.TIMEOUT + 1)

        assertThat(result)
            .isInstanceOf(ChallengeRequestResult.Timeout::class.java)
    }

    private fun createChallengeRequestExecutor(
        delay: Long = 0L
    ): ChallengeRequestExecutor {
        val keyPair = ephemeralKeyPairGenerator.generate()
        return StripeChallengeRequestExecutor(
            FakeMessageTransformer(),
            "3DS_LOA_SDK_12345",
            keyPair.private as ECPrivateKey,
            keyPair.public as ECPublicKey,
            "https://example.com",
            errorReporter,
            StripeDiffieHellmanKeyGenerator(errorReporter),
            StandardTestDispatcher(),
            FakeHttpClient(delay),
            ChallengeRequestExecutorFixtures.CONFIG
        ) {
            FakeChallengeResponseProcessor()
        }
    }

    private class FakeMessageTransformer : MessageTransformer {
        override fun encrypt(
            challengeRequest: JSONObject,
            secretKey: SecretKey
        ): String {
            return ""
        }

        override fun decrypt(
            message: String,
            secretKey: SecretKey
        ): JSONObject {
            return JSONObject()
        }
    }

    private class FakeHttpClient(
        private val delay: Long = 0
    ) : HttpClient {
        override suspend fun doGetRequest(): InputStream? {
            return null
        }

        override suspend fun doPostRequest(
            requestBody: String,
            contentType: String
        ): HttpResponse {
            delay(delay)
            return HttpResponse("{}", "application/json")
        }
    }

    private class FakeChallengeResponseProcessor : ChallengeResponseProcessor {
        override suspend fun process(
            creqData: ChallengeRequestData,
            response: HttpResponse
        ) = ChallengeRequestResultFixures.SUCCESS
    }
}
