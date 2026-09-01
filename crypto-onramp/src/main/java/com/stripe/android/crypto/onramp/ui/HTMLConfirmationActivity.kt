package com.stripe.android.crypto.onramp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.ui.res.stringResource
import androidx.core.os.BundleCompat
import com.stripe.android.link.LinkAppearance
import com.stripe.android.link.onramp.ui.HTMLConfirmationScreen
import com.stripe.android.uicore.utils.fadeOut
import kotlinx.parcelize.Parcelize

internal class HTMLConfirmationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = intent.extras?.let {
            BundleCompat.getParcelable(it, EXTRA_ARGS, HTMLConfirmationArgs::class.java)
        } ?: error("Missing HTMLConfirmationArgs")

        enableEdgeToEdge()

        setContent {
            HTMLConfirmationScreen(
                html = args.html,
                heading = stringResource(args.headingResId),
                confirmationButtonText = stringResource(args.confirmationButtonResId),
                cancelButtonText = stringResource(args.cancelButtonResId),
                appearance = args.appearance,
                onClose = { finishWithResult(HTMLConfirmationResult.Cancelled, args.declarationId) },
                onConfirm = { finishWithResult(HTMLConfirmationResult.Confirmed, args.declarationId) },
            )
        }
    }

    override fun finish() {
        super.finish()
        fadeOut()
    }

    private fun finishWithResult(
        result: HTMLConfirmationResult,
        declarationId: String?,
    ) {
        setResult(
            if (result == HTMLConfirmationResult.Confirmed) RESULT_OK else RESULT_CANCELED,
            createResultIntent(
                HTMLConfirmationActivityResult(
                    result = result,
                    declarationId = declarationId,
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
    val declarationId: String?,
    val linkAppearance: LinkAppearance?,
    val headingResId: Int,
    val confirmationButtonResId: Int,
    val cancelButtonResId: Int,
)

internal sealed interface HTMLConfirmationResult : Parcelable {
    @Parcelize
    data object Cancelled : HTMLConfirmationResult

    @Parcelize
    data object Confirmed : HTMLConfirmationResult
}

@Parcelize
internal data class HTMLConfirmationActivityResult(
    val result: HTMLConfirmationResult,
    val declarationId: String?,
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
                declarationId = input.declarationId,
                appearance = input.linkAppearance?.build(),
                headingResId = input.headingResId,
                confirmationButtonResId = input.confirmationButtonResId,
                cancelButtonResId = input.cancelButtonResId,
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
            declarationId = null,
        )
    }
}

@Parcelize
internal data class HTMLConfirmationArgs(
    val html: String,
    val declarationId: String?,
    val appearance: LinkAppearance.State?,
    val headingResId: Int,
    val confirmationButtonResId: Int,
    val cancelButtonResId: Int,
) : Parcelable
