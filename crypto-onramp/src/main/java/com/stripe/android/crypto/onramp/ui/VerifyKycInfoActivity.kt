package com.stripe.android.crypto.onramp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.crypto.onramp.model.KycRetrieveResponse
import com.stripe.android.crypto.onramp.model.RefreshKycInfo
import com.stripe.android.link.LinkAppearance
import com.stripe.android.paymentsheet.ui.KYCRefreshScreen
import kotlinx.parcelize.Parcelize

class VerifyKycInfoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val kycInfo = intent.getParcelableExtra<KycRetrieveResponse>("kycInfo")!!
        val linkAppearance = intent.getParcelableExtra<LinkAppearance>("linkAppearance")

        setContent {
            KYCRefreshScreen(
                linkAppearance,
                kycInfo,
                onClose = {
                    setResult(RESULT_CANCELED, createIntent(KycRefreshScreenAction.Cancelled))
                    finish()
                },
                onEdit = {
                    setResult(RESULT_OK, createIntent(KycRefreshScreenAction.Edit))
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

                    setResult(RESULT_OK, createIntent(KycRefreshScreenAction.Confirm(refreshInfo)))
                    finish()
                }
            )
        }
    }

    private fun createIntent(action: KycRefreshScreenAction): Intent {
        return Intent().apply { putExtra("action", action) }
    }
}
data class VerifyKycActivityContractArgs(
    val kycRetrieveResponse: KycRetrieveResponse,
    val linkAppearance: LinkAppearance?
)

@Parcelize
sealed class KycRefreshScreenAction : Parcelable {
    data object Cancelled : KycRefreshScreenAction()
    data object Edit : KycRefreshScreenAction()
    data class Confirm(val info: RefreshKycInfo) : KycRefreshScreenAction()
}

data class VerifyKycActivityContractResult(
    val action: KycRefreshScreenAction
)

class VerifyKycInfoActivityContract : ActivityResultContract<
    VerifyKycActivityContractArgs,
    VerifyKycActivityContractResult
    >() {
    override fun createIntent(context: Context, input: VerifyKycActivityContractArgs): Intent {
        return Intent(context, VerifyKycInfoActivity::class.java).apply {
            putExtra("linkAppearance", input.linkAppearance)
            putExtra("kycInfo", input.kycRetrieveResponse)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): VerifyKycActivityContractResult {
        val action = intent?.getParcelableExtra<KycRefreshScreenAction>("action") ?: KycRefreshScreenAction.Cancelled

        return VerifyKycActivityContractResult(action)
    }
}
