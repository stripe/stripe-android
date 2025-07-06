package com.stripe.android.paymentsheet.addresselement

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.bundleOf
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal object AutocompleteContract :
    ActivityResultContract<AutocompleteContract.Args, AutocompleteContract.Result>() {
    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(context, AutocompleteActivity::class.java).putExtra(EXTRA_ARGS, input)
    }

    @Suppress("DEPRECATION")
    override fun parseResult(resultCode: Int, intent: Intent?): Result =
        intent?.getParcelableExtra(EXTRA_RESULT) ?: throw IllegalStateException(
            "Unknown MPE address autocomplete result!"
        )

    @Parcelize
    data class Args internal constructor(
        internal val id: String,
        internal val country: String,
        internal val googlePlacesApiKey: String,
        internal val appearanceContext: AutocompleteAppearanceContext,
    ) : ActivityStarter.Args {
        internal companion object {
            internal fun fromIntent(intent: Intent): Args? {
                @Suppress("DEPRECATION")
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }

    sealed class Result : ActivityStarter.Result {
        abstract val id: String
        abstract val addressDetails: AddressDetails?

        @Parcelize
        data class EnterManually(
            override val id: String,
            override val addressDetails: AddressDetails?,
        ) : Result()

        @Parcelize
        data class Address(
            override val id: String,
            override val addressDetails: AddressDetails?,
        ) : Result()

        override fun toBundle(): Bundle {
            return bundleOf(EXTRA_RESULT to this)
        }
    }

    const val EXTRA_ARGS =
        "com.stripe.android.paymentsheet.addresselement.AutocompleteContract.extra_args"
    const val EXTRA_RESULT =
        "com.stripe.android.paymentsheet.addresselement.AutocompleteContract.extra_result"
}
