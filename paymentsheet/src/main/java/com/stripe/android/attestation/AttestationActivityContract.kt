package com.stripe.android.attestation

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.BundleCompat

internal class AttestationActivityContract :
    ActivityResultContract<AttestationActivityContract.Args, AttestationActivityResult>() {

    override fun createIntent(context: Context, input: Args): Intent {
        return AttestationActivity.createIntent(
            context,
            args = AttestationArgs(
                publishableKey = input.publishableKey,
                productUsage = input.productUsage.toList()
            )
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): AttestationActivityResult {
        val result = intent?.extras?.let {
            BundleCompat.getParcelable(it, EXTRA_RESULT, AttestationActivityResult::class.java)
        }
        return result
            ?: AttestationActivityResult.Failed(IllegalStateException("No result received from AttestationActivity"))
    }

    internal data class Args(
        val publishableKey: String,
        val productUsage: Set<String>
    )

    companion object {
        const val EXTRA_RESULT = "com.stripe.android.attestation.AttestationActivityContract.extra_result"
    }
}
