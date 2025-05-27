package com.stripe.android.paymentsheet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkAccountUpdate.Value.UpdateReason
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal sealed class PaymentOptionResult(
    val resultCode: Int,
) : Parcelable {

    abstract val paymentMethods: List<PaymentMethod>?
    /**
     * Contains information about changes on the Link account during the payment method selection flow, regardless
     * of the payment method selection (e.g. if the user Logged in / out to Link).
     */
    abstract val linkAccountInfo: LinkAccountInfo

    @Parcelize
    data class Succeeded(
        val paymentSelection: PaymentSelection,
        override val linkAccountInfo: LinkAccountInfo,
        override val paymentMethods: List<PaymentMethod>? = null
    ) : PaymentOptionResult(Activity.RESULT_OK)

    @Parcelize
    data class Failed(
        val error: Throwable,
        override val linkAccountInfo: LinkAccountInfo,
        override val paymentMethods: List<PaymentMethod>? = null
    ) : PaymentOptionResult(Activity.RESULT_CANCELED)

    @Parcelize
    data class Canceled(
        val mostRecentError: Throwable?,
        val paymentSelection: PaymentSelection?,
        // The user could have removed a payment method and canceled the flow. We should update
        // the list of paymentMethods
        override val paymentMethods: List<PaymentMethod>? = null,
        override val linkAccountInfo: LinkAccountInfo
    ) : PaymentOptionResult(Activity.RESULT_CANCELED)

    fun toBundle(): Bundle {
        return bundleOf(EXTRA_RESULT to this)
    }

    internal companion object {
        private const val EXTRA_RESULT = ActivityStarter.Result.EXTRA

        @JvmSynthetic
        internal fun fromIntent(intent: Intent?): PaymentOptionResult? {
            @Suppress("DEPRECATION")
            return intent?.getParcelableExtra(EXTRA_RESULT)
        }
    }
}

@Parcelize
internal data class LinkAccountInfo(
    val linkAccount: LinkAccount?,
    val lastUpdateReason: UpdateReason? = null
) : Parcelable
