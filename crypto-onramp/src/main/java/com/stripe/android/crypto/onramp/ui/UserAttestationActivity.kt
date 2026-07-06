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
import com.stripe.android.crypto.onramp.model.UserAttestation
import com.stripe.android.link.LinkAppearance
import com.stripe.android.link.onramp.ui.UserAttestationScreen
import com.stripe.android.uicore.utils.fadeOut
import kotlinx.parcelize.Parcelize

internal class UserAttestationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = intent.extras?.let {
            BundleCompat.getParcelable(it, EXTRA_ARGS, UserAttestationArgs::class.java)
        } ?: error("Missing UserAttestationArgs")

        enableEdgeToEdge()

        setContent {
            UserAttestationScreen(
                attestationText = args.attestationText,
                appearance = args.appearance,
                onClose = {
                    setResult(RESULT_CANCELED, createResultIntent(UserAttestationScreenAction.Cancelled))
                    finish()
                },
                onConfirm = {
                    setResult(RESULT_OK, createResultIntent(UserAttestationScreenAction.Confirm))
                    finish()
                }
            )
        }
    }

    override fun finish() {
        super.finish()
        fadeOut()
    }

    private fun createResultIntent(action: UserAttestationScreenAction): Intent {
        return Intent().apply { putExtra(ACTION_ARG, action) }
    }

    companion object {
        private const val EXTRA_ARGS = "user_attestation_args"
        internal const val ACTION_ARG = "action"

        internal fun createIntent(
            context: Context,
            args: UserAttestationArgs
        ): Intent {
            return Intent(context, UserAttestationActivity::class.java)
                .putExtra(EXTRA_ARGS, args)
        }
    }
}

internal data class UserAttestationActivityArgs(
    val attestation: UserAttestation,
    val linkAppearance: LinkAppearance?
)

internal sealed interface UserAttestationScreenAction : Parcelable {
    @Parcelize
    data object Cancelled : UserAttestationScreenAction

    @Parcelize
    data object Confirm : UserAttestationScreenAction
}

internal data class UserAttestationActivityResult(
    val action: UserAttestationScreenAction
)

internal class UserAttestationActivityContract : ActivityResultContract<
    UserAttestationActivityArgs,
    UserAttestationActivityResult
    >() {
    override fun createIntent(context: Context, input: UserAttestationActivityArgs): Intent {
        return UserAttestationActivity.createIntent(
            context = context,
            args = UserAttestationArgs(
                attestationText = input.attestation.text,
                appearance = input.linkAppearance?.build()
            )
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): UserAttestationActivityResult {
        val action = intent?.extras?.let {
            BundleCompat.getParcelable(
                it,
                UserAttestationActivity.ACTION_ARG,
                UserAttestationScreenAction::class.java
            )
        } ?: UserAttestationScreenAction.Cancelled

        return UserAttestationActivityResult(action)
    }
}

@Parcelize
internal data class UserAttestationArgs(
    val attestationText: String,
    val appearance: LinkAppearance.State?,
) : Parcelable
