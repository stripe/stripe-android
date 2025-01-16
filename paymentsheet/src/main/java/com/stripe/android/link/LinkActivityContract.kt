package com.stripe.android.link

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.core.utils.FeatureFlags
import javax.inject.Inject

internal class LinkActivityContract @Inject internal constructor(
    private val nativeLinkActivityContract: NativeLinkActivityContract,
    private val webLinkActivityContract: WebLinkActivityContract
) : ActivityResultContract<LinkActivityContract.Args, LinkActivityResult>() {

    override fun createIntent(context: Context, input: Args): Intent {
        return if (FeatureFlags.nativeLinkEnabled.isEnabled && input.configuration.useAttestationEndpointsForLink) {
            nativeLinkActivityContract.createIntent(context, input).apply {
                putExtra(EXTRA_USED_NATIVE_CONTRACT, true)
            }
        } else {
            webLinkActivityContract.createIntent(context, input).apply {
                putExtra(EXTRA_USED_NATIVE_CONTRACT, false)
            }
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): LinkActivityResult {
        val usedNativeContract = intent?.getBooleanExtra(EXTRA_USED_NATIVE_CONTRACT, false) ?: false
        return if (usedNativeContract) {
            nativeLinkActivityContract.parseResult(resultCode, intent)
        } else {
            webLinkActivityContract.parseResult(resultCode, intent)
        }
    }

    data class Args internal constructor(
        internal val configuration: LinkConfiguration
    )

    data class Result(
        val linkResult: LinkActivityResult
    )

    companion object {
        internal const val EXTRA_RESULT =
            "com.stripe.android.link.LinkActivityContract.extra_result"
        private const val EXTRA_USED_NATIVE_CONTRACT = "used_native_contract"
    }
}
