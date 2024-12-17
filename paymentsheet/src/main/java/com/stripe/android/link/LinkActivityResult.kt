package com.stripe.android.link

import android.app.Activity
import android.content.Intent
import android.os.Parcelable
import android.util.Base64
import androidx.annotation.RestrictTo
import androidx.core.os.BundleCompat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class LinkActivityResult : Parcelable {
    /**
     * Indicates that the flow was completed successfully.
     */
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Completed(
        val paymentMethod: PaymentMethod
    ) : LinkActivityResult()

    /**
     * The user cancelled the Link flow without completing it.
     */
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Canceled(
        val reason: Reason = Reason.BackPressed,
    ) : LinkActivityResult() {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Failed(
        val error: Throwable
    ) : LinkActivityResult()
}

internal fun createLinkActivityResult(resultCode: Int, intent: Intent?): LinkActivityResult {
    return when (resultCode) {
        Activity.RESULT_CANCELED -> {
            LinkActivityResult.Canceled()
        }

        LinkForegroundActivity.RESULT_FAILURE -> {
            val exception = intent?.extras?.let {
                BundleCompat.getSerializable(
                    it,
                    LinkForegroundActivity.EXTRA_FAILURE,
                    Exception::class.java
                )
            }
            if (exception != null) {
                LinkActivityResult.Failed(exception)
            } else {
                LinkActivityResult.Canceled()
            }
        }

        LinkForegroundActivity.RESULT_COMPLETE -> {
            val redirectUri = intent?.data ?: return LinkActivityResult.Canceled()
            when (redirectUri.getQueryParameter("link_status")) {
                "complete" -> {
                    val paymentMethod = redirectUri.getQueryParameter("pm")
                        ?.parsePaymentMethod()
                    if (paymentMethod == null) {
                        LinkActivityResult.Canceled()
                    } else {
                        LinkActivityResult.Completed(paymentMethod)
                    }
                }

                "logout" -> {
                    LinkActivityResult.Canceled(LinkActivityResult.Canceled.Reason.LoggedOut)
                }

                else -> {
                    LinkActivityResult.Canceled()
                }
            }
        }

        else -> {
            LinkActivityResult.Canceled()
        }
    }
}

private fun String.parsePaymentMethod(): PaymentMethod? = try {
    val decodedPaymentMethod = String(Base64.decode(this, 0), Charsets.UTF_8)
    val paymentMethod = PaymentMethodJsonParser()
        .parse(JSONObject(decodedPaymentMethod))
    paymentMethod
} catch (e: Exception) {
    null
}
