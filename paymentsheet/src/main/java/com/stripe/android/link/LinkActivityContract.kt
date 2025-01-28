package com.stripe.android.link

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.link.gate.LinkGate
import javax.inject.Inject

/**
 * Contract used to explicitly launch Link. It will launch either a native or web flow.
 */
internal class LinkActivityContract @Inject internal constructor(
    private val nativeLinkActivityContract: NativeLinkActivityContract,
    private val webLinkActivityContract: WebLinkActivityContract,
    private val linkGateFactory: LinkGate.Factory
) : ActivityResultContract<LinkActivityContract.Args, LinkActivityResult>() {

    override fun createIntent(context: Context, input: Args): Intent {
        val linkGate = linkGateFactory.create(input.configuration)
        return if (linkGate.useNativeLink) {
            nativeLinkActivityContract.createIntent(context, input)
        } else {
            webLinkActivityContract.createIntent(context, input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): LinkActivityResult {
        return when (resultCode) {
            LinkActivity.RESULT_COMPLETE -> {
                nativeLinkActivityContract.parseResult(resultCode, intent)
            }
            else -> {
                webLinkActivityContract.parseResult(resultCode, intent)
            }
        }
    }

    data class Args internal constructor(
        internal val configuration: LinkConfiguration,
        internal val use2faDialog: Boolean
    )

    data class Result(
        val linkResult: LinkActivityResult
    )

    companion object {
        internal const val EXTRA_RESULT =
            "com.stripe.android.link.LinkActivityContract.extra_result"
    }
}
