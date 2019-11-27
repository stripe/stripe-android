package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import com.stripe.android.ObjectBuilder
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentMethod
import com.stripe.android.view.PaymentMethodsActivityStarter.Args
import com.stripe.android.view.PaymentMethodsActivityStarter.Companion.REQUEST_CODE
import com.stripe.android.view.PaymentMethodsActivityStarter.Result
import com.stripe.android.view.PaymentMethodsActivityStarter.Result.Companion.fromIntent
import kotlinx.android.parcel.Parcelize

/**
 * A class to start [PaymentMethodsActivity]. Arguments for the activity can be specified
 * with [Args] and constructed with [Args.Builder].
 *
 * The result data is a [Result] instance, obtained using [Result.fromIntent]}.
 * The result will be returned with request code [REQUEST_CODE].
 */
class PaymentMethodsActivityStarter : ActivityStarter<PaymentMethodsActivity, Args> {

    constructor(activity: Activity) : super(
        activity,
        PaymentMethodsActivity::class.java,
        Args.DEFAULT,
        REQUEST_CODE
    )

    constructor(fragment: Fragment) : super(
        fragment,
        PaymentMethodsActivity::class.java,
        Args.DEFAULT,
        REQUEST_CODE
    )

    @Parcelize
    data class Args internal constructor(
        internal val initialPaymentMethodId: String?,
        val shouldRequirePostalCode: Boolean,
        @LayoutRes val addPaymentMethodFooterLayoutId: Int,
        internal val isPaymentSessionActive: Boolean,
        internal val paymentMethodTypes: List<PaymentMethod.Type>,
        internal val paymentConfiguration: PaymentConfiguration?
    ) : ActivityStarter.Args {
        class Builder : ObjectBuilder<Args> {
            private var initialPaymentMethodId: String? = null
            private var shouldRequirePostalCode = false
            private var isPaymentSessionActive = false
            private var paymentMethodTypes: List<PaymentMethod.Type>? = null
            private var paymentConfiguration: PaymentConfiguration? = null
            @LayoutRes
            private var addPaymentMethodFooterLayoutId: Int = 0

            fun setInitialPaymentMethodId(initialPaymentMethodId: String?): Builder {
                this.initialPaymentMethodId = initialPaymentMethodId
                return this
            }

            fun setShouldRequirePostalCode(shouldRequirePostalCode: Boolean): Builder {
                this.shouldRequirePostalCode = shouldRequirePostalCode
                return this
            }

            fun setIsPaymentSessionActive(isPaymentSessionActive: Boolean): Builder {
                this.isPaymentSessionActive = isPaymentSessionActive
                return this
            }

            fun setPaymentConfiguration(
                paymentConfiguration: PaymentConfiguration?
            ): Builder {
                this.paymentConfiguration = paymentConfiguration
                return this
            }

            /**
             * @param paymentMethodTypes a list of [PaymentMethod.Type] that indicates the types of
             * Payment Methods that the customer can select or add via Stripe UI components.
             *
             * The order of the [PaymentMethod.Type] values in the list will be used to
             * arrange the add buttons in the Stripe UI components. They will be arranged vertically
             * from first to last.
             *
             * Currently only [PaymentMethod.Type.Card] and [PaymentMethod.Type.Fpx] are supported.
             * If not specified or empty, [PaymentMethod.Type.Card] will be used.
             */
            fun setPaymentMethodTypes(
                paymentMethodTypes: List<PaymentMethod.Type>
            ): Builder {
                this.paymentMethodTypes = paymentMethodTypes
                return this
            }

            /**
             * @param addPaymentMethodFooterLayoutId optional layout id that will be inflated and
             * displayed beneath the payment details collection form on [AddPaymentMethodActivity]
             */
            fun setAddPaymentMethodFooter(@LayoutRes addPaymentMethodFooterLayoutId: Int): Builder {
                this.addPaymentMethodFooterLayoutId = addPaymentMethodFooterLayoutId
                return this
            }

            override fun build(): Args {
                return Args(
                    initialPaymentMethodId = initialPaymentMethodId,
                    shouldRequirePostalCode = shouldRequirePostalCode,
                    isPaymentSessionActive = isPaymentSessionActive,
                    paymentMethodTypes = paymentMethodTypes ?: listOf(PaymentMethod.Type.Card),
                    paymentConfiguration = paymentConfiguration,
                    addPaymentMethodFooterLayoutId = addPaymentMethodFooterLayoutId
                )
            }
        }

        internal companion object {
            internal val DEFAULT = Builder().build()

            @JvmSynthetic
            internal fun create(intent: Intent): Args {
                return requireNotNull(intent.getParcelableExtra(ActivityStarter.Args.EXTRA))
            }
        }
    }

    /**
     * The result of a [PaymentMethodsActivity].
     *
     * Retrieve in `#onActivityResult()` using [fromIntent].
     */
    @Parcelize
    data class Result internal constructor(
        @JvmField val paymentMethod: PaymentMethod,
        private val useGooglePay: Boolean = false
    ) : ActivityStarter.Result {
        override fun toBundle(): Bundle {
            val bundle = Bundle()
            bundle.putParcelable(ActivityStarter.Result.EXTRA, this)
            return bundle
        }

        companion object {
            /**
             * @return the [Result] object from the given `Intent`
             */
            @JvmStatic
            fun fromIntent(intent: Intent?): Result? {
                return intent?.getParcelableExtra(ActivityStarter.Result.EXTRA)
            }
        }
    }

    companion object {
        const val REQUEST_CODE: Int = 6000
    }
}
