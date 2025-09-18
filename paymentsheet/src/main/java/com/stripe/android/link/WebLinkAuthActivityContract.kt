package com.stripe.android.link

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.BundleCompat
import com.stripe.android.link.injection.NativeLinkScope
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

/**
 * Contract used to authenticate a user given a Link auth URL.
 */
internal object WebLinkAuthActivityContract : ActivityResultContract<String, WebLinkAuthResult>() {

    override fun createIntent(context: Context, input: String): Intent {
        return LinkForegroundActivity.createIntent(context, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): WebLinkAuthResult {
        return when (resultCode) {
            LinkForegroundActivity.RESULT_FAILURE -> {
                val exception = intent?.extras?.let {
                    BundleCompat.getSerializable(
                        it,
                        LinkForegroundActivity.EXTRA_FAILURE,
                        Exception::class.java
                    )
                }
                if (exception != null) {
                    WebLinkAuthResult.Failure(exception)
                } else {
                    WebLinkAuthResult.Canceled
                }
            }

            LinkForegroundActivity.RESULT_COMPLETE -> {
                handleCompleteResult(intent)
            }

            Activity.RESULT_CANCELED -> {
                WebLinkAuthResult.Canceled
            }
            else -> {
                WebLinkAuthResult.Canceled
            }
        }
    }

    private fun handleCompleteResult(intent: Intent?): WebLinkAuthResult {
        val redirectUri = intent?.data ?: return WebLinkAuthResult.Canceled
        return when (redirectUri.getQueryParameter("link_status")) {
            "complete" -> {
                WebLinkAuthResult.Completed
            }

            else -> {
                WebLinkAuthResult.Canceled
            }
        }
    }
}

internal sealed interface WebLinkAuthResult {
    data object Completed : WebLinkAuthResult
    data object Canceled : WebLinkAuthResult
    data class Failure(val error: Throwable) : WebLinkAuthResult
}

// Used to communicate between the ViewModel and the Activity.
@NativeLinkScope
internal class WebLinkAuthChannel @Inject constructor() {
    val requests: MutableSharedFlow<String> = MutableSharedFlow(extraBufferCapacity = 1)
    val results: MutableSharedFlow<WebLinkAuthResult> = MutableSharedFlow(extraBufferCapacity = 1)
}
