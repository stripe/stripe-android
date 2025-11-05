package com.stripe.android.crypto.onramp.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.BundleCompat
import com.stripe.android.crypto.onramp.R
import com.stripe.android.crypto.onramp.model.KycRetrieveResponse
import com.stripe.android.crypto.onramp.model.RefreshKycInfo
import com.stripe.android.link.LinkAppearance
import com.stripe.android.link.onramp.ui.OnrampKYCRefreshScreen
import com.stripe.android.link.onramp.ui.VerifyKYCInfo
import com.stripe.android.uicore.utils.fadeOut
import kotlinx.parcelize.Parcelize

internal class VerifyKycInfoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = intent.getParcelableExtra(VerifyKycInfoActivity.EXTRA_ARGS, VerifyKycArgs::class.java)
            ?: error("Missing VerifyKycArgs")
        val kycInfo = args.kycRetrieveResponse
        val linkAppearance = args.appearance

        enableEdgeToEdge()

        setContent {
            OnrampKYCRefreshScreen(
                appearance = linkAppearance,
                kycInfo = kycInfo.toVerifyKYCInfo(),
                onClose = {
                    setResult(RESULT_CANCELED, createResultIntent(KycRefreshScreenAction.Cancelled))
                    finish()
                },
                onEdit = {
                    setResult(RESULT_OK, createResultIntent(KycRefreshScreenAction.Edit))
                    finish()
                },
                onConfirm = {
                    val refreshInfo = RefreshKycInfo(
                        firstName = kycInfo.firstName,
                        lastName = kycInfo.lastName,
                        idNumberLastFour = kycInfo.idNumberLastFour,
                        idType = kycInfo.idType,
                        dateOfBirth = kycInfo.dateOfBirth,
                        address = kycInfo.address
                    )

                    setResult(RESULT_OK, createResultIntent(KycRefreshScreenAction.Confirm(refreshInfo)))
                    finish()
                }
            )
        }
    }

    override fun finish() {
        super.finish()
        fadeOut()
    }

    private fun createResultIntent(action: KycRefreshScreenAction): Intent {
        return Intent().apply { putExtra(ACTION_ARG, action) }
    }

    companion object {
        private const val EXTRA_ARGS = "verify_kyc_args"
        internal const val ACTION_ARG = "action"

        internal fun createIntent(
            context: Context,
            args: VerifyKycArgs
        ): Intent {
            return Intent(context, VerifyKycInfoActivity::class.java)
                .putExtra(EXTRA_ARGS, args)
        }
    }
}

internal data class VerifyKycActivityArgs(
    val kycRetrieveResponse: KycRetrieveResponse,
    val linkAppearance: LinkAppearance?
)

@Parcelize
internal sealed interface KycRefreshScreenAction : Parcelable {
    data object Cancelled : KycRefreshScreenAction
    data object Edit : KycRefreshScreenAction
    data class Confirm(val info: RefreshKycInfo) : KycRefreshScreenAction
}

internal data class VerifyKycActivityResult(
    val action: KycRefreshScreenAction
)

internal class VerifyKycInfoActivityContract : ActivityResultContract<
    VerifyKycActivityArgs,
    VerifyKycActivityResult
    >() {
    override fun createIntent(context: Context, input: VerifyKycActivityArgs): Intent {
        return VerifyKycInfoActivity.createIntent(
            context = context,
            args = VerifyKycArgs(
                kycRetrieveResponse = input.kycRetrieveResponse,
                appearance = input.linkAppearance
            )
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): VerifyKycActivityResult {
        val action = intent?.extras?.let {
            BundleCompat.getParcelable(it, VerifyKycInfoActivity.ACTION_ARG, KycRefreshScreenAction::class.java)
        } ?: KycRefreshScreenAction.Cancelled

        return VerifyKycActivityResult(action)
    }
}

@Parcelize
internal data class VerifyKycArgs(
    val kycRetrieveResponse: KycRetrieveResponse,
    val appearance: LinkAppearance?,
) : Parcelable

private fun KycRetrieveResponse.toVerifyKYCInfo(): VerifyKYCInfo {
    return VerifyKYCInfo(
        firstName = firstName,
        lastName = lastName,
        idNumberLastFour = idNumberLastFour,
        dateOfBirth = dateOfBirth,
        address = address
    )
}
