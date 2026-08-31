package com.stripe.android.crypto.onramp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.os.BundleCompat
import androidx.lifecycle.lifecycleScope
import com.stripe.android.link.LinkAppearance
import com.stripe.android.link.onramp.ui.HTMLConfirmationScreen
import com.stripe.android.uicore.utils.fadeOut
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.util.concurrent.ConcurrentHashMap

internal class HTMLConfirmationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = intent.extras?.let {
            BundleCompat.getParcelable(it, EXTRA_ARGS, HTMLConfirmationArgs::class.java)
        } ?: error("Missing HTMLConfirmationArgs")

        enableEdgeToEdge()

        setContent {
            var isProcessing by remember { mutableStateOf(false) }

            HTMLConfirmationScreen(
                html = args.html,
                heading = stringResource(args.headingResId),
                confirmationButtonText = stringResource(args.confirmationButtonResId),
                cancelButtonText = stringResource(args.cancelButtonResId),
                appearance = args.appearance,
                isProcessing = isProcessing,
                onClose = { cancel(args.confirmationId) },
                onConfirm = {
                    if (!isProcessing) {
                        isProcessing = true
                        confirm(args)
                    }
                }
            )
        }
    }

    override fun finish() {
        super.finish()
        fadeOut()
    }

    private fun cancel(confirmationId: String) {
        setResult(
            RESULT_CANCELED,
            createResultIntent(
                HTMLConfirmationActivityResult(
                    result = HTMLConfirmationResult.Cancelled,
                    confirmationId = confirmationId,
                    confirmationHandled = false,
                )
            )
        )
        finish()
    }

    private fun confirm(args: HTMLConfirmationArgs) {
        val confirmed = HTMLConfirmationResult.Confirmed(version = args.version)
        val confirmation = HTMLConfirmationCallbackReferences[args.confirmationId]
        if (confirmation == null) {
            finishConfirmation(confirmed, args.confirmationId, confirmationHandled = false)
        } else {
            lifecycleScope.launch {
                try {
                    confirmation(confirmed)
                } finally {
                    finishConfirmation(confirmed, args.confirmationId, confirmationHandled = true)
                }
            }
        }
    }

    private fun finishConfirmation(
        result: HTMLConfirmationResult.Confirmed,
        confirmationId: String,
        confirmationHandled: Boolean,
    ) {
        setResult(
            RESULT_OK,
            createResultIntent(
                HTMLConfirmationActivityResult(
                    result = result,
                    confirmationId = confirmationId,
                    confirmationHandled = confirmationHandled,
                )
            )
        )
        finish()
    }

    private fun createResultIntent(result: HTMLConfirmationActivityResult): Intent {
        return Intent().apply { putExtra(RESULT_ARG, result) }
    }

    companion object {
        private const val EXTRA_ARGS = "html_confirmation_args"
        internal const val RESULT_ARG = "result"

        internal fun createIntent(
            context: Context,
            args: HTMLConfirmationArgs,
        ): Intent {
            return Intent(context, HTMLConfirmationActivity::class.java)
                .putExtra(EXTRA_ARGS, args)
        }
    }
}

internal data class HTMLConfirmationActivityArgs(
    val html: String,
    val version: String,
    val linkAppearance: LinkAppearance?,
    val headingResId: Int,
    val confirmationButtonResId: Int,
    val cancelButtonResId: Int,
    val confirmationId: String,
)

internal sealed interface HTMLConfirmationResult : Parcelable {
    @Parcelize
    data object Cancelled : HTMLConfirmationResult

    @Parcelize
    data class Confirmed(
        val version: String,
    ) : HTMLConfirmationResult
}

@Parcelize
internal data class HTMLConfirmationActivityResult(
    val result: HTMLConfirmationResult,
    val confirmationId: String?,
    val confirmationHandled: Boolean,
) : Parcelable

internal class HTMLConfirmationActivityContract : ActivityResultContract<
    HTMLConfirmationActivityArgs,
    HTMLConfirmationActivityResult
    >() {
    override fun createIntent(context: Context, input: HTMLConfirmationActivityArgs): Intent {
        return HTMLConfirmationActivity.createIntent(
            context = context,
            args = HTMLConfirmationArgs(
                html = input.html,
                version = input.version,
                appearance = input.linkAppearance?.build(),
                headingResId = input.headingResId,
                confirmationButtonResId = input.confirmationButtonResId,
                cancelButtonResId = input.cancelButtonResId,
                confirmationId = input.confirmationId,
            )
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): HTMLConfirmationActivityResult {
        return intent?.extras?.let {
            BundleCompat.getParcelable(
                it,
                HTMLConfirmationActivity.RESULT_ARG,
                HTMLConfirmationActivityResult::class.java
            )
        } ?: HTMLConfirmationActivityResult(
            result = HTMLConfirmationResult.Cancelled,
            confirmationId = null,
            confirmationHandled = false,
        )
    }
}

@Parcelize
internal data class HTMLConfirmationArgs(
    val html: String,
    val version: String,
    val appearance: LinkAppearance.State?,
    val headingResId: Int,
    val confirmationButtonResId: Int,
    val cancelButtonResId: Int,
    val confirmationId: String,
) : Parcelable

internal object HTMLConfirmationCallbackReferences {
    private val callbacks = ConcurrentHashMap<
        String,
        suspend (HTMLConfirmationResult.Confirmed) -> Unit
        >()

    operator fun get(confirmationId: String): (suspend (HTMLConfirmationResult.Confirmed) -> Unit)? {
        return callbacks[confirmationId]
    }

    operator fun set(
        confirmationId: String,
        callback: suspend (HTMLConfirmationResult.Confirmed) -> Unit,
    ) {
        callbacks[confirmationId] = callback
    }

    fun remove(confirmationId: String) {
        callbacks.remove(confirmationId)
    }
}
