package com.stripe.android.stripe3ds2.views

import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.ChallengeMessageFixtures
import com.stripe.android.stripe3ds2.init.ui.StripeUiCustomization
import com.stripe.android.stripe3ds2.observability.ErrorReporter
import com.stripe.android.stripe3ds2.transaction.ChallengeRequestExecutor
import com.stripe.android.stripe3ds2.transaction.ChallengeRequestExecutorFixtures
import com.stripe.android.stripe3ds2.transaction.ChallengeRequestResult
import com.stripe.android.stripe3ds2.transaction.IntentDataFixtures
import com.stripe.android.stripe3ds2.transactions.ChallengeRequestData
import com.stripe.android.stripe3ds2.utils.ParcelUtils
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class ChallengeViewArgsTest {
    @Test
    fun create_withNullBundle_shouldThrowException() {
        assertFailsWith<IllegalArgumentException> {
            ChallengeViewArgs.create(Bundle.EMPTY)
        }
    }

    @Test
    fun create_withRequiredValues_shouldSucceed() {
        assertThat(ChallengeViewArgs.create(createArgs().toBundle()))
            .isNotNull()
    }

    @Test
    fun toBundle_shouldHaveCorrectEntries() {
        val args = createArgs()
        val actualArgs = ChallengeViewArgs.create(args.toBundle())
        assertThat(actualArgs.toBundle().keySet())
            .isEqualTo(args.toBundle().keySet())
    }

    @Test
    fun `parcelization roundtrip should return expected object`() {
        val args = createArgs()

        assertThat(createParcelRoundtrip(args))
            .isEqualTo(args)
    }

    private fun createArgs(): ChallengeViewArgs {
        return ChallengeViewArgs(
            cresData = ChallengeMessageFixtures.CRES,
            creqData = ChallengeMessageFixtures.CREQ,
            uiCustomization = StripeUiCustomization(),
            creqExecutorConfig = ChallengeRequestExecutorFixtures.CONFIG,
            creqExecutorFactory = FakeChallengeRequestExecutorFactory,
            timeoutMins = 5,
            intentData = IntentDataFixtures.DEFAULT
        )
    }

    private object FakeChallengeRequestExecutor : ChallengeRequestExecutor {
        override suspend fun execute(
            creqData: ChallengeRequestData
        ): ChallengeRequestResult {
            return ChallengeRequestResult.RuntimeError(RuntimeException())
        }
    }

    private object FakeChallengeRequestExecutorFactory : ChallengeRequestExecutor.Factory {
        override fun create(
            errorReporter: ErrorReporter,
            workContext: CoroutineContext
        ): ChallengeRequestExecutor {
            return FakeChallengeRequestExecutor
        }

        override fun equals(other: Any?): Boolean {
            return other is FakeChallengeRequestExecutorFactory
        }
    }

    private companion object {
        private fun createParcelRoundtrip(
            source: Parcelable
        ): ChallengeViewArgs {
            val bundle = bundleOf(KEY to source)

            return requireNotNull(
                ParcelUtils.copy(bundle, Bundle.CREATOR).getParcelable(KEY)
            )
        }

        private const val KEY = "parcelable"
    }
}
