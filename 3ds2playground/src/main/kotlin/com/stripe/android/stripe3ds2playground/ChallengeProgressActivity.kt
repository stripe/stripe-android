package com.stripe.android.stripe3ds2playground

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId
import com.stripe.android.stripe3ds2.views.ChallengeProgressFragmentFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class ChallengeProgressActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val args = Args.fromIntent(intent)
        supportFragmentManager.fragmentFactory = ChallengeProgressFragmentFactory(
            args.directoryServerName,
            args.sdkTransactionId,
            args.accentColor
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_challenge_progress)

        lifecycleScope.launch {
            delay(4000)
            finish()
        }
    }

    @Parcelize
    data class Args(
        internal val directoryServerName: String,
        internal val sdkTransactionId: SdkTransactionId,
        internal val accentColor: Int?
    ) : Parcelable {
        internal fun toBundle() = bundleOf(EXTRA_ARGS to this)

        internal companion object {
            private const val EXTRA_ARGS = "extra_args"

            internal fun fromIntent(intent: Intent): Args {
                return requireNotNull(IntentCompat.getParcelableExtra(intent, EXTRA_ARGS, Args::class.java))
            }
        }
    }
}
