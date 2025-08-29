package com.stripe.android.challenge.warmer

import androidx.core.os.BundleCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.challenge.ChallengeFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PassiveChallengeWarmerActivityContractTest {

    @Test
    fun `createIntent creates intent correctly with PassiveChallengeWarmerArgs`() {
        val contract = PassiveChallengeWarmerActivityContract()
        val passiveCaptchaParams = ChallengeFactory.passiveCaptchaParams()
        val args = PassiveChallengeWarmerActivityContract.Args(passiveCaptchaParams)

        val intent = contract.createIntent(ApplicationProvider.getApplicationContext(), args)
        val intentArgs = intent.extras?.let {
            BundleCompat.getParcelable(
                it,
                PassiveChallengeWarmerActivity.EXTRA_ARGS,
                PassiveChallengeWarmerArgs::class.java
            )
        }

        assertThat(intent.component?.className).isEqualTo(PassiveChallengeWarmerActivity::class.java.name)
        assertThat(intentArgs?.passiveCaptchaParams).isEqualTo(passiveCaptchaParams)
    }
}
