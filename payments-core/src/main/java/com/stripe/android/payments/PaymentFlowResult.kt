package com.stripe.android.payments

import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import com.stripe.android.PaymentController
import com.stripe.android.StripeIntentResult
import com.stripe.android.core.exception.StripeException
import com.stripe.android.model.Source
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize

/**
 * Represents the result of a [PaymentController] operation.
 *
 * This class is annotated with `@Parcelize` but uses custom parceling logic due to issues
 * with parceling an `Exception` subclass. See
 * [Parcel#writeException()](https://developer.android.com/reference/android/os/Parcel#writeException(java.lang.Exception))
 * for more details.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class PaymentFlowResult {
    @Parcelize
    data class Unvalidated constructor(
        val clientSecret: String? = null,
        @StripeIntentResult.Outcome val flowOutcome: Int = StripeIntentResult.Outcome.UNKNOWN,
        val exception: StripeException? = null,
        internal val canCancelSource: Boolean = false,
        internal val sourceId: String? = null,
        internal val source: Source? = null,
        internal val stripeAccountId: String? = null
    ) : Parcelable {
        @JvmSynthetic
        fun toBundle() = bundleOf(EXTRA to this)

        internal fun validate(): Validated {
            if (exception is Throwable) {
                throw exception
            }
            require(!clientSecret.isNullOrBlank()) {
                CLIENT_SECRET_INTENT_ERROR
            }

            return Validated(
                clientSecret = clientSecret,
                flowOutcome = flowOutcome,
                canCancelSource = canCancelSource,
                sourceId = sourceId,
                source = source,
                stripeAccountId = stripeAccountId
            )
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        companion object : Parceler<Unvalidated> {
            override fun create(parcel: Parcel): Unvalidated {
                return Unvalidated(
                    clientSecret = parcel.readString(),
                    flowOutcome = parcel.readInt(),
                    exception = parcel.readSerializable() as? StripeException?,
                    canCancelSource = parcel.readInt() == 1,
                    sourceId = parcel.readString(),
                    source = parcel.readParcelable(Source::class.java.classLoader),
                    stripeAccountId = parcel.readString()
                )
            }

            override fun Unvalidated.write(parcel: Parcel, flags: Int) {
                parcel.writeString(clientSecret)
                parcel.writeInt(flowOutcome)
                parcel.writeSerializable(exception)
                parcel.writeInt(1.takeIf { canCancelSource } ?: 0)
                parcel.writeString(sourceId)
                parcel.writeParcelable(source, flags)
                parcel.writeString(stripeAccountId)
            }

            @JvmSynthetic
            fun fromIntent(intent: Intent?): Unvalidated {
                return intent?.getParcelableExtra(EXTRA) ?: Unvalidated()
            }

            private const val EXTRA = "extra_args"
            private const val CLIENT_SECRET_INTENT_ERROR = "Invalid client_secret value in result Intent."
        }
    }

    /**
     * The representation of a valid [PaymentFlowResult]. See [Unvalidated.validate].
     */
    internal data class Validated internal constructor(
        val clientSecret: String,
        @StripeIntentResult.Outcome internal val flowOutcome: Int,

        /**
         * The Source is eligible for cancellation.
         * See [PaymentFlowResultProcessor.shouldCancelIntent] for usage.
         */
        internal val canCancelSource: Boolean = false,

        internal val sourceId: String? = null,
        internal val source: Source? = null,
        internal val stripeAccountId: String? = null
    )
}
