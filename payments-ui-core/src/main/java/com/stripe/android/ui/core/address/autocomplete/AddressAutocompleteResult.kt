package com.stripe.android.ui.core.address.autocomplete

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import com.stripe.android.model.Address
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

sealed class AddressAutocompleteResult(
    val resultCode: Int,
    open val address: Address? = null
) : Parcelable {
    fun toBundle(): Bundle {
        return bundleOf(EXTRA_RESULT to this)
    }

    @Parcelize
    data class Succeeded(
        override val address: Address? = null
    ) : AddressAutocompleteResult(Activity.RESULT_OK, address)

    @Parcelize
    data class Failed(
        val error: Throwable,
        override val address: Address? = null
    ) : AddressAutocompleteResult(Activity.RESULT_CANCELED, address)

    @Parcelize
    data class Canceled(
        val mostRecentError: Throwable?,
        override val address: Address? = null
    ) : AddressAutocompleteResult(Activity.RESULT_CANCELED, address)

    internal companion object {
        private const val EXTRA_RESULT = ActivityStarter.Result.EXTRA

        @JvmSynthetic
        internal fun fromIntent(intent: Intent?): AddressAutocompleteResult? {
            return intent?.getParcelableExtra(EXTRA_RESULT)
        }
    }
}
