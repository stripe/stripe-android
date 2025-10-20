package com.stripe.android.payments.paymentlauncher

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import android.util.Base64
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import com.stripe.android.core.exception.GenericStripeException
import com.stripe.android.model.ConfirmStripeIntentParams
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * [ActivityResultContract] to start [PaymentLauncherConfirmationActivity] and return a [PaymentResult].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PaymentLauncherContract :
    ActivityResultContract<PaymentLauncherContract.Args, InternalPaymentResult>() {
    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(
            context,
            PaymentLauncherConfirmationActivity::class.java
        ).putExtras(input.toBundle())
    }

    override fun parseResult(resultCode: Int, intent: Intent?): InternalPaymentResult {
        return InternalPaymentResult.fromIntent(intent)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed class Args(
        open val stripeAccountId: String?,
        open val enableLogging: Boolean,
        open val productUsage: Set<String>,
        open val includePaymentSheetNextHandlers: Boolean,
        @ColorInt open var statusBarColor: Int? = null,
    ) : Parcelable {
        abstract val publishableKey: String

        abstract fun validate(): Result<Unit>

        fun toBundle() = bundleOf(EXTRA_ARGS to this)

        @Parcelize
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class IntentConfirmationArgs internal constructor(
            override val publishableKey: String,
            override val stripeAccountId: String?,
            override val enableLogging: Boolean,
            override val productUsage: Set<String>,
            override val includePaymentSheetNextHandlers: Boolean,
            val confirmStripeIntentParams: ConfirmStripeIntentParams,
            @ColorInt override var statusBarColor: Int?,
        ) : Args(
            stripeAccountId = stripeAccountId,
            enableLogging = enableLogging,
            productUsage = productUsage,
            includePaymentSheetNextHandlers = includePaymentSheetNextHandlers,
            statusBarColor = statusBarColor,
        ) {
            override fun validate() = Result.success(Unit)
        }

        @Parcelize
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class PaymentIntentNextActionArgs internal constructor(
            override val publishableKey: String,
            override val stripeAccountId: String?,
            override val enableLogging: Boolean,
            override val productUsage: Set<String>,
            override val includePaymentSheetNextHandlers: Boolean,
            val paymentIntentClientSecret: String,
            @ColorInt override var statusBarColor: Int?,
        ) : Args(
            stripeAccountId = stripeAccountId,
            enableLogging = enableLogging,
            productUsage = productUsage,
            includePaymentSheetNextHandlers = includePaymentSheetNextHandlers,
            statusBarColor = statusBarColor,
        ) {
            override fun validate() = Result.success(Unit)
        }

        @Parcelize
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class SetupIntentNextActionArgs internal constructor(
            override val publishableKey: String,
            override val stripeAccountId: String?,
            override val enableLogging: Boolean,
            override val productUsage: Set<String>,
            override val includePaymentSheetNextHandlers: Boolean,
            val setupIntentClientSecret: String,
            @ColorInt override var statusBarColor: Int?,
        ) : Args(
            stripeAccountId = stripeAccountId,
            enableLogging = enableLogging,
            productUsage = productUsage,
            includePaymentSheetNextHandlers = includePaymentSheetNextHandlers,
            statusBarColor = statusBarColor,
        ) {
            override fun validate() = Result.success(Unit)
        }

        @Parcelize
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class HashedPaymentIntentNextActionArgs internal constructor(
            override val stripeAccountId: String?,
            override val enableLogging: Boolean,
            override val productUsage: Set<String>,
            override val includePaymentSheetNextHandlers: Boolean,
            val hashedValue: String,
            @ColorInt override var statusBarColor: Int?,
        ) : Args(
            stripeAccountId = stripeAccountId,
            enableLogging = enableLogging,
            productUsage = productUsage,
            includePaymentSheetNextHandlers = includePaymentSheetNextHandlers,
            statusBarColor = statusBarColor,
        ) {
            @IgnoredOnParcel
            private val decodedValue: Result<Pair<String, String>> by lazy {
                val decodedBase64 = try {
                    Base64.decode(hashedValue, 0)
                } catch (exception: IllegalArgumentException) {
                    return@lazy Result.failure(
                        GenericStripeException(
                            cause = IllegalArgumentException(
                                INVALID_HASHED_VALUE_MESSAGE,
                                exception,
                            ),
                            analyticsValue = HASHED_VALUE_NOT_BASE_64_ANALYTICS_VALUE,
                        )
                    )
                }

                val decodedString = String(decodedBase64, Charsets.UTF_8)
                val splitValue = decodedString.split(":")

                if (splitValue.size != 2) {
                    return@lazy Result.failure(
                        GenericStripeException(
                            cause = IllegalArgumentException(INVALID_HASHED_VALUE_MESSAGE),
                            analyticsValue = HASHED_VALUE_INVALID_FORMAT_ANALYTICS_VALUE,
                        )
                    )
                }

                Result.success(splitValue[0] to splitValue[1])
            }

            override val publishableKey: String
                get() = decodedValue.getOrNull()?.first ?: UNKNOWN_KEY

            val paymentIntentClientSecret: String
                get() = decodedValue.getOrNull()?.second ?: UNKNOWN_KEY

            override fun validate() = decodedValue.map {}
        }

        internal companion object {
            private const val EXTRA_ARGS = "extra_args"
            private const val INVALID_HASHED_VALUE_MESSAGE =
                "Invalid hashed value! Please provided a hashed payment intent in the correct format!"

            private const val HASHED_VALUE_NOT_BASE_64_ANALYTICS_VALUE = "invalidHashedValueNotBase64"
            private const val HASHED_VALUE_INVALID_FORMAT_ANALYTICS_VALUE = "invalidHashedValueIncorrectFormat"

            private const val UNKNOWN_KEY = "UNKNOWN"

            fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }
}
