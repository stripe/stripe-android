package com.stripe.android.challenge.confirmation

import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.StripePaymentController
import com.stripe.android.view.AuthActivityStarterHost

internal interface IntentConfirmationChallengeNextActionStarter {
    fun start(args: IntentConfirmationChallengeActivityContract.Args)

    /**
     * Uses modern [ActivityResultLauncher] API (preferred).
     * Used when [ActivityResultLauncher] is registered via Activity/Fragment.
     */
    class Modern(
        private val launcher: ActivityResultLauncher<IntentConfirmationChallengeActivityContract.Args>
    ) : IntentConfirmationChallengeNextActionStarter {
        override fun start(args: IntentConfirmationChallengeActivityContract.Args) {
            launcher.launch(args)
        }
    }

    /**
     * Uses legacy [android.app.Activity.startActivityForResult] API (fallback).
     * Used when [ActivityResultLauncher] is not available.
     */
    class Legacy(
        private val host: AuthActivityStarterHost
    ) : IntentConfirmationChallengeNextActionStarter {
        override fun start(args: IntentConfirmationChallengeActivityContract.Args) {
            host.startActivityForResult(
                target = IntentConfirmationChallengeActivity::class.java,
                extras = IntentConfirmationChallengeActivity.getBundle(
                    args = IntentConfirmationChallengeArgs(
                        publishableKey = args.publishableKey,
                        intent = args.intent,
                        productUsage = args.productUsage.toList(),
                    )
                ),
                requestCode = StripePaymentController.getRequestCode(args.intent)
            )
        }
    }
}
