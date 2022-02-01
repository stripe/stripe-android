package com.stripe.android.stripe3ds2.transaction

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.transactions.UiType
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertIs

@RunWith(RobolectricTestRunner::class)
class ChallengeResultTest {

    @Test
    fun `fromIntent should return expected ChallengeResult when valid`() {
        val challengeResult = ChallengeResult.Timeout(
            "01",
            initialUiType = UiType.Text,
            IntentDataFixtures.DEFAULT
        )

        assertThat(
            ChallengeResult.fromIntent(
                Intent().putExtras(challengeResult.toBundle())
            )
        ).isEqualTo(challengeResult)
    }

    @Test
    fun `fromIntent should return RuntimeError when invalid`() {
        val challengeResult = assertIs<ChallengeResult.RuntimeError>(
            ChallengeResult.fromIntent(Intent())
        )
        assertThat(
            assertIs<IllegalStateException>(challengeResult.throwable).message
        ).isEqualTo("Intent extras did not contain a valid ChallengeResult.")
    }
}
