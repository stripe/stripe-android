package com.stripe.android.ui.core.address.autocomplete

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

class AddressAutocompleteContract : ActivityResultContract<AddressAutocompleteContract.Args, AddressAutocompleteResult?>() {
    override fun createIntent(
        context: Context,
        input: Args
    ): Intent {
        return Intent(context, AddressAutocompleteActivity::class.java)
            .putExtra(EXTRA_ARGS, input)
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): AddressAutocompleteResult? {
        return AddressAutocompleteResult.fromIntent(intent)
    }

    /**
     * Arguments for launching [AddressAutocompleteActivity]
     *
     * @param country The country to constrain the autocomplete suggestions, must be passed as a
     *                two-character, ISO 3166-1 Alpha-2 compatible country code.
     *                https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2
     * @param googlePlacesApiKey The merchants Google Places API key
     */
    @Parcelize
    data class Args(
        val country: String,
        val googlePlacesApiKey: String,
        @InjectorKey val injectorKey: String = DUMMY_INJECTOR_KEY
    ) : ActivityStarter.Args {
        internal companion object {
            internal fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }

    internal companion object {
        internal const val EXTRA_ARGS = "extra_activity_args"
    }
}