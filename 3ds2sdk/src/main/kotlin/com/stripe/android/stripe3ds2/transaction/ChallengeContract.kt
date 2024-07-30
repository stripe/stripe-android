package com.stripe.android.stripe3ds2.transaction

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.stripe3ds2.views.ChallengeActivity
import com.stripe.android.stripe3ds2.views.ChallengeViewArgs

/**
 * An [ActivityResultContract] for starting [ChallengeActivity].
 */
class ChallengeContract : ActivityResultContract<ChallengeViewArgs, ChallengeResult>() {
    override fun createIntent(context: Context, input: ChallengeViewArgs): Intent {
        return Intent(context, ChallengeActivity::class.java)
            .putExtras(input.toBundle())
    }

    override fun parseResult(resultCode: Int, intent: Intent?): ChallengeResult {
        return ChallengeResult.fromIntent(intent)
    }
}
