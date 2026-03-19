package com.stripe.android.paymentelement.embedded.content

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.BundleCompat
import com.stripe.android.paymentelement.embedded.form.FormContract
import com.stripe.android.paymentelement.embedded.form.FormResult
import com.stripe.android.paymentelement.embedded.manage.ManageContract
import com.stripe.android.paymentelement.embedded.manage.ManageResult
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal sealed interface EmbeddedSheetArgs : Parcelable {
    @Parcelize
    data class Form(val formArgs: FormContract.Args) : EmbeddedSheetArgs

    @Parcelize
    data class Manage(val manageArgs: ManageContract.Args) : EmbeddedSheetArgs
}

internal sealed interface EmbeddedSheetResult : Parcelable {
    @Parcelize
    data class Form(val formResult: FormResult) : EmbeddedSheetResult

    @Parcelize
    data class Manage(val manageResult: ManageResult) : EmbeddedSheetResult

    companion object {
        internal const val EXTRA_RESULT = ActivityStarter.Result.EXTRA

        fun toIntent(intent: Intent, result: EmbeddedSheetResult): Intent {
            return intent.putExtra(EXTRA_RESULT, result)
        }

        fun fromIntent(intent: Intent?): EmbeddedSheetResult? {
            return intent?.extras?.let { bundle ->
                BundleCompat.getParcelable(bundle, EXTRA_RESULT, EmbeddedSheetResult::class.java)
            }
        }
    }
}

internal object EmbeddedSheetContract : ActivityResultContract<EmbeddedSheetArgs, EmbeddedSheetResult?>() {
    internal const val EXTRA_ARGS: String = "extra_embedded_sheet_args"

    override fun createIntent(context: Context, input: EmbeddedSheetArgs): Intent {
        return Intent(context, EmbeddedSheetActivity::class.java)
            .putExtra(EXTRA_ARGS, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): EmbeddedSheetResult? {
        return EmbeddedSheetResult.fromIntent(intent)
    }

    fun argsFromIntent(intent: Intent): EmbeddedSheetArgs? {
        return intent.extras?.let { bundle ->
            BundleCompat.getParcelable(bundle, EXTRA_ARGS, EmbeddedSheetArgs::class.java)
        }
    }
}
