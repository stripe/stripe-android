package com.stripe.android.link

import android.app.Activity
import android.content.Intent
import android.os.Parcelable
import android.util.Base64
import androidx.core.os.BundleCompat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

internal sealed class LinkActivityResult : Parcelable {
    /**
     * Indicates that the flow was completed successfully.
     */
    @Parcelize
    data object Completed : LinkActivityResult()

    /**
     * Indicates that the user selected a payment method. This payment method has not yet been confirmed.
     */
    @Parcelize
    data class PaymentMethodObtained(
        val paymentMethod: PaymentMethod
    ) : LinkActivityResult()

    /**
     * The user cancelled the Link flow without completing it.
     */
    @Parcelize
    data class Canceled(
        val reason: Reason = Reason.BackPressed,
    ) : LinkActivityResult() {
        enum class Reason {
            BackPressed,
            LoggedOut,
            PayAnotherWay
        }
    }

    /**
     * Something went wrong. See [error] for more information.
     */
    @Parcelize
    data class Failed(
        val error: Throwable
    ) : LinkActivityResult()
}
