package com.stripe.android.crypto.onramp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.BundleCompat
import com.stripe.android.crypto.onramp.model.CrsCarfDeclaration
import com.stripe.android.link.LinkAppearance
import com.stripe.android.link.onramp.ui.CrsCarfDeclarationScreen
import com.stripe.android.uicore.utils.fadeOut
import kotlinx.parcelize.Parcelize

internal class CrsCarfDeclarationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = intent.extras?.let {
            BundleCompat.getParcelable(it, EXTRA_ARGS, CrsCarfDeclarationArgs::class.java)
        } ?: error("Missing CrsCarfDeclarationArgs")

        enableEdgeToEdge()

        setContent {
            CrsCarfDeclarationScreen(
                attestationText = args.attestationText,
                appearance = args.appearance,
                onClose = {
                    setResult(RESULT_CANCELED, createResultIntent(CrsCarfDeclarationScreenAction.Cancelled))
                    finish()
                },
                onConfirm = {
                    setResult(RESULT_OK, createResultIntent(CrsCarfDeclarationScreenAction.Confirm))
                    finish()
                }
            )
        }
    }

    override fun finish() {
        super.finish()
        fadeOut()
    }

    private fun createResultIntent(action: CrsCarfDeclarationScreenAction): Intent {
        return Intent().apply { putExtra(ACTION_ARG, action) }
    }

    companion object {
        private const val EXTRA_ARGS = "crs_carf_declaration_args"
        internal const val ACTION_ARG = "action"

        internal fun createIntent(
            context: Context,
            args: CrsCarfDeclarationArgs
        ): Intent {
            return Intent(context, CrsCarfDeclarationActivity::class.java)
                .putExtra(EXTRA_ARGS, args)
        }
    }
}

internal data class CrsCarfDeclarationActivityArgs(
    val declaration: CrsCarfDeclaration,
    val linkAppearance: LinkAppearance?
)

internal sealed interface CrsCarfDeclarationScreenAction : Parcelable {
    @Parcelize
    data object Cancelled : CrsCarfDeclarationScreenAction

    @Parcelize
    data object Confirm : CrsCarfDeclarationScreenAction
}

internal data class CrsCarfDeclarationActivityResult(
    val action: CrsCarfDeclarationScreenAction
)

internal class CrsCarfDeclarationActivityContract : ActivityResultContract<
    CrsCarfDeclarationActivityArgs,
    CrsCarfDeclarationActivityResult
    >() {
    override fun createIntent(context: Context, input: CrsCarfDeclarationActivityArgs): Intent {
        return CrsCarfDeclarationActivity.createIntent(
            context = context,
            args = CrsCarfDeclarationArgs(
                attestationText = input.declaration.text,
                appearance = input.linkAppearance?.build()
            )
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): CrsCarfDeclarationActivityResult {
        val action = intent?.extras?.let {
            BundleCompat.getParcelable(
                it,
                CrsCarfDeclarationActivity.ACTION_ARG,
                CrsCarfDeclarationScreenAction::class.java
            )
        } ?: CrsCarfDeclarationScreenAction.Cancelled

        return CrsCarfDeclarationActivityResult(action)
    }
}

@Parcelize
internal data class CrsCarfDeclarationArgs(
    val attestationText: String,
    val appearance: LinkAppearance.State?,
) : Parcelable
