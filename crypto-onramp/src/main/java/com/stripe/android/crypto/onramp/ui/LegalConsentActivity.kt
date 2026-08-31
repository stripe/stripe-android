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
import com.stripe.android.link.onramp.ui.LegalConsentScreen
import com.stripe.android.uicore.utils.fadeOut
import kotlinx.parcelize.Parcelize

internal class LegalConsentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = intent.extras?.let {
            BundleCompat.getParcelable(it, EXTRA_ARGS, LegalConsentArgs::class.java)
        } ?: error("Missing LegalConsentArgs")

        enableEdgeToEdge()

        setContent {
            LegalConsentScreen(
                consentText = args.consentText,
                title = stringResource(args.titleResId),
                acceptButtonText = stringResource(args.acceptButtonResId),
                cancelButtonText = stringResource(args.cancelButtonResId),
                appearance = args.appearance,
                onClose = {
                    setResult(RESULT_CANCELED, createResultIntent(LegalConsentActivityResult.Cancelled))
                    finish()
                },
                onAccept = {
                    setResult(
                        RESULT_OK,
                        createResultIntent(LegalConsentActivityResult.Accepted(args.version))
                    )
                    finish()
                }
            )
        }
    }

    override fun finish() {
        super.finish()
        fadeOut()
    }

    private fun createResultIntent(result: LegalConsentActivityResult): Intent {
        return Intent().apply { putExtra(RESULT_ARG, result) }
    }

    companion object {
        private const val EXTRA_ARGS = "legal_consent_args"
        internal const val RESULT_ARG = "result"

        internal fun createIntent(
            context: Context,
            args: LegalConsentArgs,
        ): Intent {
            return Intent(context, LegalConsentActivity::class.java)
                .putExtra(EXTRA_ARGS, args)
        }
    }
}

internal data class LegalConsentActivityArgs(
    val consentText: String,
    val version: String,
    val linkAppearance: LinkAppearance?,
    val titleResId: Int,
    val acceptButtonResId: Int,
    val cancelButtonResId: Int,
)

internal sealed interface LegalConsentActivityResult : Parcelable {
    @Parcelize
    data object Cancelled : LegalConsentActivityResult

    @Parcelize
    data class Accepted(val version: String) : LegalConsentActivityResult
}

internal class LegalConsentActivityContract : ActivityResultContract<
    LegalConsentActivityArgs,
    LegalConsentActivityResult
    >() {
    override fun createIntent(context: Context, input: LegalConsentActivityArgs): Intent {
        return LegalConsentActivity.createIntent(
            context = context,
            args = LegalConsentArgs(
                consentText = input.consentText,
                version = input.version,
                appearance = input.linkAppearance?.build(),
                titleResId = input.titleResId,
                acceptButtonResId = input.acceptButtonResId,
                cancelButtonResId = input.cancelButtonResId,
            )
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): LegalConsentActivityResult {
        return intent?.extras?.let {
            BundleCompat.getParcelable(
                it,
                LegalConsentActivity.RESULT_ARG,
                LegalConsentActivityResult::class.java
            )
        } ?: LegalConsentActivityResult.Cancelled
    }
}

@Parcelize
internal data class LegalConsentArgs(
    val consentText: String,
    val version: String,
    val appearance: LinkAppearance.State?,
    val titleResId: Int,
    val acceptButtonResId: Int,
    val cancelButtonResId: Int,
) : Parcelable
